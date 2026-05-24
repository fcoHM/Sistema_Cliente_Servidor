package cliente.udp;

import datos.Mensaje;
import gui.VentanaChat;

import java.net.*;
import java.io.*;

/**
 * Hilo receptor UDP en el Cliente. Escucha señales ligeras de estado provenientes del servidor.
 * Realiza de forma obligatoria una verificación de checksum manual CRC-32 para evitar procesar
 * estados corrompidos, informando del resultado de la validación en la interfaz gráfica.
 */
public class ClienteEscuchaUDP2 extends Thread {
    protected final int PUERTO_CLIENTE;
    protected DatagramSocket socket;
    protected volatile boolean ejecutando = true;

    // Tamaño de buffer máximo UDP seguro para deserialización de objetos
    private static final int MAX_BUFFER = 65535;

    // Referencia a la interfaz gráfica principal
    private VentanaChat ventana;

    public ClienteEscuchaUDP2(DatagramSocket socketNuevo) {
        this.socket = socketNuevo;
        this.PUERTO_CLIENTE = socket.getLocalPort();
    }

    public void setVentana(VentanaChat ventana) {
        this.ventana = ventana;
    }

    @Override
    public void run() {
        if (ventana != null) {
            ventana.agregarLogMensaje("Cliente UDP", "Escuchando notificaciones en puerto local " + PUERTO_CLIENTE, true, false);
        }

        try {
            while (ejecutando) {
                try {
                    // Recibir datagrama de red
                    Mensaje mensajeObj = recibeMensaje();
                    
                    // Procesar y validar la integridad con CRC-32
                    procesarMensaje(mensajeObj);

                } catch (SocketTimeoutException e) {
                    // Timeout silencioso
                } catch (SocketException e) {
                    if (socket.isClosed()) {
                        break; // Socket cerrado intencionalmente
                    }
                    System.err.println("Error de socket UDP en escucha: " + e.getMessage());
                    ejecutando = false;
                } catch (Exception e) {
                    if (ventana != null && ejecutando) {
                        ventana.agregarLogMensaje("Error UDP", "Error recibiendo estado UDP: " + e.getMessage(), false, true);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error general en escucha UDP: " + e.getMessage());
        } finally {
            detener();
        }
    }

    private void procesarMensaje(Mensaje mensajeObj) {
        if (mensajeObj == null) return;

        // VERIFICACIÓN MANUAL DE INTEGRIDAD CON CRC-32
        long checksumRecibido = mensajeObj.getChecksum();
        long checksumCalculado = mensajeObj.calcularChecksum();

        if (checksumRecibido != checksumCalculado) {
            // Checksum inválido: paquete descartado y reporte de error de integridad
            if (ventana != null) {
                ventana.agregarLogMensaje("Alerta UDP", "¡ERROR DE INTEGRIDAD CRC-32! Checksum recibido: " + 
                        checksumRecibido + " | Calculado: " + checksumCalculado + 
                        ". Estado descartado por corrupción.", false, true);
            }
            return;
        }

        // Lógica de Estado
        if ("ESTADO".equalsIgnoreCase(mensajeObj.getTipo())) {
            String usuario = mensajeObj.getUsuario();
            String estado = mensajeObj.getMensaje();

            if (ventana != null) {
                ventana.actualizarEstadoUdp(usuario, estado);
            }
        }
    }

    private Mensaje recibeMensaje() throws Exception {
        byte[] buffer = new byte[MAX_BUFFER];
        DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);

        // Primitiva RECEIVE: Bloqueante en espera del datagrama UDP
        socket.receive(paquete);

        // Deserializar el arreglo de bytes en un objeto Mensaje
        ByteArrayInputStream bais = new ByteArrayInputStream(paquete.getData(), 0, paquete.getLength());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Mensaje mensajeObj = (Mensaje) ois.readObject();

        return mensajeObj;
    }

    public void detener() {
        ejecutando = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (ventana != null) {
            ventana.agregarLogMensaje("Cliente UDP", "Escucha finalizada.", true, false);
        }
    }
}