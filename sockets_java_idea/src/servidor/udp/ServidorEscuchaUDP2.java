package servidor.udp;

import datos.Mensaje;
import gui.VentanaChat;

import java.net.*;
import java.io.*;

/**
 * Hilo receptor UDP en el Servidor. Escucha señales ligeras de estado de los clientes.
 * Implementa una verificación manual y rigurosa de integridad mediante CRC-32.
 * Si un paquete tiene un checksum inválido, se descarta inmediatamente y se alerta en la UI.
 */
public class ServidorEscuchaUDP2 extends Thread {
    protected DatagramSocket socket;
    protected final int PUERTO_SERVER;
    protected volatile boolean ejecutando = true;

    // Tamaño de buffer máximo UDP seguro para deserialización de objetos
    private static final int MAX_BUFFER = 65535;

    // Referencia a la interfaz gráfica principal
    private VentanaChat ventana;

    public ServidorEscuchaUDP2(int puertoS) throws Exception {
        this.PUERTO_SERVER = puertoS;
        // Iniciar socket UDP en el puerto configurado
        socket = new DatagramSocket(PUERTO_SERVER);
    }

    public void setVentana(VentanaChat ventana) {
        this.ventana = ventana;
    }

    @Override
    public void run() {
        if (ventana != null) {
            ventana.agregarLogMensaje("Servidor UDP", "Iniciado y escuchando en el puerto " + PUERTO_SERVER + "...", true, false);
        }

        try {
            while (ejecutando) {
                try {
                    // Recibir datagrama de red
                    Mensaje mensajeObj = recibeMensaje();
                    
                    // Procesar la integridad (CRC-32) y la lógica de estado
                    procesaMensaje(mensajeObj);

                } catch (SocketException e) {
                    if (!ejecutando) {
                        break; // Socket cerrado intencionalmente
                    }
                    System.err.println("Error de Socket en Servidor UDP: " + e.getMessage());
                } catch (SocketTimeoutException e) {
                    // Timeout silencioso para volver a evaluar bucle
                } catch (Exception e) {
                    if (ventana != null && ejecutando) {
                        ventana.agregarLogMensaje("Error UDP", "Error procesando datagrama: " + e.getMessage(), false, true);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error general en Servidor UDP: " + e.getMessage());
        } finally {
            detener();
        }
    }

    private void procesaMensaje(Mensaje mensajeObj) throws Exception {
        if (mensajeObj == null) return;

        // VERIFICACIÓN MANUAL DE INTEGRIDAD CON CRC-32 (UDP no lo garantiza)
        long checksumRecibido = mensajeObj.getChecksum();
        long checksumCalculado = mensajeObj.calcularChecksum();

        if (checksumRecibido != checksumCalculado) {
            // Checksum inválido: paquete corrupto. Se descarta y se notifica al usuario
            if (ventana != null) {
                ventana.agregarLogMensaje("Alerta UDP", "¡ERROR DE INTEGRIDAD CRC-32! Checksum recibido: " + 
                        checksumRecibido + " | Calculado: " + checksumCalculado + 
                        ". Paquete descartado por corrupción de datos.", false, true);
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

        // Bloqueante: recibe el datagrama UDP
        socket.receive(paquete);

        // Deserializar el objeto Mensaje a partir de los bytes recibidos
        ByteArrayInputStream bais = new ByteArrayInputStream(paquete.getData(), 0, paquete.getLength());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Mensaje mensajeObj = (Mensaje) ois.readObject();

        // Guardar dirección del remitente para responder si es necesario
        mensajeObj.setAddressCliente(paquete.getAddress());
        mensajeObj.setPuertoCliente(paquete.getPort());

        return mensajeObj;
    }

    public void detener() {
        ejecutando = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (ventana != null) {
            ventana.agregarLogMensaje("Servidor UDP", "Servidor UDP detenido.", true, false);
        }
    }
}