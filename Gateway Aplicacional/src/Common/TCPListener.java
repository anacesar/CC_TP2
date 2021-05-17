package Common;

import java.io.InputStream;
import java.lang.String;
import java.net.DatagramSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.SocketException;

import static java.lang.System.out;
import Gateway.HttpGW;

/* vai ficar a espera de conexoes pelo socket tcp */
public class TCPListener implements Runnable {
    private HttpGW gateway;
    private int my_port; //8080
    private ServerSocket serverSocket;
    

    public TCPListener(HttpGW gateway, int my_port) {
        this.gateway = gateway;
        this.my_port = my_port;
    }

    public void run() {
        try { /* TCP Listener */
            serverSocket = new ServerSocket(my_port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                out.println("New connection from " + clientSocket.getInetAddress());

                BufferedReader breader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String httprequest = breader.readLine();
                
                out.println("Comando Recebido: " + httprequest);


                /* thread to deal with client request */

                new Thread(() -> {
                     String[] sps= httprequest.split(" ");
                     String[] sp= sps[1].split("/");
                     String filename = sp[1];

                     out.println(filename);

                }).start();


            }

        }catch(IOException e) {
            e.printStackTrace();
        }finally{
            try {
                serverSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}