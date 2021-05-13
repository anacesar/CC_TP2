package Common;

import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;

import Gateway.HttpGW;
import jdk.javadoc.internal.doclets.formats.html.SourceToHTMLConverter;

/* vai ficar a espera de conexoes pelo socket tcp */
public class TCPListener implements Runnable {
    private HttpGW gateway;
    private int my_port;
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
                System.out.println("New connection from " + clientSocket.getInetAddress());

                InputStream inputStream = clientSocket.getInputStream();
                byte[] data = new byte[1448];

                /* While the customer sends info keep sending packets */
                int count = inputStream.read(data);

                System.out.println(Arrays.toString(data));
                // ClientConnection clientConnection = new ClientConnection(clientSocket);
                // Connection clientConnection = new Connection(tcp_port);

                /* thread to deal with client request */
                /*
                new Thread(() -> {
                    /* parse request 

                }).start();
                */
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