package servidor.udp;

public class PruebaServidorUDP{
    public static void main(String args[]) throws Exception{
        ServidorUDP servidorUDP=new ServidorUDP(50000);
        
        servidorUDP.inicia(); 
    }
}
