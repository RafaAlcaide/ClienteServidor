package es.studium.practica3;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import javax.swing.Timer;

public class Cliente extends JFrame implements ActionListener {
	private static final long serialVersionUID = 1L;
	private Socket socket;
	private DataInputStream entrada;
	private DataOutputStream salida;
	private JTextField txtNombre, txtApuesta;
	private JTextArea textoChat;
	private JButton btnEnviar, btnConectar;
	private String nombreJugador;

	public Cliente() {
		super("Adivina el número secreto");

		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);

		JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.CENTER));
		panelSuperior.add(new JLabel("Nombre:"));
		txtNombre = new JTextField(10);
		panelSuperior.add(txtNombre);
		btnConectar = new JButton("Conectar");
		btnConectar.addActionListener(e -> conectarServidor());
		panelSuperior.add(btnConectar);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		add(panelSuperior, gbc);

		textoChat = new JTextArea(15, 40);
		textoChat.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(textoChat);
		gbc.gridy = 1;
		add(scrollPane, gbc);

		JPanel panelInferior = new JPanel(new FlowLayout(FlowLayout.CENTER));
		panelInferior.add(new JLabel("Tu Apuesta:"));
		txtApuesta = new JTextField(5);
		panelInferior.add(txtApuesta);
		btnEnviar = new JButton("Enviar");
		btnEnviar.setEnabled(false);
		btnEnviar.addActionListener(this);
		panelInferior.add(btnEnviar);
		gbc.gridy = 2;
		add(panelInferior, gbc);

		setSize(500, 400);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setVisible(true);
	}

	private void conectarServidor() {
		nombreJugador = txtNombre.getText().trim().toUpperCase();
		if (nombreJugador.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Introduce un nombre.");
			return;
		}

		try {
			socket = new Socket("localhost", 6000);
			entrada = new DataInputStream(socket.getInputStream());
			salida = new DataOutputStream(socket.getOutputStream());
			salida.writeUTF(nombreJugador);

			btnEnviar.setEnabled(true);
			btnConectar.setEnabled(false);
			txtNombre.setEnabled(false);

			// HILO
			Thread recibirMensajes = new Thread(() -> {
				try {
					while (true) {
						String mensaje = entrada.readUTF();
						textoChat.append(mensaje + "\n");

						if (mensaje.toUpperCase().contains("FIN DEL JUEGO")) {
							SwingUtilities.invokeLater(() -> {
								btnEnviar.setEnabled(false);
								txtApuesta.setEnabled(false);
								JOptionPane.showMessageDialog(Cliente.this, "El juego ha finalizado.");
							});
							break;
						}
					}
				} catch (IOException e) {
					if (!socket.isClosed()) {
						textoChat.append("Desconectado del servidor.\n");
					}
				}
			});
			recibirMensajes.start();

		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "No se pudo conectar con el servidor.");
		}
	}

	// BOTÓN ENVIAR
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnEnviar) {
			String apuestaStr = txtApuesta.getText().trim();
			if (apuestaStr.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Debes introducir un número antes de enviar.");
				return;
			}
			try {
				int apuesta = Integer.parseInt(apuestaStr);
				if (apuesta < 1 || apuesta > 100) {
					JOptionPane.showMessageDialog(this, "Introduce un número entre 1 y 100.");
					return;
				}
				salida.writeUTF(nombreJugador + ": " + apuesta);
				txtApuesta.setText("");

				// Suspensión de 3 segundos
				btnEnviar.setEnabled(false);
				Timer timer = new Timer(3000, evt -> btnEnviar.setEnabled(true)); // Temporizador
				timer.setRepeats(false);
				timer.start();

			} catch (NumberFormatException | IOException ex) {
				JOptionPane.showMessageDialog(this, "Error al enviar la apuesta.");
			}
		}
	}
	public static void main(String[] args) {
		SwingUtilities.invokeLater(Cliente::new);
	}
}