package be.kuleuven.ee5.eliasstalpaert.sosarlink;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class TcpClient {

    private static final String TAG = TcpClient.class.getSimpleName();
    private static final int SERVER_PORT = 5678;
    private String serverMessage;
    private OnMessageReceived messageListener;
    private boolean run = false;
    // Used to send messages
    private PrintWriter bufferOut;
    // Used to read messages from the server
    private BufferedReader bufferIn;

    private String serverIp;

    // The messageListener listens for messages received from the server
    public TcpClient(OnMessageReceived listener, String ip) {
        this.serverIp = ip;
        this.messageListener = listener;
    }

    // Close connection and release its member variables
    private void stopClient() {

        run = false;

        if (bufferOut != null) {
            bufferOut.flush();
            bufferOut.close();
        }

        messageListener = null;
        bufferIn = null;
        bufferOut = null;
        serverMessage = null;
    }

    public void run() {

        run = true;
        Socket socket = new Socket();

        try {
            // Gets the server address
            InetAddress serverAddr = InetAddress.getByName(this.serverIp);

            Log.d(TAG, "C: Connecting...");

            // Create socket for the TCP connection with the server
            InetSocketAddress address = new InetSocketAddress(serverAddr, SERVER_PORT);
            // Timeout of 5 seconds
            int timeout = 5000;
            socket.setSoTimeout(timeout);
            socket.connect(address, timeout);

            if (socket.isConnected()) {
                try {

                    // Sends the message to the server
                    bufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                    // Receives messages from, the server
                    bufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));


                    // In this while-loop we listen for messages from the server as long as the server is running
                    while (run) {

                        if (bufferIn.ready()) {
                            serverMessage = bufferIn.readLine();
                            serverMessage = bufferIn.readLine();
                        }

                        if (serverMessage != null && messageListener != null) {
                            // Calls the messageReceived method of the listener assigned in the TCPClient constructor
                            messageListener.messageReceived(serverMessage);
                            Log.d(TAG, "S: Received Message: '" + serverMessage + "'");
                            // When a 'no' is received, there won't be anymore messages coming from the server, so the client can be closed
                            if (serverMessage.contains("no")) {
                                stopClient();
                                Log.d(TAG, "C: Socket Closed");
                            }
                        }

                    }
                } catch (SocketTimeoutException e) {
                    Log.e("TCP", "S: socket timed out");
                    stopClient();
                } finally {
                    // The socket must be closed. It is not possible to reconnect to this socket
                    // after it is closed, which means a new socket instance has to be created.
                    socket.close();
                    Log.e("TCP", "S: socket closed");
                }
            } else {
                stopClient();
                socket.close();
                Log.e("TCP", "S: no connection");
                Log.e("TCP", "S: socket closed");
            }
        } catch (SocketTimeoutException e) {
            stopClient();
            try {
                socket.close();
            } catch (Exception socket_e) {
                Log.e("TCP", "C: Error", socket_e);
            }
            Log.e("TCP", "S: no connection");
            Log.e("TCP", "S: socket closed");
        } catch (Exception e) {
            Log.e("TCP", "C: Error", e);
        }
    }

    //Declares the interface for the listener which has to be defined by the class instantiating the TCPClient (see constructor)
    public interface OnMessageReceived {
        void messageReceived(String message);
    }

}
