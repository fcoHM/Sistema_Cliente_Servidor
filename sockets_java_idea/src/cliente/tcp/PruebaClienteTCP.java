package cliente.tcp;

public class PruebaClienteTCP{
    public static void main(String args[])throws Exception{
        ClienteTCP clienteTCP =new ClienteTCP("10.10.27.29",60000);
             
        clienteTCP.inicia();
    }
}
