package Common;

import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;

import Gateway.HttpGW;

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

                BufferedReader breader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String httprequest = breader.readLine();
                
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