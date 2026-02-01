package es.studium.practica3;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class Hilo extends Thread {
	private DataInputStream entrada;
	private Servidor servidor;
	private String nombreJugador;


	public Hilo(Socket socket, Servidor servidor) {
		this.servidor = servidor;

		try {
			entrada = new DataInputStream(socket.getInputStream());
			nombreJugador = entrada.readUTF();
			servidor.enviarMensajeATodos(nombreJugador + " se ha conectado.");
		} catch (IOException e) {
			servidor.enviarMensajeATodos(nombreJugador);
		}
	}


	@Override
	public void run() {
		try {
			while (true) {
				String mensaje = entrada.readUTF();
				procesarApuesta(mensaje); ////////
			}
		} catch (IOException e) {
			servidor.enviarMensajeATodos(nombreJugador + " se ha desconectado.");
		}
	}


	private void procesarApuesta(String mensaje) {
		servidor.enviarMensajeATodos(mensaje);

		String[] partes = mensaje.split(":"); 
		String jugador = partes[0].trim();
		int apuesta = Integer.parseInt(partes[1].trim());
		int numeroSecreto = servidor.getNumeroSecreto();
		String respuesta;

		if (apuesta < numeroSecreto) {
			respuesta = "SERVIDOR: " + jugador + " piensa que el número es " + apuesta + ". Pero el número es mayor.";
		} else if (apuesta > numeroSecreto) {
			respuesta = "SERVIDOR: " + jugador + " piensa que el número es " + apuesta + ". Pero el número es menor.";
		} else {
			respuesta = "SERVIDOR: " + jugador + " piensa que el número es " + apuesta + ". Y HA ACERTADOOOOO!!!";
			servidor.enviarMensajeATodos(respuesta);
			servidor.enviarMensajeATodos("FIN DEL JUEGO.");
			return;
		}
		servidor.enviarMensajeATodos(respuesta);
	}
}