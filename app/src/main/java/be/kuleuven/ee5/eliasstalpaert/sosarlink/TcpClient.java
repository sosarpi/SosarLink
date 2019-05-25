package be.kuleuven.ee5.eliasstalpaert.sosarlink;

import android.content.Context;
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
    // message to send to the server
    private String serverMessage;
    // sends message received notifications
    private OnMessageReceived messageListener;
    // while this is true, the server will continue running
    private boolean run = false;
    // used to send messages
    private PrintWriter bufferOut;
    // used to read messages from the server
    private BufferedReader bufferIn;

    private String serverIp;

    /**
     * Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TcpClient(OnMessageReceived listener, String ip) {
        this.serverIp = ip;
        this.messageListener = listener;
    }

    /**
     * Close the connection and release the members
     */
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

    public void run(){

        run = true;
        Socket socket = new Socket();

        try {
            //here you must put your computer's IP address.
            InetAddress serverAddr = InetAddress.getByName(this.serverIp);

            Log.d(TAG, "C: Connecting...");

            //create a socket to make the connection with the server
            InetSocketAddress address = new InetSocketAddress(serverAddr, SERVER_PORT);
            int timeout = 5000;
            socket.setSoTimeout(timeout);
            socket.connect(address, timeout);

            if(socket.isConnected()){
                try {

                    //sends the message to the server
                    bufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                    //receives the message which the server sends back
                    bufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));


                    //in this while the client listens for the messages sent by the server
                    while (run) {

                        if(bufferIn.ready()){
                            serverMessage = bufferIn.readLine();
                            serverMessage = bufferIn.readLine();
                        }

                        if (serverMessage != null && messageListener != null) {
                            //call the method messageReceived from MyActivity class
                            messageListener.messageReceived(serverMessage);
                            Log.d(TAG, "S: Received Message: '" + serverMessage + "'");
                            if(serverMessage.contains("no")) {
                                stopClient();
                                Log.d(TAG, "C: Socket Closed");
                            }
                        }

                    }
                }
                catch (SocketTimeoutException e) {
                    Log.e("TCP", "S: socket timed out");
                    stopClient();
                }
                finally {
                    //the socket must be closed. It is not possible to reconnect to this socket
                    // after it is closed, which means a new socket instance has to be created.
                    socket.close();
                    Log.e("TCP", "S: socket closed");
                }
            }
            else {
                stopClient();
                socket.close();
                Log.e("TCP", "S: no connection");
                Log.e("TCP", "S: socket closed");
            }
        } catch (SocketTimeoutException e) {
            stopClient();
            try {
                socket.close();
            }
            catch(Exception socket_e) {
                Log.e("TCP", "C: Error", socket_e);
            }
            Log.e("TCP", "S: no connection");
            Log.e("TCP", "S: socket closed");
        }
        catch(Exception e) {
            Log.e("TCP", "C: Error", e);
        }
    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the Activity
    //class at on AsyncTask doInBackground
    public interface OnMessageReceived {
        void messageReceived(String message);
    }

}
