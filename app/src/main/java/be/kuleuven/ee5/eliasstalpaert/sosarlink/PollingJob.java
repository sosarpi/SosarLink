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

public class PollingJob extends JobService {
    private static final String TAG = PollingJob.class.getSimpleName();
    private static final int DEFAULT_INTERVAL = 5;

    private String serverIp;
    private int pollingInterval;

    //onStartJob called when job launched
    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "PollingJob started");

        PersistableBundle extras = params.getExtras();
        this.pollingInterval = extras.getInt(CapturesFragment.POLLING_INTERVAL_KEY, PollingJob.DEFAULT_INTERVAL);
        this.serverIp = params.getExtras().getString(SettingsFragment.IP_KEY);

        scheduleRefresh();
        doBackGroundWork(params);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "PollingJob cancelled before completion");
        jobFinished(params, false);
        return true;
    }

    //Here our actual job work is done
    private void doBackGroundWork(final JobParameters params) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                new ConnectTask(getApplicationContext(), serverIp).execute("");
                Log.d(TAG, "ConnectTask started");
            }
        }).start();
        jobFinished(params, false);
    }


    //onStopJob is called when PollingJob failes/gets interrupted.

    private void scheduleRefresh() {
        JobScheduler mJobScheduler = (JobScheduler) getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);

        JobInfo.Builder mJobBuilder =
                new JobInfo.Builder(CapturesFragment.POLLINGJOB_ID,
                        new ComponentName(this,
                                PollingJob.class));

        PersistableBundle extras = new PersistableBundle();
        extras.putString(SettingsFragment.IP_KEY, this.serverIp);
        extras.putInt(CapturesFragment.POLLING_INTERVAL_KEY, this.pollingInterval);

        mJobBuilder
                .setMinimumLatency(pollingInterval * 60 * 1000) //tijdsinterval
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

    protected static class ConnectTask extends AsyncTask<String, String, TcpClient> {

        private Context context;
        private NotificationManager notificationManager;
        private NotificationChannel notificationChannel;
        private int numberOfReceivedParts;
        private String satelliteName, satelliteTime, satelliteDate;
        int notificationID;
        boolean fullCaptureReceived;

        private String serverIp;

        ConnectTask(Context context, String serverIp) {
            super();
            this.context = context;
            this.serverIp = serverIp;
            this.notificationID = 0;
            this.fullCaptureReceived = false;
        }

        @Override
        protected TcpClient doInBackground(String... message) {
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String CHANNEL_ID = BuildConfig.APPLICATION_ID + ".notificationchannel";// The id of the channel.
                CharSequence name = "Picture Notification Channel";// The user-visible satelliteName of the channel.
                int importance = NotificationManager.IMPORTANCE_HIGH;
                this.notificationChannel = new NotificationChannel(CHANNEL_ID, name, importance);
                notificationManager.createNotificationChannel(notificationChannel);
            }

            //we create a TCPClient object
            TcpClient mTcpClient = new TcpClient(new TcpClient.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                    //this method calls the onProgressUpdate
                    publishProgress(message);
                }
            }, this.serverIp);

            mTcpClient.run();

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            //process server response here....
            if (values[0].contains("yes")) {
                numberOfReceivedParts = 0;
            }
            else if (values[0].contains("no")) {
                if (fullCaptureReceived) {
                    Intent newCaptureBroadcast = new Intent("SATELLITE");
                    context.sendBroadcast(newCaptureBroadcast);
                    fullCaptureReceived = false;
                }
                numberOfReceivedParts = 0;
            }
            else {
                if (numberOfReceivedParts == 0) {
                    satelliteName = values[0];
                    numberOfReceivedParts++;
                } else if (numberOfReceivedParts == 1) {
                    satelliteTime = values[0];
                    numberOfReceivedParts++;
                } else if (numberOfReceivedParts == 2) {
                    satelliteDate = values[0];
                    numberOfReceivedParts++;
                }
            }

            if (numberOfReceivedParts >= 3) {
                String prefMessage = satelliteName + satelliteTime + satelliteDate;
                if (prefMessage.length() == 18) { //correcte format?
                    updateSharedPreferences(prefMessage); //write to shared application preferences
                    switch (satelliteName) {
                        case "NOAA15":
                            satelliteName = "NOAA-15";
                            break;
                        case "NOAA18":
                            satelliteName = "NOAA-18";
                            break;
                        case "NOAA19":
                            satelliteName = "NOAA-19";
                            break;
                        case "METEOR":
                            satelliteName = "METEOR-M N2";
                            break;
                        default:
                            satelliteName = "ERROR";
                    }

                    String nMessage = satelliteName + " at " + satelliteTime.substring(0, 2) + ":" + satelliteTime.substring(2, 4) + ":" + satelliteTime.substring(4, 6)
                            + " on " + satelliteDate.substring(0, 2) + "/" + satelliteDate.substring(2, 4) + "/" + satelliteDate.substring(4, 6);
                    pushNotification(nMessage); //push notifications to user
                    fullCaptureReceived = true;
                }
                numberOfReceivedParts = 0;
            }

        }

        private void pushNotification(String notificationMessage) {
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            Notification.Builder notificationBuilder = new Notification.Builder(context)
                    .setContentTitle("New weather satellite picture!")
                    .setContentText(notificationMessage)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.ic_satellite);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationBuilder.setChannelId(notificationChannel.getId());
                notificationManager.notify(notificationID, notificationBuilder.build());
            } else {
                notificationManager.notify(notificationID, notificationBuilder.build());
            }
            notificationID++;
        }

        private void updateSharedPreferences(String prefMessage) {
            ArrayList<String> stringList = MainActivity.getArrayList(MainActivity.LIST_NAME, context);
            if (stringList == null) {
                stringList = new ArrayList<>();
            }
            boolean found = false;
            for (String next : stringList) {
                if (next.equals(prefMessage)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                stringList.add(prefMessage);
                Log.d(TAG, prefMessage + " added to sharedpreferences");
            }
            MainActivity.saveArrayList(stringList, MainActivity.LIST_NAME, context);
        }
    }
}