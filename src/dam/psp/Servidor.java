package dam.psp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Servidor {
	public static void main(String[] args) throws IOException {
		ServerSocket serverSocket = new ServerSocket(9000);
		Socket socket = serverSocket.accept();
//		socket.setSoTimeout(5000); //
		try (DataInputStream in = new DataInputStream(socket.getInputStream());
				DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
			String peticion = in.readUTF();
			switch (peticion) {
			case "hash":
				funcionHash(in, out);
				break;
			case "cert":
				funcionCert(in, out);
				break;
			case "cifrar":
				funcionCifrar(in, out);
				break;
			default:
				System.err.println("ERROR:'petición' no se reconoce como una petición válida");
				out.writeUTF("ERROR:'petición' no se reconoce como una petición válida");
			}
		}
		serverSocket.close();
	}

	public static void funcionHash(DataInputStream in, DataOutputStream out) {
		try {
			String algoritmo = in.readUTF();
			if (algoritmo.isBlank() || algoritmo == null) {
				System.err.println("ERROR:Se esperaba un algoritmo");
				out.writeUTF("ERROR:Se esperaba un algoritmo");
			}
			String mensaje = in.readUTF();
			if (mensaje.isBlank() || mensaje == null) {
				System.err.println("ERROR:Se esperaban datos");
				out.writeUTF("ERROR:Se esperaban datos");
			}

			MessageDigest md = MessageDigest.getInstance(algoritmo);
			String resultado = Base64.getEncoder().encodeToString(md.digest(mensaje.getBytes()));
			System.out.println("OK:" + resultado);
			out.writeUTF("OK:" + resultado);
			out.flush();
		} catch (NoSuchAlgorithmException e) {
			try {
				System.err.println("ERROR:Se esperaba un algoritmo");
				out.writeUTF("ERROR:Se esperaba un algoritmo");
				out.flush();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void funcionCert(DataInputStream in, DataOutputStream out) {
		try {
			KeyStore ks = KeyStore.getInstance("pkcs12");

			String aliasCert = in.readUTF();
			if (aliasCert.isBlank() || aliasCert == null) {
				System.err.println("ERROR:Se esperaba un alias");
				out.writeUTF("ERROR:Se esperaba un alias");
			}
			String codificacion = in.readUTF(); // HASH
			if (codificacion.isBlank() || codificacion == null) {
				System.err.println("ERROR:Se esperaba un certificado");
				out.writeUTF("ERROR:Se esperaba un certificado");
			}

			Certificate cert = null;
			byte[] b = Base64.getDecoder().decode(codificacion.getBytes());

			ks.setCertificateEntry(aliasCert, cert);

			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(codificacion.getBytes());
			md.digest();
			String hash = Base64.getEncoder().encodeToString(md.digest());
			out.writeUTF("OK:" + hash);
			out.flush();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	private static void funcionCifrar(DataInputStream in, DataOutputStream out) {
		try {
			String aliasCifrar = in.readUTF();
			if (aliasCifrar.isBlank() || aliasCifrar == null) {
				System.err.println("ERROR:Se esperaba un alias");
				out.writeUTF("ERROR:Se esperaba un alias");
			}
			KeyStore ks = KeyStore.getInstance("pkcs12");
			ks.load(new FileInputStream(System.getProperty("user.dir") + "\\res\\keystore.p12"),
					"practicas".toCharArray());
			PublicKey pubKey = ks.getCertificate(aliasCifrar).getPublicKey();
			if (pubKey.getAlgorithm() != "RSA") {
				System.err.println("ERROR:'alias' no contiene una clave RSA");
				out.writeUTF("ERROR:'alias' no contiene una clave RSA");
			}
			
			
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, pubKey);
			byte[] buffer = new byte[256];
			int n;
			while ((n = in.read(buffer)) != -1) {
				String s = Base64.getEncoder().encodeToString(cipher.doFinal(buffer, 0, n));
				out.writeUTF("OK:" + s);
				out.flush();
			}
			out.writeUTF("FIN:CIFRADO");
			out.flush();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			try {
				System.err.println("ERROR:'alias' no es un certificado");
				out.writeUTF("ERROR:'alias' no es un certificado");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}
	}
}
