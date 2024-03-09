package dam.psp;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Servidor extends Thread {
	private Socket socket;

	public Servidor(Socket socket) throws SocketException {
		this.socket = socket;
		socket.setSoTimeout(5000);
	}

	public static void main(String[] args) {
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(9000);

			while (true) {
				Socket socket = serverSocket.accept();
				new Servidor(socket).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try (DataInputStream in = new DataInputStream(socket.getInputStream());
				DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
			try {
				String peticion = in.readUTF();
				switch (peticion) {
				case "hash":
					funcionHash(socket, in, out);
					break;
				case "cert":
					funcionCert(socket, in, out);
					break;
				case "cifrar":
					funcionCifrar(socket, in, out);
					break;
				default:
					out.writeUTF("ERROR:'" + peticion + "' no se reconoce como una petición válida");
				}
			} catch (SocketTimeoutException e) {
				out.writeUTF("ERROR:Read timed out");
			} catch (EOFException e) {
				out.writeUTF("ERROR:Se esperaba una petición");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void funcionHash(Socket socket, DataInputStream in, DataOutputStream out) {
		String algoritmo = null;
		try {
			algoritmo = in.readUTF();
			byte[] mensaje = in.readAllBytes();

			if (mensaje.length > 0) {
				MessageDigest md = MessageDigest.getInstance(algoritmo);
				String resultado = Base64.getEncoder().encodeToString(md.digest(mensaje));
				out.writeUTF("OK:" + resultado);
				out.flush();
			} else {
				out.writeUTF("ERROR:Se esperaban datos");
				out.flush();
			}
		} catch (NoSuchAlgorithmException | EOFException e) {
			try {
				out.writeUTF("ERROR:Se esperaba un algoritmo");
				out.flush();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (SocketTimeoutException e) {
			try {
				out.writeUTF("ERROR:Read timed out");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void funcionCert(Socket socket, DataInputStream in, DataOutputStream out) {
		String alias = "";
		try {
			alias = in.readUTF();
			KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
			ks.load(null);

			String codificacion = in.readUTF();
			byte[] bytesCodif = Base64.getDecoder().decode(codificacion.getBytes());

			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			Certificate cert = cf.generateCertificate(new ByteArrayInputStream(bytesCodif));

			ks.setCertificateEntry(alias, cert);

			MessageDigest md;
			md = MessageDigest.getInstance("SHA-256");
			md.update(codificacion.getBytes());
			String b64HashB64 = Base64.getEncoder().encodeToString(md.digest());
			out.writeUTF("OK:" + b64HashB64);
		} catch (EOFException e) {
			if (alias.isBlank())
				try {
					out.writeUTF("ERROR:Se esperaba un alias");
					out.flush();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			else
				try {
					out.writeUTF("ERROR:Se esperaba un certificado");
					out.flush();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		} catch (SocketTimeoutException e) {
			try {
				out.writeUTF("ERROR:Read timed out");
				out.flush();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (IllegalArgumentException e) {
			try {
				out.writeUTF("ERROR:Se esperaba Base64");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
			e.printStackTrace();
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void funcionCifrar(Socket socket, DataInputStream in, DataOutputStream out) {
		String alias = "";
		try {
			KeyStore ks = KeyStore.getInstance("pkcs12");
			ks.load(new FileInputStream(System.getProperty("user.dir") + "/res/keystore.p12"), "practicas".toCharArray());
			
			alias = in.readUTF();
			byte[] datos = in.readAllBytes();
			if (!ks.containsAlias(alias)) {
				out.writeUTF("ERROR:'" + alias + "' no es un certificado");
				out.flush();
			} else if (ks.containsAlias(alias) && ks.isCertificateEntry(alias)) {
				
				try {
					
					Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
					cipher.init(Cipher.ENCRYPT_MODE, ks.getCertificate(alias));
					
					int n = 0; 
					byte[] buffer = new byte[256];
					boolean aux = false; 
					ByteArrayInputStream bytein = new ByteArrayInputStream(datos);
					while ((n = bytein.read(buffer)) != -1) {
						aux = true; 
						byte[] b = cipher.doFinal(buffer);
						String b64HashB64 = Base64.getEncoder().encodeToString(b);
						out.writeUTF("OK:" + b64HashB64);
						System.out.println("OK:" + b64HashB64);
					}
					if (aux = true) {
						out.writeUTF("FIN:CIFRADO");
						out.flush(); 
						System.out.println("FIN");
					} else {
						//creo que no tiene manera de llegar a esto pero por si acaso
						out.writeUTF("ERROR:Se esperaban datos");
					}
				} catch (NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
					e.printStackTrace();
				}
				
			} else if (datos.length>0) {
				out.writeUTF("ERROR:'" + alias + "' no contiene una clave RSA");
				out.flush(); 
			} else {
				out.writeUTF("ERROR:Se esperaban datos");
				out.flush(); 
			}
		} catch (SocketTimeoutException e) {
			try {
				out.writeUTF("ERROR:Read timed out");
				out.flush();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (EOFException e) {
			if (alias.isBlank())
				try {
					out.writeUTF("ERROR:Se esperaba un alias");
					out.flush();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			else
				try {
					out.writeUTF("ERROR:Se esperaban datos");
					out.flush();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		} catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException e) {
			e.printStackTrace();
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
