package dam.psp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.MethodName.class)
class ServidorUnitTest {

	static KeyStore ks;
	
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(ServidorUnitTest.class.getResourceAsStream("/keystore.p12"), "practicas".toCharArray());
	}
	
	@Test
	@DisplayName("(0,2 puntos) No se envía petición (A)")
	void test01() {
		try (Socket socket = new Socket("localhost", 9000)){
			socket.setSoTimeout(10000);
			assertEquals("ERROR:Read timed out", new DataInputStream(socket.getInputStream()).readUTF());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
	}
	
	@Test
	@DisplayName("(0,2 puntos) No se envía petición (B)")
	void test02() {
		try (Socket socket = new Socket("localhost", 9000)){
			socket.setSoTimeout(10000);
			socket.shutdownOutput();
			assertEquals("ERROR:Se esperaba una petición", new DataInputStream(socket.getInputStream()).readUTF());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
	}
	
	@Test
	@DisplayName("(0,2 puntos) Petición incorrecta (A)")
	void test03() {
		try (Socket socket = new Socket("localhost", 9000)){
			socket.setSoTimeout(10000);
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			out.writeUTF("abcd");
			assertEquals("ERROR:'abcd' no se reconoce como una petición válida", new DataInputStream(socket.getInputStream()).readUTF());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
	}
	
	@Test
	@DisplayName("(0,2 puntos) Petición incorrecta (B)")
	void test04() {
		try (Socket socket = new Socket("localhost", 9000)){
			socket.setSoTimeout(10000);
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			out.writeUTF("abcd");
			socket.shutdownOutput();
			assertEquals("ERROR:'abcd' no se reconoce como una petición válida", new DataInputStream(socket.getInputStream()).readUTF());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
	}
	
	@Test
	@DisplayName("(1,4 puntos) Petición \"hash\"")
	void test05() {
		String mensaje = "MENSAJE DE PRUEBA";
		for (String algoritmo: new String [] {"SHA-256", "MD5", "SHA3-512"}) {
			try (Socket socket = new Socket("localhost", 9000)){
				socket.setSoTimeout(1000);
				MessageDigest md;
				md = MessageDigest.getInstance(algoritmo);
				String hashB64 = Base64.getEncoder().encodeToString(md.digest(mensaje.getBytes()));
				
				DataOutputStream out = new DataOutputStream(socket.getOutputStream());
				out.writeUTF("hash");
				out.writeUTF(algoritmo);
				out.write(mensaje.getBytes());
				socket.shutdownOutput();
				assertEquals("OK:" + hashB64, new DataInputStream(socket.getInputStream()).readUTF());
			} catch (IOException | NoSuchAlgorithmException e) {
				fail(e.getLocalizedMessage());
			}
		}
	}
	
	@Test
	@DisplayName("(0,2 puntos) Petición \"hash\" sin algoritmo (A)")
	void test06() {
		try (Socket socket = new Socket("localhost", 9000)){
			socket.setSoTimeout(10000);
			
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			out.writeUTF("hash");
			
			assertEquals("ERROR:Read timed out", new DataInputStream(socket.getInputStream()).readUTF());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
	}
	
	@Test
	@DisplayName("(0,2 puntos) Petición \"hash\" sin algoritmo (B)")
	void test07() {
		try (Socket socket = new Socket("localhost", 9000)){
			socket.setSoTimeout(10000);
			
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			out.writeUTF("hash");
			socket.shutdownOutput();
			
			assertEquals("ERROR:Se esperaba un algoritmo", new DataInputStream(socket.getInputStream()).readUTF());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
	}
	
	@Test
	@DisplayName("(0,2 puntos) Petición \"hash\" sin datos (A)")
	void test08() {
		try (Socket socket = new Socket("localhost", 9000)){
			socket.setSoTimeout(10000);
			
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			out.writeUTF("hash");
			out.writeUTF("SHA-256");
			
			assertEquals("ERROR:Read timed out", new DataInputStream(socket.getInputStream()).readUTF());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
	}
	
	@Test
	@DisplayName("(0,2 puntos) Petición \"hash\" sin datos (B)")
	void test09() {
		try (Socket socket = new Socket("localhost", 9000)){
			socket.setSoTimeout(10000);
			
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			out.writeUTF("hash");
			out.writeUTF("SHA-256");
			socket.shutdownOutput();
			
			assertEquals("ERROR:Se esperaban datos", new DataInputStream(socket.getInputStream()).readUTF());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
	}
	
	@Test
	@DisplayName("(0,2 puntos) Petición \"hash\" cliente no envía EOF")
	void test10() {
		String mensaje = "MENSAJE DE PRUEBA";
		try (Socket socket = new Socket("localhost", 9000)){
			socket.setSoTimeout(10000);
			
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			out.writeUTF("hash");
			out.writeUTF("SHA-256");
			out.write(mensaje.getBytes());
			
			assertEquals("ERROR:Read timed out", new DataInputStream(socket.getInputStream()).readUTF());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
	}
	
	@Test
	@DisplayName("(1,4 puntos) Petición \"cert\"")
	void test11() {
		try (Socket socket = new Socket("localhost", 9000)){
			socket.setSoTimeout(1000);
			String b64 = Base64.getEncoder().encodeToString(ks.getCertificate("psp").getEncoded());
			MessageDigest md;
			md = MessageDigest.getInstance("SHA-256");
			md.update(b64.getBytes());
			String b64HashB64 = Base64.getEncoder().encodeToString(md.digest());
			
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			out.writeUTF("cert");
			out.writeUTF("psp");
			out.writeUTF(b64);
			socket.shutdownOutput();
			
			assertEquals("OK:" + b64HashB64, new DataInputStream(socket.getInputStream()).readUTF());
		} catch (IOException | CertificateEncodingException | NoSuchAlgorithmException | KeyStoreException e) {
			fail(e.getLocalizedMessage());
		}
	}
	
	@Test
	@DisplayName("(0,2 puntos) Petición \"cert\" sin alias (A)")
	void test12() {
		try (Socket socket = new Socket("localhost", 9000)){
			socket.setSoTimeout(10000);
			
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			out.writeUTF("cert");
			
			assertEquals("ERROR:Read timed out", new DataInputStream(socket.getInputStream()).readUTF());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
	}	
	
	@Test
	@DisplayName("(0,2 puntos) Petición \"cert\" sin alias (B)")
	void test13() {
		try (Socket socket = new Socket("localhost", 9000)){
			socket.setSoTimeout(10000);
			
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			out.writeUTF("cert");
			socket.shutdownOutput();
			
			assertEquals("ERROR:Se esperaba un alias", new DataInputStream(socket.getInputStream()).readUTF());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
	}
	
	@Test
	@DisplayName("(0,2 puntos) Petición \"cert\" sin certificado (A)")
	void test14() {
		try (Socket socket = new Socket("localhost", 9000)){
			socket.setSoTimeout(10000);
			
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			out.writeUTF("cert");
			out.writeUTF("psp");
			
			assertEquals("ERROR:Read timed out", new DataInputStream(socket.getInputStream()).readUTF());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
	}
	
	@Test
	@DisplayName("(0,2 puntos) Petición \"cert\" sin certificado (B)")
	void test15() {
		try (Socket socket = new Socket("localhost", 9000)){
			socket.setSoTimeout(10000);
			
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			out.writeUTF("cert");
			out.writeUTF("psp");
			socket.shutdownOutput();
			
			assertEquals("ERROR:Se esperaba un certificado", new DataInputStream(socket.getInputStream()).readUTF());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
	}
	
	@Test
	@DisplayName("(0,2 puntos) Petición \"cert\" envía datos sin codificar en Base64 (B)")
	void test16() {
		try (Socket socket = new Socket("localhost", 9000)){
			socket.setSoTimeout(10000);
			
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			out.writeUTF("cert");
			out.writeUTF("psp");
			out.writeUTF("*****");
			
			assertEquals("ERROR:Se esperaba Base64", new DataInputStream(socket.getInputStream()).readUTF());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
	}
	
	@Test
	@DisplayName("(3 puntos) Petición \"cifrar\"")
	void test17() {
		try (Socket socket = new Socket("localhost", 9000)) {
			StringBuilder sb = new StringBuilder();
			socket.setSoTimeout(10000);
			String accion = "cifrar";
			String alias = "psp";
			String s = "Entra en tu cuenta de GitHub y haz un fork del repositorio https://github.com/DamFleming/PSP20240304 Cuando se haya completado el fork, clona desde Eclipse tu nuevo repositorio e importa el proyecto. Renombra el proyecto con tu nombre usando el formato siguiente: apellidos, nombre. Deshabilita cualquier conexión a Internet en el ordenador donde realizas el examen. Cuando finalices el examen: Exporta el proyecto a un archivo comprimido. Pide permiso para habilitar de nuevo la conexión de Internet. Entrega el archivo comprimido con el proyecto del examen en la tarea de Teams. Ejecuta un commit & push con el repositorio";
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			out.writeUTF(accion);
			out.writeUTF(alias);
			out.writeUTF(s);
			
			DataInputStream in = new DataInputStream(socket.getInputStream());
			try {
				Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
				while (true) {
					String[] split = in.readUTF().split(":");
					sb.append(new String(cipher.doFinal(Base64.getDecoder().decode(split[1]))));
//					assertEquals("OK")
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
	}
	
	@Test
	@DisplayName("(0,2 puntos) Petición \"cifrar\" sin alias (A)")
	void test18() {
		try (Socket socket = new Socket("localhost", 9000)){
			socket.setSoTimeout(10000);
			
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			out.writeUTF("cifrar");
			
			assertEquals("ERROR:Read timed out", new DataInputStream(socket.getInputStream()).readUTF());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
	}
	
	@Test
	@DisplayName("(0,2 puntos) Petición \"cifrar\" sin alias (B)")
	void test19() {
		try (Socket socket = new Socket("localhost", 9000)){
			socket.setSoTimeout(10000);
			
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			out.writeUTF("cifrar");
			socket.shutdownOutput();
			
			assertEquals("ERROR:Se esperaba un alias", new DataInputStream(socket.getInputStream()).readUTF());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
	}
	
	@Test
	@DisplayName("(0,2 puntos) Petición \"cifrar\" el alias no es válido")
	void test20() {
		try (Socket socket = new Socket("localhost", 9000)){
			socket.setSoTimeout(10000);
			
			String texto = "En un lugar de la Mancha, de cuyo nombre no quiero acordarme ...";
			
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			out.writeUTF("cifrar");
			out.writeUTF("aliasnoválido");
			out.write(texto.getBytes());
			socket.shutdownOutput();
			
			assertEquals("ERROR:'aliasnoválido' no es un certificado", new DataInputStream(socket.getInputStream()).readUTF());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
	}
	
	@Test
	@DisplayName("(0,2 puntos) Petición \"cifrar\" el certificado no contiene una clave RSA")
	void test21() {
		try (Socket socket = new Socket("localhost", 9000)){
			socket.setSoTimeout(1000);
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			out.writeUTF("cert");
			out.writeUTF("alumno");
			out.writeUTF(Base64.getEncoder().encodeToString(ks.getCertificate("alumno").getEncoded()));
			
			new DataInputStream(socket.getInputStream()).readUTF();
		} catch (IOException | CertificateEncodingException | KeyStoreException e) {
			fail(e.getLocalizedMessage());
		}
		
		try (Socket socket = new Socket("localhost", 9000)){
			socket.setSoTimeout(10000);
			
			String texto = "En un lugar de la Mancha, de cuyo nombre no quiero acordarme ...";
			
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			out.writeUTF("cifrar");
			out.writeUTF("alumno");
			out.write(texto.getBytes());
			socket.shutdownOutput();
			
			assertEquals("ERROR:'alumno' no contiene una clave RSA", new DataInputStream(socket.getInputStream()).readUTF());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
	}
	
	@Test
	@DisplayName("(0,2 puntos) Petición \"cifrar\" cliente no envía EOF")
	void test22() {
		try (Socket socket = new Socket("localhost", 9000)){
			socket.setSoTimeout(10000);
			
			String texto = "En un lugar de la Mancha, de cuyo nombre no quiero acordarme ...";
			
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			out.writeUTF("cifrar");
			out.writeUTF("psp");
			out.write(texto.getBytes());
			DataInputStream in = new DataInputStream(socket.getInputStream());
			String s = null;
			try {
				while (true)
					s = in.readUTF();
			} catch (EOFException e) {
				assertTrue(s != null);
				assertEquals("ERROR:Read timed out", s);
			}
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
	}
	
	@Test
	@DisplayName("(0,2 puntos) Petición \"cifrar\" no se envían datos (A)")
	void test23() {
		try (Socket socket = new Socket("localhost", 9000)){
			socket.setSoTimeout(10000);
			
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			out.writeUTF("cifrar");
			out.writeUTF("psp");
			
			assertEquals("ERROR:Read timed out", new DataInputStream(socket.getInputStream()).readUTF());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
	}
	
	@Test
	@DisplayName("(0,2 puntos) Petición \"cifrar\" no se envían datos (B)")
	void test24() {
		try (Socket socket = new Socket("localhost", 9000)){
			socket.setSoTimeout(10000);
			
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			out.writeUTF("cifrar");
			out.writeUTF("psp");
			socket.shutdownOutput();
			
			assertEquals("ERROR:Se esperaban datos", new DataInputStream(socket.getInputStream()).readUTF());
		} catch (IOException e) {
			fail(e.getLocalizedMessage());
		}
	}

} 
