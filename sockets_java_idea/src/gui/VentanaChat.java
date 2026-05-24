package gui;

import datos.Mensaje;
import cliente.tcp.ClienteEnviaTCP2;
import cliente.tcp.ClienteTCP;
import cliente.udp.ClienteEnviaUDP2;
import cliente.udp.ClienteEscuchaUDP2;
import cliente.udp.ClienteUDP;
import servidor.tcp.ServidorEscuchaTCP2;
import servidor.tcp.ServidorTCP;
import servidor.udp.ServidorEscuchaUDP2;
import servidor.udp.ServidorUDP;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Ventana de chat principal (Swing). Implementa una interfaz de usuario
 * premium,
 * moderna y limpia con tema oscuro. Permite iniciar la aplicación tanto en modo
 * Servidor como en modo Cliente, controlando la red TCP/UDP en hilos
 * independientes.
 */
public class VentanaChat extends JFrame {
    // Componentes de Conexión/Modo
    private JPanel panelConfig;
    private JRadioButton rbtnCliente;
    private JRadioButton rbtnServidor;
    private JTextField txtUsuario;
    private JTextField txtIpServer;
    private JTextField txtPuertoTcp;
    private JTextField txtPuertoUdp;
    private JButton btnConectar;
    private JLabel lblEstado;

    // Componentes del Chat
    private JTextPane areaChat;
    private HTMLEditorKit htmlEditorKit;
    private HTMLDocument htmlDocument;
    private JTextField txtEntrada;
    private JButton btnEnviar;
    private JButton btnEnviarArchivo;
    private JLabel lblEstadoUdp;

    // Panel de Archivo
    private PanelArchivo panelArchivo;

    // Instancias de Red y Estado
    private boolean esServidor = true;
    private boolean conectado = false;
    private String nombreUsuario = "Usuario";

    // Servidor
    private ServidorTCP servidorTCP;
    private ServidorUDP servidorUDP;
    private ServidorEscuchaTCP2 escuchaTCP;
    private ServidorEscuchaUDP2 escuchaUDP;

    // Cliente
    private ClienteTCP clienteTCP;
    private ClienteUDP clienteUDP;
    private ClienteEnviaTCP2 clienteEnviaTCP;
    private ClienteEnviaUDP2 clienteEnviaUDP;
    private ClienteEscuchaUDP2 clienteEscuchaUDP;

    // Temporizador para el estado "escribiendo..." de UDP
    private Timer timerEscribiendo;
    private boolean escribiendoNotificado = false;

    // Colores del Tema Oscuro Premium
    private static final Color BG_DARK = new Color(30, 30, 30);
    private static final Color BG_PANEL = new Color(43, 43, 43);
    private static final Color BG_INPUT = new Color(60, 60, 60);
    private static final Color TEXT_LIGHT = new Color(240, 240, 240);
    private static final Color TEXT_MUTED = new Color(160, 160, 160);
    private static final Color ACCENT_BLUE = new Color(0, 168, 255);
    private static final Color ACCENT_GREEN = new Color(46, 204, 113);
    private static final Color ACCENT_RED = new Color(231, 76, 60);

