package dam.psp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Cliente {
	public static void main(String[] args) throws UnknownHostException, IOException {
		Socket socket = new Socket("localhost", 9000);
		try (DataInputStream in = new DataInputStream(socket.getInputStream());
				DataOutputStream out = new DataOutputStream(socket.getOutputStream());
				DataInputStream keyIn = new DataInputStream(System.in)) {
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
