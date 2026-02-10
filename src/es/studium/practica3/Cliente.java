package es.studium.practica3;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class Cliente extends JFrame implements ActionListener {
	private static final long serialVersionUID = 1L;

	private Socket socket;
	private DataInputStream entrada;
	private DataOutputStream salida;
	private String nombre;
	private boolean conectado = true;

	// Componentes de la interfaz
	private JTextArea areaChat;
	private JTextField campoMensaje;
	private JTextField campoNombre;
	private JButton btnEnviar, btnConectar, btnSalir;
	private JLabel estado;

	public Cliente() {
		super("Cliente - Adivina el Número");
		setLayout(new BorderLayout());

		// Panel superior - Conexión
		JPanel panelSuperior = new JPanel(new FlowLayout());
		panelSuperior.add(new JLabel("Nombre:"));
		campoNombre = new JTextField(10);
		panelSuperior.add(campoNombre);

		btnConectar = new JButton("Conectar");
		btnConectar.addActionListener(this);
		panelSuperior.add(btnConectar);

		estado = new JLabel("Desconectado");
		panelSuperior.add(estado);
		add(panelSuperior, BorderLayout.NORTH);

		// Área central - Mensajes
		areaChat = new JTextArea();
		areaChat.setEditable(false);
		add(new JScrollPane(areaChat), BorderLayout.CENTER);

		// Panel inferior - Apuestas
		JPanel panelInferior = new JPanel(new FlowLayout());
		panelInferior.add(new JLabel("Tu apuesta (1-100):"));
		campoMensaje = new JTextField(5);
		campoMensaje.setEnabled(false);
		panelInferior.add(campoMensaje);

		btnEnviar = new JButton("Enviar Apuesta");
		btnEnviar.setEnabled(false);
		btnEnviar.addActionListener(this);
		panelInferior.add(btnEnviar);

		btnSalir = new JButton("Salir");
		btnSalir.addActionListener(this);
		panelInferior.add(btnSalir);
		add(panelInferior, BorderLayout.SOUTH);

		// Configuración de la ventana
		setSize(500, 400);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}

	private void conectar() {
		nombre = campoNombre.getText().trim();
		if (nombre.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Introduce un nombre");
			return;
		}

		try {
			socket = new Socket("localhost", 6000);
			entrada = new DataInputStream(socket.getInputStream());
			salida = new DataOutputStream(socket.getOutputStream());

			// Enviar nombre al servidor
			salida.writeUTF(nombre);

			// Actualizar interfaz
			estado.setText("Conectado como: " + nombre);
			estado.setForeground(Color.GREEN);
			btnConectar.setEnabled(false);
			campoNombre.setEnabled(false);
			btnEnviar.setEnabled(true);
			campoMensaje.setEnabled(true);
			campoMensaje.requestFocus();

			// Iniciar hilo de recepción
			Thread hiloReceptor = new Thread(new Runnable() {
				@Override
				public void run() {
					recibirMensajes();
				}
			});
			hiloReceptor.start();

		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Error conectando al servidor");
		}
	}

	// MÉTODO PRINCIPAL DE RECEPCIÓN
	private void recibirMensajes() {
		try {
			while (conectado) {
				String mensaje = entrada.readUTF();

				// Actualizar en el hilo de Swing
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						areaChat.append(mensaje + "\n");

						// Deshabilitar si el juego terminó
						if (mensaje.contains("HA ACERTADO") || mensaje.equals("FIN_JUEGO")) {
							btnEnviar.setEnabled(false);
							campoMensaje.setEnabled(false);
							areaChat.append("--- JUEGO TERMINADO ---\n");
						}

						// Si es nuevo juego, habilitar
						if (mensaje.equals("NUEVO_JUEGO")) {
							btnEnviar.setEnabled(true);
							campoMensaje.setEnabled(true);
							campoMensaje.requestFocus();
						}
					}
				});
			}
		} catch (IOException e) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					areaChat.append("--- DESCONECTADO DEL SERVIDOR ---\n");
					estado.setText("Desconectado");
					estado.setForeground(Color.RED);
				}
			});
		}
	}

	private void enviarApuesta() {
		String texto = campoMensaje.getText().trim();
		try {
			int apuesta = Integer.parseInt(texto);
			if (apuesta < 1 || apuesta > 100) {
				JOptionPane.showMessageDialog(this, "El número debe estar entre 1 y 100");
				return;
			}

			salida.writeUTF("APUESTA:" + apuesta);
			campoMensaje.setText("");
			campoMensaje.requestFocus();

		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Introduce un número válido");
		} catch (IOException e) {
			areaChat.append("Error enviando apuesta\n");
		}
	}

	private void desconectar() {
		conectado = false;
		try {
			if (salida != null) {
				salida.writeUTF("DESCONECTAR");
			}
			if (socket != null && !socket.isClosed()) {
				socket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnConectar) {
			conectar();
		} else if (e.getSource() == btnEnviar) {
			enviarApuesta();
		} else if (e.getSource() == btnSalir) {
			desconectar();
		}
	}

	public static void main(String[] args) {
		new Cliente();
	}
}
