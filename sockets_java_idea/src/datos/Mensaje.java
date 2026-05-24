package datos;

import java.io.Serializable;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/**
 * Clase de datos compartida que encapsula los mensajes de texto, notificaciones de estado y
 * bloques de archivos transmitidos en la red local. Implementa Serializable para permitir
 * la transmisión de objetos en flujos TCP y datagramas UDP.
 */
public class Mensaje implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String mensaje; // Cadena de texto del chat o descripción del estado
    protected int puertoServidor;
    protected int puertoCliente;
    protected InetAddress addressCliente;
    protected InetAddress addressServidor;

    // Campos extendidos solicitados para el chat completo y transferencia de archivos
    protected String tipo;            // "TEXTO", "ARCHIVO_INICIO", "ARCHIVO_CHUNK", "ARCHIVO_FIN", "ESTADO"
    protected long checksum;          // Valor CRC-32 calculado manualmente para validación en UDP
    protected String nombreArchivo;   // Nombre del archivo que se está transfiriendo
    protected long tamanioArchivo;    // Tamaño total del archivo en bytes
    protected byte[] datosArchivo;    // Bloque/chunk de bytes del archivo transmitido
    protected String usuario;         // Nombre del remitente
    protected long timestamp;         // Marca de tiempo del mensaje para la visualización del chat

    public Mensaje() {
        this.timestamp = System.currentTimeMillis();
    }

    public Mensaje(String mensaje, int puertoServidor, int puertoCliente, InetAddress addressCliente, InetAddress addressServidor) {
        this();
        this.mensaje = mensaje;
        this.puertoServidor = puertoServidor;
        this.puertoCliente = puertoCliente;
        this.addressCliente = addressCliente;
        this.addressServidor = addressServidor;
    }

    /**
     * Calcula manualmente el checksum del paquete utilizando el algoritmo CRC-32 estándar
     * sobre los campos críticos del mensaje. Esto se utiliza para la detección de errores en UDP.
     */
    public long calcularChecksum() {
        CRC32 crc = new CRC32();
        if (mensaje != null) {
            crc.update(mensaje.getBytes(StandardCharsets.UTF_8));
        }
        if (tipo != null) {
            crc.update(tipo.getBytes(StandardCharsets.UTF_8));
        }
        if (usuario != null) {
            crc.update(usuario.getBytes(StandardCharsets.UTF_8));
        }
        // También incluimos el timestamp para asegurar la unicidad y detectar duplicados leves
        byte[] tsBytes = java.nio.ByteBuffer.allocate(Long.BYTES).putLong(timestamp).array();
        crc.update(tsBytes);
        return crc.getValue();
    }

    // Getters y Setters
    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public int getPuertoServidor() {
        return puertoServidor;
    }

    public void setPuertoServidor(int puertoServidor) {
        this.puertoServidor = puertoServidor;
    }

    public int getPuertoCliente() {
        return puertoCliente;
    }

    public void setPuertoCliente(int puertoCliente) {
        this.puertoCliente = puertoCliente;
    }

    public InetAddress getAddressCliente() {
        return addressCliente;
    }

    public void setAddressCliente(InetAddress addressCliente) {
        this.addressCliente = addressCliente;
    }

    public InetAddress getAddressServidor() {
        return addressServidor;
    }

    public void setAddressServidor(InetAddress addressServidor) {
        this.addressServidor = addressServidor;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public long getChecksum() {
        return checksum;
    }

    public void setChecksum(long checksum) {
        this.checksum = checksum;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public long getTamanioArchivo() {
        return tamanioArchivo;
    }

    public void setTamanioArchivo(long tamanioArchivo) {
        this.tamanioArchivo = tamanioArchivo;
    }

    public byte[] getDatosArchivo() {
        return datosArchivo;
    }

    public void setDatosArchivo(byte[] datosArchivo) {
        this.datosArchivo = datosArchivo;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

