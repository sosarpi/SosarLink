package be.kuleuven.ee5.eliasstalpaert.sosarlink;

import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    Button ftpButton, tcpButton, messageButton, tcpCloseButton;
    TcpClient mTcpClient;
    TextView receivedText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ftpButton = findViewById(R.id.connectFtpButton);
        tcpButton = findViewById(R.id.tcpButton);
        messageButton = findViewById(R.id.tcpMessageButton);
        tcpCloseButton = findViewById(R.id.tcpCloseButton);
        receivedText = findViewById(R.id.receivedText);
        ftpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectToFtp();
            }
        });
        tcpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectTcp();
            }
        });
        messageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                messageTcp();
            }
        });
        tcpCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeTcp();
            }
        });
    }

    public void connectToFtp() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();

        String ftpHost = formatIP(dhcpInfo.gateway);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("ftp://" + ftpHost));
        intent.putExtra("ftp_username","pi");
        intent.putExtra("ftp_password","sosar");
        startActivity(intent);
    }

    public void connectTcp() {
        new ConnectTask().execute("");
    }

    public void messageTcp() {

        if (mTcpClient != null) {
            char message = '1';
            mTcpClient.sendMessage(message);
        }

    }

    public void closeTcp() {
        if (mTcpClient != null) {
            mTcpClient.stopClient();
        }
    }

    private String formatIP(int ip) {
        return String.format(
                "%d.%d.%d.%d",
                (ip & 0xff),
                (ip >> 8 & 0xff),
                (ip >> 16 & 0xff),
                (ip >> 24 & 0xff)
        );
    }

    class ConnectTask extends AsyncTask<String, String, TcpClient> {

        @Override
        protected TcpClient doInBackground(String... message) {

            //we create a TCPClient object
            mTcpClient = new TcpClient(new TcpClient.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                    //this method calls the onProgressUpdate
                    publishProgress(message);
                }
            }, receivedText);
            mTcpClient.run();

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            //response received from server
            Log.d("test", "response " + values[0]);
            //process server response here....

        }
    }
}

