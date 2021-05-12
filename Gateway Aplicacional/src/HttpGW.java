import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class HttpGW {
    int tcp_port = 8080;
    static int udp_port = 8888;

    /* pool de servidores */
    //Map<InetAddress, FastFileServer> servers;

    public static void main(String[] args) {
        try {

            System.out.println("Listening for connections...");
            /* ficar a escuta de conexoes */
            while(true){
            /* ficar a escuta de pedidos tcp (http get) */
                byte[] data = "ola".getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, , udp_port);
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }


}

