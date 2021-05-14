package FastFileServer;

import java.net.InetAddress;
import java.net.UnknownHostException;

import Common.UDPListener;

public class FastFileServer {
    private int my_port;
    private InetAddress my_address;


    /* arg[0] -> IP do server  */
    public static void main(String[] args) throws UnknownHostException {
        /* get server address and port */
        InetAddress address = InetAddress.getByName(args[0]);
        int port = Integer.parseInt(args[1]);


        /* udp listener to be able to look for conections */
        /* pito a pito enche o rito o bico */
    }
}
