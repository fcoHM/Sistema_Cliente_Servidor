package cliente.tcp;

/**
 * Clase principal de inicio para el Cliente TCP.
 * 
 * DECISIÓN DE PROTOCOLO:
 * Se utiliza TCP para garantizar que:
 * - Los mensajes del chat se entreguen sin pérdidas ni duplicados en orden cronológico exacto.
 * - Los archivos de cualquier tamaño se transmitan con total integridad libre de errores de transmisión.
 */
public class ClienteTCP {
    protected final String SERVER;
    protected final int PUERTO_SERVER;
    
    public ClienteTCP(String servidor, int puertoS) {
        SERVER = servidor;
        PUERTO_SERVER = puertoS;
    }
    
    public ClienteEnviaTCP2 inicia() throws Exception {
        ClienteEnviaTCP2 clienteTCP = new ClienteEnviaTCP2(SERVER, PUERTO_SERVER);
        clienteTCP.start();
        return clienteTCP;
    }
}

