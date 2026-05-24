package servidor.tcp;

public class PruebaServidorTCP{
    public static void main(String args[])throws Exception{
        ServidorTCP servidorTCP=new ServidorTCP(60000);
        
        servidorTCP.inicia();
    }
}
