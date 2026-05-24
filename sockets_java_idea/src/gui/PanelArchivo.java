package gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * Panel de progreso y estadísticas para la transferencia de archivos por red (TCP).
 * Muestra barra de progreso, velocidad en bps/Kbps/Mbps, tiempo transcurrido y restante.
 * Diseñado con una estética oscura premium a juego con la ventana de chat.
 */
public class PanelArchivo extends JPanel {
    private JLabel lblNombreArchivo;
    private JProgressBar progressBar;
    private JLabel lblProgresoPct;
    private JLabel lblVelocidad;
    private JLabel lblTiempoTranscurrido;
    private JLabel lblTiempoRestante;
    private JLabel lblDetallesBytes;

    private static final Color BG_DARK = new Color(43, 43, 43);
    private static final Color TEXT_LIGHT = new Color(220, 220, 220);
    private static final Color ACCENT_BLUE = new Color(0, 168, 255);
    private static final Color BG_CARD = new Color(53, 53, 53);

    public PanelArchivo() {
        setLayout(new BorderLayout(10, 10));
        setBackground(BG_DARK);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // Contenedor interno tipo tarjeta premium
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new GridBagLayout());
        cardPanel.setBackground(BG_CARD);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(75, 75, 75), 1, true),
                new EmptyBorder(15, 15, 15, 15)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.weightx = 1.0;

        // Fila 0: Título/Nombre del Archivo
        lblNombreArchivo = new JLabel("Ninguna transferencia en curso");
        lblNombreArchivo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblNombreArchivo.setForeground(TEXT_LIGHT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        cardPanel.add(lblNombreArchivo, gbc);

        // Fila 1: Barra de progreso + Porcentaje
        JPanel progressWrapper = new JPanel(new BorderLayout(10, 0));
        progressWrapper.setBackground(BG_CARD);

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setForeground(ACCENT_BLUE);
        progressBar.setBackground(new Color(30, 30, 30));
        progressBar.setBorder(BorderFactory.createEmptyBorder());
        progressBar.setPreferredSize(new Dimension(150, 16));

        lblProgresoPct = new JLabel("0%");
        lblProgresoPct.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblProgresoPct.setForeground(ACCENT_BLUE);
        lblProgresoPct.setPreferredSize(new Dimension(40, 16));
        lblProgresoPct.setHorizontalAlignment(SwingConstants.RIGHT);

        progressWrapper.add(progressBar, BorderLayout.CENTER);
        progressWrapper.add(lblProgresoPct, BorderLayout.EAST);

        gbc.gridy = 1;
        cardPanel.add(progressWrapper, gbc);

        // Fila 2: Panel de métricas e indicadores estadísticos en cuadrícula
        JPanel metricsPanel = new JPanel(new GridLayout(2, 2, 10, 8));
        metricsPanel.setBackground(BG_CARD);

        lblVelocidad = createMetricLabel("Velocidad: 0 bps");
        lblTiempoTranscurrido = createMetricLabel("Transcurrido: 0s");
        lblTiempoRestante = createMetricLabel("Restante: Estimando...");
        lblDetallesBytes = createMetricLabel("Enviados: 0 B / 0 B");

        metricsPanel.add(lblVelocidad);
        metricsPanel.add(lblDetallesBytes);
        metricsPanel.add(lblTiempoTranscurrido);
        metricsPanel.add(lblTiempoRestante);

        gbc.gridy = 2;
        cardPanel.add(metricsPanel, gbc);

        add(cardPanel, BorderLayout.CENTER);
        setVisible(false); // Ocultar por defecto, se muestra dinámicamente al iniciar una transferencia
    }

    private JLabel createMetricLabel(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        label.setForeground(new Color(180, 180, 180));
        return label;
    }

    /**
     * Inicia visualmente una nueva transferencia de archivo en la UI.
     */
    public void iniciarTransferencia(String nombre, long totalBytes) {
        SwingUtilities.invokeLater(() -> {
            lblNombreArchivo.setText("Transferiendo: " + nombre);
            progressBar.setValue(0);
            lblProgresoPct.setText("0%");
            lblVelocidad.setText("Velocidad: Calculando...");
            lblTiempoTranscurrido.setText("Transcurrido: 0.0s");
            lblTiempoRestante.setText("Restante: Estimando...");
            lblDetallesBytes.setText("Enviado: 0 B / " + formatearBytes(totalBytes));
            setVisible(true);
            revalidate();
            repaint();
        });
    }

    /**
     * Actualiza en tiempo real los valores y progreso de la transferencia en la UI.
     */
    public void actualizarProgreso(long bytesEnviados, long totalBytes, double tasaBps, long tiempoTranscurridoMs, long tiempoRestanteSegundos) {
        SwingUtilities.invokeLater(() -> {
            int pct = (int) (((double) bytesEnviados / totalBytes) * 100);
            progressBar.setValue(pct);
            lblProgresoPct.setText(pct + "%");

            lblVelocidad.setText("Velocidad: " + formatearTasaTransferencia(tasaBps));
            lblTiempoTranscurrido.setText(String.format("Transcurrido: %.1fs", tiempoTranscurridoMs / 1000.0));

            if (bytesEnviados >= totalBytes) {
                lblTiempoRestante.setText("Restante: Completado");
            } else if (tiempoRestanteSegundos < 0 || Double.isInfinite(tasaBps) || tasaBps == 0) {
                lblTiempoRestante.setText("Restante: Estimando...");
            } else {
                lblTiempoRestante.setText(String.format("Restante: ~%ds", tiempoRestanteSegundos));
            }

            lblDetallesBytes.setText("Progreso: " + formatearBytes(bytesEnviados) + " / " + formatearBytes(totalBytes));
        });
    }

    /**
     * Oculta el panel o lo reinicia cuando termina una transferencia.
     */
    public void finalizarTransferencia(boolean completadaExito, long tiempoTotalMs) {
        SwingUtilities.invokeLater(() -> {
            if (completadaExito) {
                lblNombreArchivo.setText("¡Transferencia completada!");
                progressBar.setValue(100);
                lblProgresoPct.setText("100%");
                lblTiempoRestante.setText("Restante: Finalizado");
                lblTiempoTranscurrido.setText(String.format("Tiempo total: %.2fs", tiempoTotalMs / 1000.0));
            } else {
                lblNombreArchivo.setText("Transferencia cancelada o fallida");
                lblTiempoRestante.setText("Restante: Error");
            }
            // Después de 5 segundos, ocultamos el panel suavemente
            Timer timer = new Timer(5000, e -> {
                setVisible(false);
            });
            timer.setRepeats(false);
            timer.start();
        });
    }

    /**
     * Formatea los bytes a una cadena legible (B, KB, MB, GB).
     */
    public static String formatearBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.2f %cB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Formatea la tasa de bits a una cadena legible (bps, Kbps, Mbps).
     */
    public static String formatearTasaTransferencia(double bps) {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        if (bps < 1000) {
            return df.format(bps) + " bps";
        } else if (bps < 1000000) {
            return df.format(bps / 1000.0) + " Kbps";
        } else {
            return df.format(bps / 1000000.0) + " Mbps";
        }
    }
}
