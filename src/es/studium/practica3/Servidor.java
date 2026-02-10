package es.studium.practica3;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class Servidor extends JFrame {
	private static final long serialVersionUID = 1L;
	private static final int PUERTO = 6000;
	private static final int MAX_JUGADORES = 4;

	private ServerSocket servidor;
	private int numeroSecreto;
	private int conexionesActuales = 0;
	private Socket[] tablaJugadores = new Socket[MAX_JUGADORES];
	private DataOutputStream[] salidas = new DataOutputStream[MAX_JUGADORES];

	private JTextArea textoServidor;
	private JTextField estadoConexiones;

	public Servidor() {
		super("Servidor - Adivina el Número");
		setLayout(new BorderLayout());

		// Panel superior - Estado
		JPanel panelSuperior = new JPanel();
		panelSuperior.setLayout(new FlowLayout());
		panelSuperior.add(new JLabel("Estado: "));
		estadoConexiones = new JTextField("Esperando jugadores...", 20);
		estadoConexiones.setEditable(false);
		panelSuperior.add(estadoConexiones);
		add(panelSuperior, BorderLayout.NORTH);

		// Área central - Log del servidor
		textoServidor = new JTextArea();
		textoServidor.setEditable(false);
		add(new JScrollPane(textoServidor), BorderLayout.CENTER);

		// Panel inferior - Botones
		JPanel panelInferior = new JPanel();
		JButton btnNuevoJuego = new JButton("Nuevo Juego");
		btnNuevoJuego.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				iniciarNuevoJuego();
			}
		});
		panelInferior.add(btnNuevoJuego);

		JButton btnSalir = new JButton("Salir");
		btnSalir.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				salir();
			}
		});
		panelInferior.add(btnSalir);
		add(panelInferior, BorderLayout.SOUTH);

		setSize(500, 400);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);

		iniciarServidor();
	}

	private void iniciarServidor() {
		try {
			servidor = new ServerSocket(PUERTO);
			generarNumeroSecreto();
			textoServidor.append("Servidor iniciado en puerto " + PUERTO + "\n");
			textoServidor.append("Número secreto generado: " + numeroSecreto + "\n");

			// Aceptar conexiones de jugadores
			Thread hiloAceptador = new Thread(new Runnable() {
				@Override
				public void run() {
					while (conexionesActuales < MAX_JUGADORES) {
						try {
							Socket jugador = servidor.accept();
							int indice = conexionesActuales;
							tablaJugadores[indice] = jugador;
							salidas[indice] = new DataOutputStream(jugador.getOutputStream());
							conexionesActuales++;

							actualizarEstado();
							textoServidor.append("Nuevo jugador conectado (Total: " + conexionesActuales + ")\n");

							// Iniciar hilo para este jugador
							new Hilo(jugador, indice, Servidor.this).start();

						} catch (IOException e) {
							textoServidor.append("Error aceptando conexión: " + e.getMessage() + "\n");
						}
					}
					estadoConexiones.setText("Máximo de jugadores alcanzado (" + MAX_JUGADORES + ")");
				}
			});
			hiloAceptador.start();

		} catch (IOException e) {
			textoServidor.append("Error iniciando servidor: " + e.getMessage() + "\n");
		}
	}

	private void generarNumeroSecreto() {
		numeroSecreto = new Random().nextInt(100) + 1;
		textoServidor.append("Nuevo número secreto: " + numeroSecreto + "\n");
	}

	public void iniciarNuevoJuego() {
		generarNumeroSecreto();
		enviarATodos("NUEVO_JUEGO");
		enviarATodos("SERVIDOR> ¡Nuevo juego iniciado! Adivina el número (1-100)");
		textoServidor.append("--- NUEVO JUEGO INICIADO ---\n");
	}

	public void enviarATodos(String mensaje) {
		for (int i = 0; i < conexionesActuales; i++) {
			if (salidas[i] != null) {
				try {
					salidas[i].writeUTF(mensaje);
				} catch (IOException e) {
					textoServidor.append("Error enviando a jugador " + i + "\n");
				}
			}
		}
		textoServidor.append("Para todos: " + mensaje + "\n");
	}

	public void enviarAJugador(int indice, String mensaje) {
		if (salidas[indice] != null) {
			try {
				salidas[indice].writeUTF(mensaje);
			} catch (IOException e) {
				textoServidor.append("Error enviando a jugador " + indice + "\n");
			}
		}
	}

	public void procesarApuesta(int indiceJugador, String nombre, int apuesta) {
		String mensaje = nombre + " apuesta: " + apuesta;
		textoServidor.append(mensaje + "\n");
		enviarATodos(mensaje);

		// Comprobar apuesta
		if (apuesta < numeroSecreto) {
			String respuesta = "SERVIDOR> " + nombre + ": " + apuesta + " es MENOR que el número secreto";
			enviarATodos(respuesta);
		} else if (apuesta > numeroSecreto) {
			String respuesta = "SERVIDOR> " + nombre + ": " + apuesta + " es MAYOR que el número secreto";
			enviarATodos(respuesta);
		} else {
			String respuesta = "SERVIDOR> ¡¡¡" + nombre + " HA ACERTADO!!! El número era " + numeroSecreto;
			enviarATodos(respuesta);
			enviarATodos("FIN_JUEGO");
			textoServidor.append("--- JUEGO TERMINADO - Ganador: " + nombre + " ---\n");
		}
	}

	public void jugadorDesconectado(int indice) {
		if (tablaJugadores[indice] != null) {
			try {
				tablaJugadores[indice].close();
			} catch (IOException e) {}
			tablaJugadores[indice] = null;
			salidas[indice] = null;
			conexionesActuales--;
			actualizarEstado();
			textoServidor.append("Jugador " + indice + " desconectado\n");
		}
	}

	private void actualizarEstado() {
		estadoConexiones.setText("Jugadores conectados: " + conexionesActuales + "/" + MAX_JUGADORES);
	}

	private void salir() {
		try {
			if (servidor != null && !servidor.isClosed()) {
				servidor.close();
			}
			for (int i = 0; i < MAX_JUGADORES; i++) {
				if (tablaJugadores[i] != null) {
					tablaJugadores[i].close();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

	public static void main(String[] args) {
		new Servidor();
	}
}
