package be.kuleuven.ee5.eliasstalpaert.sosarlink;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    Button ftpButton, tcpButton, messageButton, tcpCloseButton;
    //TcpClient mTcpClient;
    TextView receivedText;

    //NotificationManager NM;
    //NotificationChannel mChannel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Setup Views
        setupViews();
        //Schedule Jobs
        scheduleJobs();
        /*
        this.NM=(NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "my_channel_01";// The id of the channel.
            CharSequence name = "yesbaby";// The user-visible name of the channel.
            int importance = NotificationManager.IMPORTANCE_HIGH;
            this.mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            NM.createNotificationChannel(this.mChannel);
        }
        */
    }

    public void setupViews() {
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
        /*
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
        */
    }

    public void scheduleJobs() {
        //Klasse componentName noodzakelijk voor creëren JobInfo object.
        ComponentName componentName = new ComponentName(this, Job.class);
        //JobInfo.Builder maakt mogelijk alle instellingen van de jobs die geschudeld worden in te stellen.
        //Zie https://developer.android.com/reference/android/app/job/JobInfo.Builder.html voor opties.
        JobInfo info = new JobInfo.Builder(1, componentName)
                .setPersisted(true)            //Na reboot zal job nog altijd onthouden worden.
                .setPeriodic(1 * 60 * 1000)    //Job iedere 30 minuten. Minimum mogelijk in te stellen is 15 minuten.
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .build();

        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        int resultCode = scheduler.schedule(info);
        //Log message printen wanneer job geslaagd of niet.
        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Periodically checking scheduled");
        } else {
            Log.d(TAG, "Scheduling periodically checking failed");
        }
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

    private String formatIP(int ip) {
        return String.format(
                "%d.%d.%d.%d",
                (ip & 0xff),
                (ip >> 8 & 0xff),
                (ip >> 16 & 0xff),
                (ip >> 24 & 0xff)
        );
    }

    public void connectTcp() {
        new ConnectTask(getApplicationContext()).execute("");
        /*
        int notifyID = 1;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification n =
                    new NotificationCompat.Builder(this, mChannel.getId())
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setContentTitle("My notification")
                            .setContentText("Hello World!").build();

            NM.notify(notifyID,n);
        }
        else{
            Notification n = new Notification.Builder(getApplicationContext())
                    .setContentTitle("SosarLink")
                    .setContentText("yesbaby")
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .build();
            NM.notify(notifyID, n);
        }
        */
    }
/*
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
    */
}

