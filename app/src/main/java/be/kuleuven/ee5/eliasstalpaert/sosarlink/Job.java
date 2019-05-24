package be.kuleuven.ee5.eliasstalpaert.sosarlink;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PersistableBundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;

public class Job extends JobService {
    private static final String TAG = Job.class.getSimpleName();
    private static final int INTERVAL = 5;

    private JobParameters mParams;
    private String server_ip;

    //onStartJob called when job launched
    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "Job started");
        this.mParams = params;
        this.server_ip = params.getExtras().getString(FtpFragment.FTPIP_KEY);
        scheduleRefresh();
        doBackGroundWork(params);

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "Job cancelled before completion");
        jobFinished(params, false);
        return true;
    }

    //Here our actual job work is done
    private void doBackGroundWork(final JobParameters params) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                new ConnectTask(getApplicationContext(), server_ip).execute("");
                Log.d(TAG, "ConnectTask started");
            }
        }).start();
    }

    //onStopJob is called when Job failes/gets interrupted.

    private void scheduleRefresh() {
        JobScheduler mJobScheduler = (JobScheduler) getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        JobInfo.Builder mJobBuilder =
                new JobInfo.Builder(1,
                        new ComponentName(this,
                                Job.class));
        PersistableBundle extras = new PersistableBundle();
        extras.putString(FtpFragment.FTPIP_KEY, this.server_ip);

        mJobBuilder
                .setMinimumLatency(INTERVAL * 60 * 1000) //tijdsinterval
                .setPersisted(true)
                .setExtras(extras)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);

        int resultCode = mJobScheduler.schedule(mJobBuilder.build());
        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Next job successfully scheduled");
        } else {
            Log.d(TAG, "Failed to schedule next job");
        }
    }

    private class ConnectTask extends AsyncTask<String, String, TcpClient> {

        private Context mContext;
        private NotificationManager mNotificationManager;
        private NotificationChannel mNotificationChannel;
        private int numberOfReceivedParts;
        private String satellite_name, satellite_time, satellite_date;
        int notificationID;
        boolean fullCaptureReceived;

        private String server_ip;

        ConnectTask(Context mContext, String server_ip) {
            super();
            this.mContext = mContext;
            this.server_ip = server_ip;
            this.notificationID = 0;
            this.fullCaptureReceived = false;

            mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String CHANNEL_ID = BuildConfig.APPLICATION_ID + ".notificationchannel";// The id of the channel.
                CharSequence name = "Picture Notification Channel";// The user-visible satellite_name of the channel.
                int importance = NotificationManager.IMPORTANCE_HIGH;
                this.mNotificationChannel = new NotificationChannel(CHANNEL_ID, name, importance);
                mNotificationManager.createNotificationChannel(mNotificationChannel);
            }

        }

        @Override
        protected void onPostExecute(TcpClient tcpClient) {
            super.onPostExecute(tcpClient);
            if (mParams != null) {
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
            }, this.server_ip,getApplicationContext());

            mTcpClient.run();

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            //process server response here....
            if (values[0].contains("yes")) {
                numberOfReceivedParts = 0;
            } else if (!values[0].contains("no")) {
                if (numberOfReceivedParts == 0) {
                    satellite_name = new String(values[0]);
                    numberOfReceivedParts++;
                } else if (numberOfReceivedParts == 1) {
                    satellite_time = new String(values[0]);
                    numberOfReceivedParts++;
                } else if (numberOfReceivedParts == 2) {
                    satellite_date = new String(values[0]);
                    numberOfReceivedParts++;
                }
            } else if (values[0].contains("no")) {
                if (fullCaptureReceived) {
                    Intent new_satellite = new Intent("SATELLITE");
                    sendBroadcast(new_satellite);
                    fullCaptureReceived = false;
                }
                numberOfReceivedParts = 0;
            }
            if (numberOfReceivedParts >= 3) {
                String pref_message = satellite_name + satellite_time + satellite_date;
                if (pref_message.length() >= 18) { //correcte format?
                    updateSharedPreferences(pref_message); //write to shared application preferences
                    switch (satellite_name) {
                        case "NOAA15":
                            satellite_name = "NOAA-15";
                            break;
                        case "NOAA18":
                            satellite_name = "NOAA-18";
                            break;
                        case "NOAA19":
                            satellite_name = "NOAA-19";
                            break;
                        case "METEOR":
                            satellite_name = "METEOR-M N2";
                            break;
                        default:
                            satellite_name = "ERROR";
                    }

                    String nMessage = satellite_name + " at " + satellite_time.substring(0, 2) + ":" + satellite_time.substring(2, 4) + ":" + satellite_time.substring(4, 6)
                            + " on " + satellite_date.substring(0, 2) + "/" + satellite_date.substring(2, 4) + "/" + satellite_date.substring(4, 6);
                    pushNotification(nMessage); //push notifications to user
                    fullCaptureReceived = true;
                }
                numberOfReceivedParts = 0;
            }

        }

        private void pushNotification(String nMessage) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

            Notification.Builder notificationBuilder = new Notification.Builder(mContext)
                    .setContentTitle("New weather satellite picture!")
                    .setContentText(nMessage)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setSmallIcon(R.mipmap.ic_sosarlogo);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationBuilder.setChannelId(mNotificationChannel.getId());
                mNotificationManager.notify(notificationID, notificationBuilder.build());
            } else {
                mNotificationManager.notify(notificationID, notificationBuilder.build());
            }
            notificationID++;
        }

        private void updateSharedPreferences(String pref_message) {
            ArrayList<String> stringList = MainActivity.getArrayList(MainActivity.LIST_NAME, getApplicationContext());
            if (stringList == null) {
                stringList = new ArrayList<>();
            }
            Iterator<String> iterator = stringList.iterator();
            boolean found = false;
            while (iterator.hasNext() && !found) {
                String next = iterator.next();
                if (next.equals(pref_message)) {
                    found = true;
                }
            }
            if (!found) {
                stringList.add(pref_message);
                Log.d(TAG, "New string added to sharedpreferences");
            }
            MainActivity.saveArrayList(stringList, MainActivity.LIST_NAME, getApplicationContext());
        }
    }
}