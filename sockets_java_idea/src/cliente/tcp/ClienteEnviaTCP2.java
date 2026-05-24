package cliente.tcp;

import datos.Mensaje;
import gui.PanelArchivo;
import gui.VentanaChat;

import java.net.*;
import java.io.*;
import java.util.Arrays;

/**
 * Hilo que gestiona la comunicación TCP del Cliente. 
 * Actúa como receptor asíncrono en su método run() para actualizar la interfaz al recibir
 * mensajes del servidor, y proporciona métodos síncronos públicos para enviar texto y archivos.
 */
public class ClienteEnviaTCP2 extends Thread {
    protected Socket socket;
    protected final int PUERTO_SERVER;
    protected final String SERVER;
    
    protected ObjectOutputStream out;
    protected ObjectInputStream in;
    protected volatile boolean ejecutando = true;

    // Referencia a la interfaz gráfica principal
    private VentanaChat ventana;

    public ClienteEnviaTCP2(String servidor, int puertoS) throws Exception {
        this.PUERTO_SERVER = puertoS;
        this.SERVER = servidor;

        // Primitiva CONNECT: establece la conexión de red con el Servidor TCP
        socket = new Socket(SERVER, PUERTO_SERVER);
    }

    public void setVentana(VentanaChat ventana) {
        this.ventana = ventana;
    }

    @Override
    public void run() {
        if (ventana != null) {
            ventana.agregarLogMensaje("Sistema", "Conectado al servidor TCP " + SERVER + ":" + PUERTO_SERVER, true, false);
        }

        try {
            // Inicializar Object Streams (Es crucial que el emisor haga flush() primero)
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // Ciclo de escucha infinita para recibir respuestas o mensajes globales del chat
            while (ejecutando) {
                try {
                    Object obj = in.readObject();
                    if (obj instanceof Mensaje) {
                        Mensaje msg = (Mensaje) obj;
                        procesarMensajeRecibido(msg);
                    }
                } catch (EOFException | SocketException e) {
                    // Desconexión normal o forzada del servidor
                    break;
                } catch (Exception e) {
                    System.err.println("Error leyendo objeto en cliente TCP: " + e.getMessage());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error en flujos del cliente: " + e.getMessage());
        } finally {
            detener();
        }
    }

    private void procesarMensajeRecibido(Mensaje msg) {
        if ("TEXTO".equals(msg.getTipo())) {
            if (ventana != null) {
                ventana.agregarLogMensaje(msg.getUsuario(), msg.getMensaje(), false, false);
            }
        }
    }

    /**
     * Envía un mensaje de texto plano al chat grupal.
     */
    public void enviarTexto(String remitente, String texto) {
        try {
            if (out == null || !ejecutando) return;

            Mensaje msg = new Mensaje();
            msg.setTipo("TEXTO");
            msg.setUsuario(remitente);
            msg.setMensaje(texto);

            synchronized (out) {
                out.writeObject(msg);
                out.flush();
            }

            // Reflejar localmente de inmediato en la UI
            if (ventana != null) {
                ventana.agregarLogMensaje(remitente, texto, false, false);
            }

        } catch (Exception e) {
            if (ventana != null) {
                ventana.agregarLogMensaje("Error", "No se pudo enviar el mensaje: " + e.getMessage(), false, true);
            }
        }
    }

    /**
     * Envía un archivo por chunks de red de forma asíncrona, actualizando
     * dinámicamente las métricas de transferencia en el Panel de Progreso.
     */
    public void enviarArchivo(File archivo, PanelArchivo panel, String remitente) throws Exception {
        if (out == null || !ejecutando) {
            throw new IOException("No hay conexión con el servidor.");
        }

        long tiempoInicio = System.currentTimeMillis();
        long bytesEnviados = 0;
        long totalBytes = archivo.length();

        // 1. Mostrar e Inicializar panel de progreso en la GUI
        panel.iniciarTransferencia(archivo.getName(), totalBytes);
        if (ventana != null) {
            ventana.agregarLogMensaje("Sistema", "Iniciando envío de: " + archivo.getName() + 
                    " (" + PanelArchivo.formatearBytes(totalBytes) + ")...", true, false);
        }

        // 2. Enviar cabecera de aviso de archivo
        Mensaje msgInicio = new Mensaje();
        msgInicio.setTipo("ARCHIVO_INICIO");
        msgInicio.setNombreArchivo(archivo.getName());
        msgInicio.setTamanioArchivo(totalBytes);
        msgInicio.setUsuario(remitente);

        synchronized (out) {
            out.writeObject(msgInicio);
            out.flush();
        }

        // 3. Transmisión del archivo por bloques (chunks de 16 KB para alto rendimiento)
        byte[] buffer = new byte[16384]; 
        int bytesRead;

        try (FileInputStream fis = new FileInputStream(archivo)) {
            while ((bytesRead = fis.read(buffer)) != -1) {
                // Crear chunk exacto para enviar solo lo leído
                byte[] chunkReal = Arrays.copyOf(buffer, bytesRead);

                Mensaje msgChunk = new Mensaje();
                msgChunk.setTipo("ARCHIVO_CHUNK");
                msgChunk.setDatosArchivo(chunkReal);
                msgChunk.setUsuario(remitente);

                synchronized (out) {
                    out.writeObject(msgChunk);
                    out.flush();
                }

                bytesEnviados += bytesRead;

                // LÓGICA DE MÉTRICAS DE TRANSFERENCIA (Requerida en la especificación)
                long tiempoTranscurrido = System.currentTimeMillis() - tiempoInicio;
                if (tiempoTranscurrido == 0) tiempoTranscurrido = 1; // Evitar división por cero

                double tasaBps = (bytesEnviados * 8.0) / (tiempoTranscurrido / 1000.0);
                long tiempoRestante = 0;
                if (tasaBps > 0) {
                    tiempoRestante = (long) ((totalBytes - bytesEnviados) / (tasaBps / 8.0));
                }

                // Actualizar la interfaz gráfica de forma segura en tiempo real
                panel.actualizarProgreso(bytesEnviados, totalBytes, tasaBps, tiempoTranscurrido, tiempoRestante);
            }

            // 4. Enviar fin de transferencia de archivo
            Mensaje msgFin = new Mensaje();
            msgFin.setTipo("ARCHIVO_FIN");
            msgFin.setUsuario(remitente);

            synchronized (out) {
                out.writeObject(msgFin);
                out.flush();
            }

            long tiempoTotal = System.currentTimeMillis() - tiempoInicio;
            panel.finalizarTransferencia(true, tiempoTotal);

            if (ventana != null) {
                ventana.agregarLogMensaje("Sistema", "✓ Archivo '" + archivo.getName() + "' enviado exitosamente en " + 
                        String.format("%.2fs", tiempoTotal / 1000.0) + ".", true, false);
            }

        } catch (Exception e) {
            panel.finalizarTransferencia(false, 0);
            throw e;
        }
    }

    /**
     * Cierra de manera ordenada los streams y socket de red del Cliente.
     */
    public void detener() {
        if (!ejecutando) return;
        ejecutando = false;

        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            System.err.println("Error cerrando recursos TCP del cliente: " + e.getMessage());
        }

        if (ventana != null) {
            ventana.agregarLogMensaje("Sistema", "Conexión TCP finalizada.", true, false);
        }
    }
}