package es.studium.practica3;

import java.awt.BorderLayout;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class Servidor extends JFrame {
	private static final long serialVersionUID = 1L;
	private static final int PUERTO = 6000;
	private ServerSocket servidor;
	private int numeroSecreto;
	private List<Socket> clientes = new ArrayList<>();
	private JTextArea textoServidor;

	public Servidor() {
		super("Adivina el Número Secreto");

		setLayout(new BorderLayout());
		textoServidor = new JTextArea();
		textoServidor.setEditable(false);
		add(new JScrollPane(textoServidor), BorderLayout.CENTER);
		setSize(500, 400);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);

		iniciarServidor();
	}

	private synchronized void nuevoNumero() {
		numeroSecreto = new Random().nextInt(100) + 1;
		System.out.println(numeroSecreto);
		textoServidor.append("Número aleatorio generado.\n");
	}

	private void iniciarServidor() {
		try {
			servidor = new ServerSocket(PUERTO);	
			nuevoNumero();
			textoServidor.append("Servidor iniciado.\nEsperando jugadores...\n");

			while (true) {
				Socket cliente = servidor.accept(); 
				clientes.add(cliente); 
				new Hilo(cliente, this).start(); ////////////
			}
		} catch (IOException e) {
			textoServidor.append("Error en el servidor: " + e.getMessage() + "\n");
		}
	}

	public synchronized void enviarMensajeATodos(String mensaje) {
		for (Socket cliente : clientes) {
			try {
				DataOutputStream salida = new DataOutputStream(cliente.getOutputStream());
				salida.writeUTF(mensaje);
			} catch (IOException ignored) {}
		}
		textoServidor.append(mensaje + "\n"); 
	}

	public int getNumeroSecreto() {
		return numeroSecreto;
	}

	public static void main(String[] args) {
		new Servidor();
	}
}