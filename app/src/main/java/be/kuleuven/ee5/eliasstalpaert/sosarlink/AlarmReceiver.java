package be.kuleuven.ee5.eliasstalpaert.sosarlink;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";
    public static final int REQUEST_CODE = 12345;
    public static final String ACTION = "be.kuleuven.ee5.eliasstalpaert.sosarlink.alarm";

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d("AlarmReceiver", "Alarm received");
        new Thread(new Runnable() {
            @Override
            public void run() {
                new AlarmReceiver.ConnectTask(context).execute("");
                Log.d(TAG, "ConnectTask started");
            }
        }).start();
    }

    protected class ConnectTask extends AsyncTask<String, String, TcpClient> {

        Context mContext;
        NotificationManager NM;
        NotificationChannel mChannel;
        int number;
        String name, time, date;
        int notifyID;
        boolean captureReceived;

        ConnectTask(Context mContext) {
            super();
            this.mContext = mContext;
            this.notifyID = 0;
            this.captureReceived = false;

            NM = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String CHANNEL_ID = "my_channel_01";// The id of the channel.
                CharSequence name = "nChannelSosarlink";// The user-visible name of the channel.
                int importance = NotificationManager.IMPORTANCE_HIGH;
                this.mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
                NM.createNotificationChannel(mChannel);
            }

        }

        @Override
        protected void onPostExecute(TcpClient tcpClient) {
            super.onPostExecute(tcpClient);
            Log.d(TAG, "Alarm finished");

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

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            //process server response here....
            if (values[0].contains("yes")) {
                number = 0;
            } else if (!values[0].contains("no")) {
                if (number == 0) {
                    name = new String(values[0]);
                    number++;
                } else if (number == 1) {
                    time = new String(values[0]);
                    number++;
                } else if (number == 2) {
                    date = new String(values[0]);
                    number++;
                }
            } else if(values[0].contains("no")) {
                if(captureReceived) {
                    Intent new_satellite = new Intent("SATELLITE");
                    mContext.sendBroadcast(new_satellite);
                    captureReceived = false;
                }
                number = 0;
            }
            if (number >= 3) {
                String pref_message = name + time + date;
                updateSharedPreferences(pref_message); //write to shared application preferences
                switch (name) {
                    case "NOAA15":
                        name = "NOAA-15";
                        break;
                    case "NOAA18":
                        name = "NOAA-18";
                        break;
                    case "NOAA19":
                        name = "NOAA-19";
                        break;
                    case "METEOR":
                        name = "METEOR-M N2";
                        break;
                    default:
                        name = "ERROR";
                }

                Log.d("ConnectTask","New capture");
                String nMessage = name + " at " + time.substring(0, 2) + ":" + time.substring(2, 4) + ":" + time.substring(4, 6)
                        + " on " + date.substring(0, 2) + "/" + date.substring(2, 4) + "/" + date.substring(4, 6);
                pushNotification(nMessage); //push notifications to user
                captureReceived = true;
                number = 0;
            }

        }

        public void pushNotification(String nMessage) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification n = new Notification.Builder(mContext, mChannel.getId())
                        .setContentTitle("New weather satellite picture!")
                        .setContentText(nMessage)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setChannelId(mChannel.getId())
                        .build();
                NM.notify(notifyID, n);
            } else {
                Notification n = new Notification.Builder(mContext)
                        .setContentTitle("New weather satellite picture!")
                        .setContentText(nMessage)
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .build();
                NM.notify(notifyID, n);
            }
            notifyID++;
        }

        public void updateSharedPreferences(String pref_message) {
            ArrayList<String> stringList = MainActivity.getArrayList(MainActivity.LIST_NAME,mContext);
            if (stringList == null) {
                stringList = new ArrayList<>();
            }
            Iterator<String> iterator = stringList.iterator();
            boolean found = false;
            while(iterator.hasNext() && !found) {
                String next = iterator.next();
                if(next.equals(pref_message)) {
                    found = true;
                }
            }
            if(!found) {
                stringList.add(pref_message);
                Log.d(TAG, "New string added to sharedpreferences");
            }
            MainActivity.saveArrayList(stringList,MainActivity.LIST_NAME,mContext);
        }
    }
}
