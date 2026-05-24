package servidor.tcp;

/**
 * Clase principal de inicio para el Servidor TCP.
 * 
 * DECISIÓN DE PROTOCOLO:
 * Se utiliza TCP para la transmisión de mensajes de chat de texto y la transferencia de archivos.
 * - Confiabilidad garantizada: TCP proporciona detección de errores y retransmisiones automáticas.
 * - Orden secuencial: Garantiza que los chunks de archivos y las frases de texto lleguen en el orden exacto en que se enviaron.
 * - Control de congestión y flujo: Evita saturar la red local al transferir archivos de gran tamaño.
 */
public class ServidorTCP {
    protected final int PUERTO_SERVER;
    
    public ServidorTCP(int puertoS) {
        PUERTO_SERVER = puertoS;
    }
    
    public ServidorEscuchaTCP2 inicia() throws Exception {
        ServidorEscuchaTCP2 servidorTCP = new ServidorEscuchaTCP2(PUERTO_SERVER);
        servidorTCP.start();
        return servidorTCP;
    }
}

