package cliente.udp;

import java.net.*;

/**
 * Clase principal de inicio para el Cliente UDP.
 * 
 * DECISIÓN DE PROTOCOLO:
 * Se utiliza UDP para el envío rápido y liviano de notificaciones de estado y control (ej. "escribiendo...").
 * - Sin handshake: Reduce latencia en la transmisión de cambios rápidos.
 * - CRC-32 manual: Debido a la falta de fiabilidad inherente de UDP, se introduce una verificación por código de redundancia
 *   cíclica (CRC-32) que se calcula antes del datagrama y se valida estrictamente al recibir.
 */
public class ClienteUDP {
    protected final int PUERTO_SERVER;
    protected final String SERVER;
    
    public ClienteUDP(String servidor, int puertoS) {
        PUERTO_SERVER = puertoS;
        SERVER = servidor;
    }
    
    public void inicia() throws Exception {
        DatagramSocket socket = new DatagramSocket();
        
        ClienteEscuchaUDP2 clienteEscucha = new ClienteEscuchaUDP2(socket);
        ClienteEnviaUDP2 clienteEnvia = new ClienteEnviaUDP2(socket, SERVER, PUERTO_SERVER);
        
        clienteEscucha.start();
        clienteEnvia.start();
    }
}

