package cliente.udp;

public class PruebaClienteUDP{
    public static void main(String args[]) throws Exception{
        ClienteUDP clienteUDP =new ClienteUDP("10.10.27.29",50000);
        
        clienteUDP.inicia();
    }
}
