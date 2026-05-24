package servidor.tcp;

import datos.Mensaje;
import gui.VentanaChat;
import gui.PanelArchivo;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Servidor TCP multihilo que escucha en un puerto específico y acepta conexiones
 * de múltiples clientes de forma simultánea. Administra hilos de tipo ClienteHandler
 * para procesar mensajes de texto y transferencia de archivos por chunks por separado.
 */
public class ServidorEscuchaTCP2 extends Thread {
    protected ServerSocket serverSocket;
    protected final int PUERTO_SERVER;
    protected volatile boolean ejecutando = true;

    // Lista sincronizada de manejadores de clientes conectados
    protected final List<ClienteHandlerTCP> clientes = Collections.synchronizedList(new ArrayList<>());
    
    // Referencia opcional a la interfaz gráfica principal
    private VentanaChat ventana;

    public ServidorEscuchaTCP2(int puertoS) throws Exception {
        this.PUERTO_SERVER = puertoS;
        // Primitiva LISTEN: abre socket en el puerto configurado
        serverSocket = new ServerSocket(PUERTO_SERVER);
    }

    public void setVentana(VentanaChat ventana) {
        this.ventana = ventana;
    }

    @Override
    public void run() {
        if (ventana != null) {
            ventana.agregarLogMensaje("Servidor TCP", "Iniciado y escuchando en el puerto " + PUERTO_SERVER + "...", true, false);
        }

        try {
            while (ejecutando) {
                try {
                    // Primitiva ACCEPT: Bloqueante, espera a un nuevo cliente
                    Socket clientSocket = serverSocket.accept();

                    if (!ejecutando) {
                        clientSocket.close();
                        break;
                    }

                    if (ventana != null) {
                        ventana.agregarLogMensaje("Servidor TCP", "Cliente conectado desde " + 
                                clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort(), true, false);
                    }

                    // Crear e iniciar el hilo manejador para ese cliente
                    ClienteHandlerTCP handler = new ClienteHandlerTCP(clientSocket);
                    clientes.add(handler);
                    handler.start();

                } catch (SocketException e) {
                    if (!ejecutando) {
                        break; // Servidor cerrado intencionadamente
                    }
                    System.err.println("Error de Socket en bucle de aceptación: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error general en Servidor TCP: " + e.getMessage());
        } finally {
            detener();
        }
    }

    /**
     * Envía un mensaje a todos los clientes TCP activos conectados.
     */
    public void enviarMensajeAClientes(String remitente, String texto) {
        Mensaje mensajeObj = new Mensaje();
        mensajeObj.setTipo("TEXTO");
        mensajeObj.setUsuario(remitente);
        mensajeObj.setMensaje(texto);

        synchronized (clientes) {
            for (ClienteHandlerTCP cliente : clientes) {
                cliente.enviarMensajeObj(mensajeObj);
            }
        }
    }

    /**
     * Retransmite un mensaje recibido a todos los demás clientes (broadcast).
     */
    protected void retransmitirMensaje(Mensaje msg, ClienteHandlerTCP origen) {
        synchronized (clientes) {
            for (ClienteHandlerTCP cliente : clientes) {
                if (cliente != origen) {
                    cliente.enviarMensajeObj(msg);
                }
            }
        }
    }

    /**
     * Detiene el servidor y cierra todas las conexiones de forma segura.
     */
    public void detener() {
        ejecutando = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            System.err.println("Error cerrando ServerSocket: " + e.getMessage());
        }

        synchronized (clientes) {
            for (ClienteHandlerTCP cliente : clientes) {
                cliente.cerrarConexion();
            }
            clientes.clear();
        }

        if (ventana != null) {
            ventana.agregarLogMensaje("Servidor TCP", "Servidor detenido.", true, false);
        }
    }

    /**
     * Hilo interno (Handler) encargado de gestionar la comunicación de un cliente individual.
     */
    protected class ClienteHandlerTCP extends Thread {
        protected Socket socket;
        protected ObjectInputStream in;
        protected ObjectOutputStream out;
        protected volatile boolean activo = true;

        // Atributos para la recepción de archivos por chunks
        private FileOutputStream fileOutputStream = null;
        private File archivoDestino = null;
        private long bytesRecibidos = 0;
        private long totalBytesArchivo = 0;
        private String nombreArchivoActual = "";
        private long tiempoInicioArchivo = 0;

        public ClienteHandlerTCP(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                // Configurar flujos de serialización de objetos
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());

                while (activo) {
                    try {
                        // Leer objeto Mensaje serializado (bloqueante)
                        Object obj = in.readObject();
                        if (obj instanceof Mensaje) {
                            Mensaje msg = (Mensaje) obj;
                            procesarMensaje(msg);
                        }
                    } catch (EOFException | SocketException e) {
                        // Desconexión normal del cliente o pérdida de red
                        break;
                    } catch (Exception e) {
                        System.err.println("Error de lectura de objeto en Handler: " + e.getMessage());
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error inicializando Streams del cliente: " + e.getMessage());
            } finally {
                cerrarConexion();
            }
        }

        private void procesarMensaje(Mensaje msg) {
            String tipo = msg.getTipo();

            if ("TEXTO".equals(tipo)) {
                // 1. Mostrar en la UI del Servidor
                if (ventana != null) {
                    ventana.agregarLogMensaje(msg.getUsuario(), msg.getMensaje(), false, false);
                }
                // 2. Retransmitir a los otros clientes para simular una sala de chat grupal
                retransmitirMensaje(msg, this);

            } else if ("ARCHIVO_INICIO".equals(tipo)) {
                try {
                    nombreArchivoActual = msg.getNombreArchivo();
                    totalBytesArchivo = msg.getTamanioArchivo();
                    bytesRecibidos = 0;
                    tiempoInicioArchivo = System.currentTimeMillis();

                    // Asegurar que exista la carpeta descargas
                    File dirDescargas = new File("descargas");
                    if (!dirDescargas.exists()) {
                        dirDescargas.mkdirs();
                    }

                    // Evitar sobrescribir si el archivo ya existe renombrándolo
                    archivoDestino = new File(dirDescargas, nombreArchivoActual);
                    int count = 1;
                    String ext = "";
                    String base = nombreArchivoActual;
                    int dotIdx = nombreArchivoActual.lastIndexOf('.');
                    if (dotIdx > 0) {
                        base = nombreArchivoActual.substring(0, dotIdx);
                        ext = nombreArchivoActual.substring(dotIdx);
                    }
                    while (archivoDestino.exists()) {
                        archivoDestino = new File(dirDescargas, base + "_" + count + ext);
                        count++;
                    }

                    fileOutputStream = new FileOutputStream(archivoDestino);

                    if (ventana != null) {
                        ventana.agregarLogMensaje("Sistema", "Recibiendo archivo: " + nombreArchivoActual + 
                                " (" + PanelArchivo.formatearBytes(totalBytesArchivo) + ") de " + msg.getUsuario() + "...", true, false);
                        
                        // Mostrar e iniciar el panel de progreso del Servidor
                        ventana.getPanelArchivo().iniciarTransferencia(nombreArchivoActual, totalBytesArchivo);
                    }

                } catch (Exception e) {
                    if (ventana != null) {
                        ventana.agregarLogMensaje("Error", "No se pudo iniciar la descarga de: " + msg.getNombreArchivo(), false, true);
                    }
                    e.printStackTrace();
                }

            } else if ("ARCHIVO_CHUNK".equals(tipo)) {
                try {
                    byte[] chunk = msg.getDatosArchivo();
                    if (fileOutputStream != null && chunk != null) {
                        fileOutputStream.write(chunk);
                        bytesRecibidos += chunk.length;

                        // Calcular métricas solicitadas
                        long tiempoTranscurrido = System.currentTimeMillis() - tiempoInicioArchivo;
                        if (tiempoTranscurrido == 0) tiempoTranscurrido = 1; // Evitar división por cero

                        // tasaBps = (bits enviados) / (tiempo en segundos)
                        double tasaBps = (bytesRecibidos * 8.0) / (tiempoTranscurrido / 1000.0);
                        
                        // tiempo restante estimado
                        long tiempoRestante = 0;
                        if (tasaBps > 0) {
                            tiempoRestante = (long) ((totalBytesArchivo - bytesRecibidos) / (tasaBps / 8.0));
                        }

                        // Actualizar UI del servidor
                        if (ventana != null) {
                            ventana.getPanelArchivo().actualizarProgreso(
                                    bytesRecibidos, totalBytesArchivo, tasaBps, tiempoTranscurrido, tiempoRestante);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error escribiendo chunk de archivo: " + e.getMessage());
                }

            } else if ("ARCHIVO_FIN".equals(tipo)) {
                try {
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                        fileOutputStream = null;
                    }

                    long tiempoTotal = System.currentTimeMillis() - tiempoInicioArchivo;

                    if (ventana != null) {
                        ventana.agregarLogMensaje("Sistema", "Archivo recibido con éxito: " + archivoDestino.getName() +
                                " en la carpeta 'descargas'.", true, false);
                        ventana.getPanelArchivo().finalizarTransferencia(true, tiempoTotal);
                    }

                    // Enviar confirmación al chat para que el cliente lo vea
                    Mensaje ack = new Mensaje();
                    ack.setTipo("TEXTO");
                    ack.setUsuario("Sistema");
                    ack.setMensaje("✓ Archivo '" + nombreArchivoActual + "' recibido e integrado con éxito.");
                    enviarMensajeObj(ack);
                    retransmitirMensaje(ack, this);

                } catch (Exception e) {
                    System.err.println("Error finalizando archivo: " + e.getMessage());
                }
            }
        }

        /**
         * Envía un objeto Mensaje serializado a este cliente.
         */
        public void enviarMensajeObj(Mensaje msg) {
            try {
                if (out != null && activo) {
                    synchronized (out) {
                        out.writeObject(msg);
                        out.flush();
                    }
                }
            } catch (Exception e) {
                System.err.println("Error al enviar mensaje a cliente: " + e.getMessage());
            }
        }

        public void cerrarConexion() {
            if (!activo) return;
            activo = false;
            clientes.remove(this);

            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (Exception ignored) {}

            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception e) {
                System.err.println("Error cerrando sockets del Handler: " + e.getMessage());
            }

            if (ventana != null) {
                ventana.agregarLogMensaje("Servidor TCP", "Cliente desconectado de la sala.", true, false);
            }
        }
    }
}