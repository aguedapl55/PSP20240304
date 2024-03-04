package dam.psp;

public class Pruebas {
	public static void main(String[] args) {
		String s = "Entra en tu cuenta de GitHub y haz un fork del repositorio\r\n"
				+ "https://github.com/DamFleming/PSP20240304\r\n"
				+ "• Cuando se haya completado el fork, clona desde Eclipse tu nuevo repositorio e importa el\r\n"
				+ "proyecto.\r\n"
				+ "• Renombra el proyecto con tu nombre usando el formato siguiente: apellidos, nombre.\r\n"
				+ "• Deshabilita cualquier conexión a Internet en el ordenador donde realizas el examen.\r\n"
				+ "• Cuando finalices el examen:\r\n"
				+ "o Exporta el proyecto a un archivo comprimido.\r\n"
				+ "o Pide permiso para habilitar de nuevo la conexión de Internet.\r\n"
				+ "o Entrega el archivo comprimido con el proyecto del examen en la tarea de Teams.\r\n"
				+ "o Ejecuta un commit & push con el repositorio";
		System.out.println(s);
		System.out.println(s.getBytes().length);
//		System.out.println(System.getProperty("user.dir") + "\\res\\keystore.p12");
	}
}
