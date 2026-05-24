package cliente.udp;

import datos.Mensaje;
import gui.VentanaChat;

import java.net.*;
import java.io.*;

/**
 * Hilo que gestiona el envío de notificaciones de estado por UDP del Cliente.
 * Antes de transmitir, calcula de forma manual el CRC-32 del contenido del
 * mensaje
 * y lo inyecta en el objeto Mensaje, que luego se serializa y se transmite en
 * un DatagramPacket.
 */
public class ClienteEnviaUDP2 extends Thread {
    protected final int PUERTO_SERVER;
    protected final String SERVER;
    protected DatagramSocket socket;
    protected InetAddress addressServer;
    protected volatile boolean ejecutando = true;

    // Referencia a la interfaz gráfica principal
    private VentanaChat ventana;

    public ClienteEnviaUDP2(DatagramSocket nuevoSocket, String servidor, int puertoServidor) throws Exception {
        this.socket = nuevoSocket;
        this.SERVER = servidor;
        this.PUERTO_SERVER = puertoServidor;

        // Primitiva de resolución: Obtiene la IP del servidor
        this.addressServer = InetAddress.getByName(SERVER);
    }

    public void setVentana(VentanaChat ventana) {
        this.ventana = ventana;
    }

    @Override
    public void run() {
        // En esta versión controlada por la GUI, el envío es guiado por eventos del
        // chat (no bloqueante en teclado).
        // Mantenemos el hilo vivo de forma segura por compatibilidad o terminación
        // limpia.
        try {
            while (ejecutando) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            // Terminación limpia
        }
    }

    /**
     * Envía una notificación de estado UDP (ej. "escribiendo", "conectado",
     * "desconectado")
     * al servidor remoto, incluyendo el cálculo manual del Checksum CRC-32.
     */
    public void enviarEstado(String remitente, String estado) throws Exception {
        if (socket == null || socket.isClosed() || !ejecutando) {
            return;
        }

        Mensaje mensajeObj = new Mensaje();
        mensajeObj.setTipo("ESTADO");
        mensajeObj.setMensaje(estado);
        mensajeObj.setUsuario(remitente);
        mensajeObj.setTimestamp(System.currentTimeMillis());

        // CÁLCULO MANUAL DEL CHECKSUM CRC-32 (Para garantizar integridad sobre UDP)
        long crc32 = mensajeObj.calcularChecksum();
        mensajeObj.setChecksum(crc32);

        // Serializar el objeto Mensaje a un arreglo de bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(mensajeObj);
        oos.flush();

        byte[] buffer = baos.toByteArray();

        // Crear y enviar el DatagramPacket por el socket UDP
        DatagramPacket paquete = new DatagramPacket(buffer, buffer.length, addressServer, PUERTO_SERVER);
        socket.send(paquete);
    }

    public void detener() {
        ejecutando = false;
        // No cerramos el socket aquí si es compartido con el hilo de escucha
    }
}