    public VentanaChat() {
        setTitle("Sistema de Chat y Transferencia de Archivos (TCP/UDP)");
        setSize(1270, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(10, 10));

        // Inicializar interfaz gráfica premium
        inicializarUI();
        configurarEstiloGlobal();

        // Registrar cierre ordenado de sockets en hilos al cerrar la ventana
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                desconectarTodo();
            }
        });
    }

    private void inicializarUI() {
        // --- 1. PANEL SUPERIOR: CONFIGURACIÓN Y MODO ---
        panelConfig = new JPanel(new GridBagLayout());
        panelConfig.setBackground(BG_PANEL);
        panelConfig.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(60, 60, 60), 1),
                new javax.swing.border.EmptyBorder(10, 15, 10, 15)
        ));

        ButtonGroup grupoModo = new ButtonGroup();
        rbtnCliente = new JRadioButton("Cliente");
        rbtnCliente.setSelected(true);
        rbtnCliente.setForeground(Color.WHITE);
        rbtnCliente.setBackground(BG_PANEL);
        rbtnCliente.setFont(new Font("Segoe UI", Font.BOLD, 12));

        rbtnServidor = new JRadioButton("Servidor");
        rbtnServidor.setForeground(Color.WHITE);
        rbtnServidor.setBackground(BG_PANEL);
        rbtnServidor.setFont(new Font("Segoe UI", Font.BOLD, 12));

        grupoModo.add(rbtnCliente);
        grupoModo.add(rbtnServidor);

        txtUsuario = new JTextField("Cliente", 8);
        txtIpServer = new JTextField("127.0.0.1", 10);
        txtPuertoTcp = new JTextField("60000", 5);
        txtPuertoUdp = new JTextField("60001", 5);

        estilizarTextField(txtUsuario);
        estilizarTextField(txtIpServer);
        estilizarTextField(txtPuertoTcp);
        estilizarTextField(txtPuertoUdp);

        btnConectar = new JButton("Conectar");
        estilizarBoton(btnConectar, ACCENT_BLUE, Color.WHITE);

        lblEstado = new JLabel("● Desconectado");
        lblEstado.setForeground(TEXT_MUTED);
        lblEstado.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Configuración de GridBagConstraints para alinear los elementos en dos filas
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Fila 0: Entradas de configuración
        // Modo
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.weightx = 0.0;
        panelConfig.add(new JLabel("Modo:"), gbc);

        gbc.gridx = 1;
        panelConfig.add(rbtnCliente, gbc);

        gbc.gridx = 2;
        panelConfig.add(rbtnServidor, gbc);

        // Nombre
        gbc.gridx = 3;
        panelConfig.add(new JLabel("Nombre:"), gbc);

        gbc.gridx = 4; gbc.weightx = 1.0;
        panelConfig.add(txtUsuario, gbc);

        // IP
        gbc.gridx = 5; gbc.weightx = 0.0;
        panelConfig.add(new JLabel("IP Servidor:"), gbc);

        gbc.gridx = 6; gbc.weightx = 1.0;
        panelConfig.add(txtIpServer, gbc);

        // Puerto TCP
        gbc.gridx = 7; gbc.weightx = 0.0;
        panelConfig.add(new JLabel("Puerto TCP:"), gbc);

        gbc.gridx = 8; gbc.weightx = 0.5;
        panelConfig.add(txtPuertoTcp, gbc);

        // Puerto UDP
        gbc.gridx = 9; gbc.weightx = 0.0;
        panelConfig.add(new JLabel("Puerto UDP:"), gbc);

        gbc.gridx = 10; gbc.weightx = 0.5;
        panelConfig.add(txtPuertoUdp, gbc);

        // Fila 1: Acciones y Estado
        gbc.gridy = 1; gbc.gridx = 0; gbc.gridwidth = 3; gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panelConfig.add(btnConectar, gbc);

        gbc.gridx = 3; gbc.gridwidth = 8; gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelConfig.add(lblEstado, gbc);

        add(panelConfig, BorderLayout.NORTH);

        // --- 2. PANEL CENTRAL: HISTORIAL DE CHAT Y ESTADOS ---
        JPanel panelChatPrincipal = new JPanel(new BorderLayout(5, 5));
        panelChatPrincipal.setBackground(BG_DARK);
        panelChatPrincipal.setBorder(new EmptyBorder(10, 10, 10, 10));

        areaChat = new JTextPane();
        areaChat.setEditable(false);
        areaChat.setBackground(new Color(25, 25, 25));
        areaChat.setForeground(TEXT_LIGHT);
        areaChat.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        htmlEditorKit = new HTMLEditorKit();
        areaChat.setEditorKit(htmlEditorKit);
        htmlDocument = (HTMLDocument) areaChat.getDocument();
        // Inyectar CSS base para burbujas y timestamps legibles
        htmlEditorKit.getStyleSheet().addRule(
                "body { font-family: 'Segoe UI', sans-serif; color: #eceff1; background-color: #191919; margin: 10px; }");
        htmlEditorKit.getStyleSheet()
                .addRule(".timestamp { font-size: 10px; color: #888888; font-weight: normal; margin-right: 5px; }");
        htmlEditorKit.getStyleSheet().addRule(".username { font-weight: bold; font-size: 12px; color: #00a8ff; }");
        htmlEditorKit.getStyleSheet().addRule(".username-srv { font-weight: bold; font-size: 12px; color: #2ecc71; }");
        htmlEditorKit.getStyleSheet()
                .addRule(".msg-box { margin-bottom: 8px; border-bottom: 1px solid #2d2d2d; padding-bottom: 5px; }");
        htmlEditorKit.getStyleSheet().addRule(".system-info { font-style: italic; color: #2ecc71; font-size: 11px; }");
        htmlEditorKit.getStyleSheet()
                .addRule(".system-warn { font-style: italic; color: #e74c3c; font-size: 11px; font-weight: bold; }");
        htmlEditorKit.getStyleSheet()
                .addRule(".file-link { color: #f1c40f; text-decoration: underline; font-weight: bold; }");

        JScrollPane scrollChat = new JScrollPane(areaChat);
        scrollChat.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60), 1));
        panelChatPrincipal.add(scrollChat, BorderLayout.CENTER);

        // Panel inferior del chat: barra de escritura + estado UDP
        JPanel panelEscrituraEst = new JPanel(new BorderLayout(5, 5));
        panelEscrituraEst.setBackground(BG_DARK);

        lblEstadoUdp = new JLabel(" ");
        lblEstadoUdp.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblEstadoUdp.setForeground(ACCENT_BLUE);
        panelEscrituraEst.add(lblEstadoUdp, BorderLayout.NORTH);

        JPanel panelEntradaBtc = new JPanel(new BorderLayout(8, 0));
        panelEntradaBtc.setBackground(BG_DARK);

        txtEntrada = new JTextField();
        estilizarTextField(txtEntrada);

        // Botones interactivos
        JPanel panelBotonesAccion = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        panelBotonesAccion.setBackground(BG_DARK);

        btnEnviar = new JButton("Enviar Mensaje");
        estilizarBoton(btnEnviar, ACCENT_GREEN, Color.WHITE);

        btnEnviarArchivo = new JButton("Enviar Archivo");
        estilizarBoton(btnEnviarArchivo, new Color(155, 89, 182), Color.WHITE); // Color púrpura premium

        panelBotonesAccion.add(btnEnviarArchivo);
        panelBotonesAccion.add(btnEnviar);

        panelEntradaBtc.add(txtEntrada, BorderLayout.CENTER);
        panelEntradaBtc.add(panelBotonesAccion, BorderLayout.EAST);

        panelEscrituraEst.add(panelEntradaBtc, BorderLayout.CENTER);
        panelChatPrincipal.add(panelEscrituraEst, BorderLayout.SOUTH);

        add(panelChatPrincipal, BorderLayout.CENTER);

        // --- 3. PANEL LATERAL INFERIOR: PANEL DE ARCHIVO ---
        panelArchivo = new PanelArchivo();
        add(panelArchivo, BorderLayout.SOUTH);

        // --- Habilitar y Deshabilitar Controles según el Modo ---
        ActionListener listenerModos = e -> cambiarModoControles();
        rbtnCliente.addActionListener(listenerModos);
        rbtnServidor.addActionListener(listenerModos);

        cambiarModoControles(); // Inicial

        // --- Configurar Listeners de Botones ---
        btnConectar.addActionListener(e -> toggleConexion());
        btnEnviar.addActionListener(e -> enviarMensajeDeTexto());
        txtEntrada.addActionListener(e -> enviarMensajeDeTexto());
        btnEnviarArchivo.addActionListener(e -> seleccionarYEnviarArchivo());

        // Document Listener para detectar cuándo el usuario está escribiendo (UDP)
        timerEscribiendo = new Timer(1500, evt -> enviarNotificacionEstado("conectado"));
        timerEscribiendo.setRepeats(false);

        txtEntrada.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                mandarEscribiendo();
            }

            public void removeUpdate(DocumentEvent e) {
                mandarEscribiendo();
            }

            public void changedUpdate(DocumentEvent e) {
                mandarEscribiendo();
            }

            private void mandarEscribiendo() {
                if (!conectado || esServidor)
                    return;
                if (!escribiendoNotificado) {
                    escribiendoNotificado = true;
                    enviarNotificacionEstado("escribiendo");
                }
                timerEscribiendo.restart();
            }
        });
    }

    private void configurarEstiloGlobal() {
        // Estilizar los JLabels dentro del panel de configuración
        for (Component c : panelConfig.getComponents()) {
            if (c instanceof JLabel) {
                c.setForeground(Color.WHITE); // Blanco puro para contraste
                c.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
            }
        }
    }

    private void estilizarBoton(JButton boton, Color colorFondo, Color colorTexto) {
        boton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        boton.setBackground(colorFondo);
        boton.setForeground(colorTexto);
        boton.setFocusPainted(false);
        boton.setContentAreaFilled(true);
        boton.setOpaque(true);

        // Borde compuesto para forzar el fondo plano en Windows L&F
        boton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(colorFondo.darker(), 1),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));

        // Limpiar MouseListeners ColorHoverListener previos para evitar duplicación
        for (MouseListener ml : boton.getMouseListeners()) {
            if (ml instanceof ColorHoverListener) {
                boton.removeMouseListener(ml);
            }
        }
        boton.addMouseListener(new ColorHoverListener(boton, colorFondo));
    }

    private static class ColorHoverListener extends MouseAdapter {
        private final JButton button;
        private final Color baseColor;

        public ColorHoverListener(JButton button, Color baseColor) {
            this.button = button;
            this.baseColor = baseColor;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            if (button.isEnabled()) {
                button.setBackground(baseColor.brighter());
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            button.setBackground(baseColor);
        }
    }

    private void estilizarTextField(JTextField campo) {
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        campo.setBackground(new Color(24, 24, 24)); // Fondo oscuro puro
        campo.setForeground(Color.WHITE); // Texto blanco brillante
        campo.setCaretColor(Color.WHITE); // Cursor blanco
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(90, 90, 90), 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
    }

    private void cambiarModoControles() {
        esServidor = rbtnServidor.isSelected();
        if (esServidor) {
            txtUsuario.setText("Servidor");
            txtUsuario.setEnabled(false);
            txtIpServer.setEnabled(false);
            btnConectar.setText("Iniciar Servidor");
            btnEnviarArchivo.setEnabled(false); // Servidor solo escucha archivos de clientes en esta versión
        } else {
            txtUsuario.setText("Cliente");
            txtUsuario.setEnabled(true);
            txtIpServer.setEnabled(true);
            btnConectar.setText("Conectar Cliente");
            btnEnviarArchivo.setEnabled(true);
        }
    }

    private void toggleConexion() {
        if (!conectado) {
            nombreUsuario = txtUsuario.getText().trim();
            if (nombreUsuario.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Por favor introduce tu nombre.");
                return;
            }

            int puertoTCP, puertoUDP;
            try {
                puertoTCP = Integer.parseInt(txtPuertoTcp.getText().trim());
                puertoUDP = Integer.parseInt(txtPuertoUdp.getText().trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Puertos inválidos. Introduce números válidos.");
                return;
            }

            if (esServidor) {
                iniciarModoServidor(puertoTCP, puertoUDP);
            } else {
                String ip = txtIpServer.getText().trim();
                iniciarModoCliente(ip, puertoTCP, puertoUDP);
            }
        } else {
            desconectarTodo();
        }
    }

    /**
     * Inicia los hilos del Servidor TCP y UDP.
     */
    private void iniciarModoServidor(int puertoTCP, int puertoUDP) {
        try {
            agregarLogMensaje("Sistema", "Iniciando servidores en red local...", true, false);

            servidorTCP = new ServidorTCP(puertoTCP);
            // Pasar la referencia de esta VentanaChat para actualizar logs
            escuchaTCP = new ServidorEscuchaTCP2(puertoTCP);
            escuchaTCP.setVentana(this);
            escuchaTCP.start();

            servidorUDP = new ServidorUDP(puertoUDP);
            escuchaUDP = new ServidorEscuchaUDP2(puertoUDP);
            escuchaUDP.setVentana(this);
            escuchaUDP.start();

            conectado = true;
            actualizarEstadoConexion(true, "● Servidor Activo (Escuchando en " + puertoTCP + " / " + puertoUDP + ")");
            rbtnCliente.setEnabled(false);
            rbtnServidor.setEnabled(false);
            txtPuertoTcp.setEnabled(false);
            txtPuertoUdp.setEnabled(false);
            btnConectar.setText("Detener Servidor");
            estilizarBoton(btnConectar, ACCENT_RED, Color.WHITE);

        } catch (Exception e) {
            agregarLogMensaje("Error", "No se pudo iniciar el servidor: " + e.getMessage(), false, true);
        }
    }

    /**
     * Inicia la conexión del Cliente TCP y los listeners del Cliente UDP.
     */
    private void iniciarModoCliente(String ip, int puertoTCP, int puertoUDP) {
        try {
            agregarLogMensaje("Sistema", "Conectando al servidor " + ip + "...", true, false);

            // 1. TCP Connection
            // Creamos socket e hilo emisor/receptor
            clienteTCP = new ClienteTCP(ip, puertoTCP);
            clienteEnviaTCP = new ClienteEnviaTCP2(ip, puertoTCP);
            clienteEnviaTCP.setVentana(this);
            clienteEnviaTCP.start();

            // 2. UDP Socket and Listeners
            DatagramSocket udpSocket = new DatagramSocket(); // Puerto efímero local para enviar y recibir
            clienteEscuchaUDP = new ClienteEscuchaUDP2(udpSocket);
            clienteEscuchaUDP.setVentana(this);
            clienteEscuchaUDP.start();

            clienteEnviaUDP = new ClienteEnviaUDP2(udpSocket, ip, puertoUDP);
            clienteEnviaUDP.setVentana(this);
            clienteEnviaUDP.start();

            conectado = true;
            actualizarEstadoConexion(true, "● Conectado a " + ip);
            rbtnCliente.setEnabled(false);
            rbtnServidor.setEnabled(false);
            txtUsuario.setEnabled(false);
            txtIpServer.setEnabled(false);
            txtPuertoTcp.setEnabled(false);
            txtPuertoUdp.setEnabled(false);
            btnConectar.setText("Desconectar");
            estilizarBoton(btnConectar, ACCENT_RED, Color.WHITE);

            // Enviar notificación de estado conectado mediante UDP
            enviarNotificacionEstado("conectado");

        } catch (Exception e) {
            agregarLogMensaje("Error", "Error al conectar: " + e.getMessage(), false, true);
            desconectarTodo();
        }
    }

    private void desconectarTodo() {
        if (!conectado)
            return;

        agregarLogMensaje("Sistema", "Cerrando conexiones de red...", true, false);

        // Enviar desconexión UDP si es cliente
        if (!esServidor && clienteEnviaUDP != null) {
            enviarNotificacionEstado("desconectado");
        }

        // Detener Cliente
        if (clienteEnviaTCP != null)
            clienteEnviaTCP.detener();
        if (clienteEscuchaUDP != null)
            clienteEscuchaUDP.detener();
        if (clienteEnviaUDP != null)
            clienteEnviaUDP.detener();

        // Detener Servidor
        if (escuchaTCP != null)
            escuchaTCP.detener();
        if (escuchaUDP != null)
            escuchaUDP.detener();

        conectado = false;
        actualizarEstadoConexion(false, "● Desconectado");

        rbtnCliente.setEnabled(true);
        rbtnServidor.setEnabled(true);
        txtUsuario.setEnabled(true);
        txtIpServer.setEnabled(true);
        txtPuertoTcp.setEnabled(true);
        txtPuertoUdp.setEnabled(true);
        btnConectar.setText("Conectar");
        estilizarBoton(btnConectar, ACCENT_BLUE, Color.WHITE);
        lblEstadoUdp.setText(" ");
        cambiarModoControles();
    }

    private void actualizarEstadoConexion(boolean conectado, String statusText) {
        SwingUtilities.invokeLater(() -> {
            lblEstado.setText(statusText);
            if (conectado) {
                lblEstado.setForeground(esServidor ? ACCENT_GREEN : ACCENT_BLUE);
            } else {
                lblEstado.setForeground(TEXT_MUTED);
            }
        });
    }

    private void enviarMensajeDeTexto() {
        if (!conectado) {
            JOptionPane.showMessageDialog(this, "No estás conectado a ninguna red.");
            return;
        }

        String texto = txtEntrada.getText().trim();
        if (texto.isEmpty())
            return;

        if (esServidor) {
            // El servidor en esta versión solo retransmite o muestra.
            // Permite al servidor mandar mensajes al cliente conectado.
            if (escuchaTCP != null) {
                escuchaTCP.enviarMensajeAClientes(nombreUsuario, texto);
                agregarLogMensaje(nombreUsuario, texto, false, false);
            }
        } else {
            // El cliente envía al servidor vía TCP
            if (clienteEnviaTCP != null) {
                clienteEnviaTCP.enviarTexto(nombreUsuario, texto);
            }
        }

        txtEntrada.setText("");
        // Reiniciar estado "escribiendo" en UDP
        if (escribiendoNotificado) {
            escribiendoNotificado = false;
            enviarNotificacionEstado("conectado");
        }
    }

    private void seleccionarYEnviarArchivo() {
        if (!conectado) {
            JOptionPane.showMessageDialog(this, "Debes estar conectado para enviar un archivo.");
            return;
        }
        if (esServidor) {
            JOptionPane.showMessageDialog(this, "El servidor está configurado para recibir archivos únicamente.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Selecciona un archivo para transferir");
        int res = fileChooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File archivo = fileChooser.getSelectedFile();
            if (archivo != null && archivo.exists()) {
                // Iniciar el hilo de envío de archivos para no congelar la UI
                new Thread(() -> {
                    try {
                        clienteEnviaTCP.enviarArchivo(archivo, panelArchivo, nombreUsuario);
                    } catch (Exception ex) {
                        agregarLogMensaje("Error", "Error al transmitir archivo: " + ex.getMessage(), false, true);
                    }
                }).start();
            }
        }
    }

    /**
     * Envía una señal UDP de estado ligero (conectado, desconectado, escribiendo).
     */
    private void enviarNotificacionEstado(String estado) {
        if (esServidor || clienteEnviaUDP == null)
            return;
        new Thread(() -> {
            try {
                clienteEnviaUDP.enviarEstado(nombreUsuario, estado);
                if (estado.equals("conectado")) {
                    escribiendoNotificado = false;
                }
            } catch (Exception ex) {
                System.err.println("Error al enviar notificación de estado por UDP: " + ex.getMessage());
            }
        }).start();
    }

    /**
     * Actualiza la etiqueta de estado UDP ("Usuario está escribiendo...").
     */
    public void actualizarEstadoUdp(String usuario, String estado) {
        SwingUtilities.invokeLater(() -> {
            if (estado.equalsIgnoreCase("escribiendo")) {
                lblEstadoUdp.setText("✍ " + usuario + " está escribiendo...");
            } else if (estado.equalsIgnoreCase("conectado")) {
                lblEstadoUdp.setText(" ");
            } else if (estado.equalsIgnoreCase("desconectado")) {
                lblEstadoUdp.setText("❌ " + usuario + " se ha desconectado");
                // Mostrar temporalmente y borrar
                Timer t = new Timer(3000, e -> lblEstadoUdp.setText(" "));
                t.setRepeats(false);
                t.start();
            }
        });
    }

    /**
     * Agrega un mensaje formateado al historial de chat en HTML.
     */
    public void agregarLogMensaje(String remitente, String contenido, boolean esSistema, boolean esError) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            StringBuilder sb = new StringBuilder();
            sb.append("<div class='msg-box'>");
            sb.append("<span class='timestamp'>[").append(timestamp).append("]</span> ");

            if (esSistema) {
                sb.append("<span class='system-info'>").append(contenido).append("</span>");
            } else if (esError) {
                sb.append("<span class='system-warn'>").append(contenido).append("</span>");
            } else {
                String cl = remitente.equalsIgnoreCase("servidor") ? "username-srv" : "username";
                sb.append("<span class='").append(cl).append("'>").append(remitente).append(":</span> ");
                // Escapar caracteres HTML básicos y parsear saltos de línea
                String textoEscapado = contenido
                        .replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\n", "<br>");
                sb.append(textoEscapado);
            }
            sb.append("</div>");

            try {
                htmlEditorKit.insertHTML(htmlDocument, htmlDocument.getLength(), sb.toString(), 0, 0, null);
                // Auto-scroll al final
                areaChat.setCaretPosition(htmlDocument.getLength());
            } catch (Exception e) {
                System.err.println("Error al renderizar log de chat: " + e.getMessage());
            }
        });
    }

    public PanelArchivo getPanelArchivo() {
        return panelArchivo;
    }

    public static void main(String[] args) {
        // Activar antialiasing en Swing para fuentes limpias
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Intentar usar Look & Feel nativo
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            VentanaChat ventana = new VentanaChat();
            ventana.setVisible(true);
        });
    }
}
