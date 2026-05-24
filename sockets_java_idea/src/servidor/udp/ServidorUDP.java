package servidor.udp;

/**
 * Clase principal de inicio para el Servidor UDP.
 * 
 * DECISIÓN DE PROTOCOLO:
 * Se utiliza UDP para señales ligeras de control y notificaciones de estado (como "escribiendo...", "conectado", "desconectado").
 * - Bajo overhead: No requiere establecimiento de conexión formal, ideal para eventos de alta frecuencia y baja prioridad.
 * - Latencia mínima: El envío de datagramas es inmediato.
 * - Integridad manual: Dado que UDP no garantiza que el contenido llegue sin errores, implementamos un algoritmo de CRC-32 manual
 *   en el objeto Mensaje, calculándolo en el emisor y verificándolo minuciosamente en el receptor.
 */
public class ServidorUDP {
    public final int PUERTO_SERVER;
    
    public ServidorUDP(int puertoS) {
        PUERTO_SERVER = puertoS;
    }
    
    public ServidorEscuchaUDP2 inicia() throws Exception {
        ServidorEscuchaUDP2 servidorUDP = new ServidorEscuchaUDP2(PUERTO_SERVER);
        servidorUDP.start();
        return servidorUDP;
    }
}

