package es.studium.practica3;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Hilo extends Thread {
	private Socket socket;
	private DataInputStream entrada;
	private DataOutputStream salida;
	private Servidor servidor;
	private int indice;
	private String nombreJugador;
	private boolean activo = true;

	public Hilo(Socket socket, int indice, Servidor servidor) {
		this.socket = socket;
		this.indice = indice;
		this.servidor = servidor;

		try {
			entrada = new DataInputStream(socket.getInputStream());
			salida = new DataOutputStream(socket.getOutputStream());

			// Recibir nombre del jugador
			nombreJugador = entrada.readUTF();
			servidor.enviarATodos("SERVIDOR> " + nombreJugador + " se ha unido al juego");

			// Enviar mensaje de bienvenida
			salida.writeUTF("SERVIDOR> ¡Bienvenido " + nombreJugador + "!");
			salida.writeUTF("SERVIDOR> Adivina el número entre 1 y 100");

		} catch (IOException e) {
			System.out.println("Error creando hilo para jugador " + indice);
		}
	}

	@Override
	public void run() {
		try {
			while (activo) {
				// Esperar mensaje del cliente
				String mensaje = entrada.readUTF();

				if (mensaje.equals("DESCONECTAR")) {
					// Jugador quiere salir
					servidor.enviarATodos("SERVIDOR> " + nombreJugador + " ha abandonado el juego");
					activo = false;
				} else if (mensaje.startsWith("APUESTA:")) {
					// Procesar apuesta
					try {
						int apuesta = Integer.parseInt(mensaje.substring(8));
						servidor.procesarApuesta(indice, nombreJugador, apuesta);
					} catch (NumberFormatException e) {
						salida.writeUTF("SERVIDOR> Error: Apuesta no válida");
					}
				}
			}
		} catch (IOException e) {
			// Jugador desconectado inesperadamente
			if (activo) {
				servidor.enviarATodos("SERVIDOR> " + nombreJugador + " se desconectó");
			}
		} finally {
			servidor.jugadorDesconectado(indice);
			try {
				socket.close();
			} catch (IOException e) {}
		}
	}
}
