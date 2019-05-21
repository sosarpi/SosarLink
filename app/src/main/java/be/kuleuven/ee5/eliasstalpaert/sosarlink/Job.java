package be.kuleuven.ee5.eliasstalpaert.sosarlink;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

import javax.xml.transform.Result;

public class Job extends JobService {
    private static final String TAG = "Job";

    JobParameters mParams;
    SharedPreferences.Editor editor;
    SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor= sharedPreferences.edit();
    }

    //onStartJob called when job launched
    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "Job started");
        this.mParams = params;
        doBackGroundWork(params);

        return true;
    }

    //Here our actual job work is done
    private void doBackGroundWork(final JobParameters params) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                new ConnectTask(getApplicationContext()).execute("");
                Log.d(TAG, "AsyncTask started");
            }
        }).start();
    }

    //onStopJob is called when Job failes/gets interrupted.
    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "Job cancelled before completion");
        jobFinished(params, false);
        //jobCancelled = true;
        return true;
    }

    protected class ConnectTask extends AsyncTask<String, String, TcpClient> {

        Context mContext;
        NotificationManager NM;
        NotificationChannel mChannel;
        int number;
        String name, time, date;
        int notifyID;

        ConnectTask(Context mContext) {
            super();
            this.mContext = mContext;
            this.notifyID = 0;

            NM=(NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

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
            if(mParams != null) {
                Log.d(TAG, "Job finished");
                jobFinished(mParams, false);
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
                String pref_message = name + time + date;
                updateSharedPreferences(pref_message); //write to shared application preferences

                switch(name) {
                    case "NOAA19":
                        name = "NOAA-19";
                        break;
                    case "NOAA18":
                        name = "NOAA-18";
                        break;
                    case "METEOR":
                        name = "METEOR-M N2";
                }

                String nMessage = name + " at " + time.substring(0,2) + ":" + time.substring(2,4) + ":" + time.substring(4,6)
                        + " on " + date.substring(0,2) + "/" + date.substring(2,4) + "/" + date.substring(4,6);
                pushNotification(nMessage); //push notifications to user
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
                NM.notify(notifyID,n);
            }
            else{
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
            if(editor != null) {
                Set<String> stringSet = sharedPreferences.getStringSet("passes", null);
                if(stringSet == null) {
                    stringSet = new HashSet<>();
                }
                stringSet.add(pref_message);
                editor.putStringSet("passes", stringSet);
                editor.apply();
                Log.d(TAG, "New string added to sharedpreferences");
            }
            else {
                Log.d(TAG, "Error: Sharedpreferences editor is null");
            }
        }
    }
}
