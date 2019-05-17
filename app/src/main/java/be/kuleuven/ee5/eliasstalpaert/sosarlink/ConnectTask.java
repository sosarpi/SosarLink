package be.kuleuven.ee5.eliasstalpaert.sosarlink;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.TextView;

public class ConnectTask extends AsyncTask<String, String, TcpClient> {

    //TextView receivedText;

    Context mContext;
    NotificationManager NM;
    NotificationChannel mChannel;
    int number;
    String name, time, date;

    ConnectTask(Context mContext) {
        super();
        //this.receivedText = receivedText;
        this.mContext = mContext;

        NM=(NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "my_channel_01";// The id of the channel.
            CharSequence name = "yesbaby";// The user-visible name of the channel.
            int importance = NotificationManager.IMPORTANCE_HIGH;
            this.mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            NM.createNotificationChannel(mChannel);
        }

    }

    @Override
    protected TcpClient doInBackground(String... message) {

        //we create a TCPClient object
        TcpClient mTcpClient = new TcpClient(new TcpClient.OnMessageReceived() {
            @Override
            //here the messageReceived method is implemented
            public void messageReceived(String message) {
                //this method calls the onProgressUpdate
                publishProgress(message);
            }
        });

        mTcpClient.run();

        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        //response received from server
        Log.d("test", "response " + values[0]);
        //process server response here....
        if(values[0].contains("yes")) {
            number = 0;
        }
        else if(!values[0].contains("no")){
            if(number == 0) {
                name = new String(values[0]);
                number++;
            }
            else if(number == 1) {
                time = new String(values[0]);
                number++;
            }
            else if(number == 2) {
                date = new String(values[0]);
                number++;
            }
        }
        else{
            number = 0;
        }
        if(number >= 3) {
            String message = name + "\t" + time + "\t" + date;
            pushNotification(message);
        }

    }

    public void pushNotification(String nMessage) {
        int notifyID = 1;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification n = new Notification.Builder(mContext, mChannel.getId())
                    .setContentTitle("SosarLink")
                    .setContentText(nMessage)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setChannelId(mChannel.getId())
                    .build();
            NM.notify(notifyID,n);
        }
        else{
            Notification n = new Notification.Builder(mContext)
                    .setContentTitle("SosarLink")
                    .setContentText(nMessage)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .build();
            NM.notify(notifyID, n);
        }
        Log.d("test", "notified " + nMessage);
    }
}