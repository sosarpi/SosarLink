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

    //onStopJob is called when PollingJob failes/gets interrupted.
    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "PollingJob cancelled before completion");
        jobFinished(params, false);
        return true;
    }

    //Defines the work we want done by the Job
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



    //Schedules the next job with appropriate latency according to the set polling interval (can't do this using JobScheduler because the defined minimum is 15 minutes)
    private void scheduleRefresh() {
        JobScheduler jobScheduler = (JobScheduler) getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);

        JobInfo.Builder jobBuilder =
                new JobInfo.Builder(CapturesFragment.POLLINGJOB_ID,
                        new ComponentName(this,
                                PollingJob.class));

        //Passing variables to the next job
        PersistableBundle extras = new PersistableBundle();
        extras.putString(SettingsFragment.IP_KEY, this.serverIp);
        extras.putInt(CapturesFragment.POLLING_INTERVAL_KEY, this.pollingInterval);

        jobBuilder
                .setMinimumLatency(pollingInterval * 60 * 1000) //defines the delay before the job is executed
                .setPersisted(true)
                .setExtras(extras)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);

        int resultCode = jobScheduler.schedule(jobBuilder.build());
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
        boolean captureReceived;

        private String serverIp;

        ConnectTask(Context context, String serverIp) {
            super();
            this.context = context;
            this.serverIp = serverIp;
            this.notificationID = 0;
            this.captureReceived = false;
        }

        @Override
        protected TcpClient doInBackground(String... message) {
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            // Android Oreo and higher requires a notification channel to push the notifications to
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String CHANNEL_ID = BuildConfig.APPLICATION_ID + ".notificationchannel";// The id of the channel.
                CharSequence name = "Picture Notification Channel";// The user-visible satelliteName of the channel.
                int importance = NotificationManager.IMPORTANCE_HIGH;
                this.notificationChannel = new NotificationChannel(CHANNEL_ID, name, importance);
                notificationManager.createNotificationChannel(notificationChannel);
            }

            // Creates an object of TCPClient along with a listener which is passed to the TCPClient
            TcpClient mTcpClient = new TcpClient(new TcpClient.OnMessageReceived() {
                @Override
                public void messageReceived(String message) {
                    // This method calls the onProgressUpdate of the ConnectTask
                    publishProgress(message);
                }
            }, this.serverIp);

            mTcpClient.run();

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            /*
            * Meaning of the values:
            * 'yes': means there are captures present in the log file in the server and will be sent next
            * 'no': means there are no captures available (anymore)
             */
            if (values[0].contains("yes")) {
                numberOfReceivedParts = 0;
            }
            else if (values[0].contains("no")) {
                // If 1 or more captures were received, the BroadcastReceiver of the PassesFragment is informed (which then updates the RecyclerView)
                if (captureReceived) {
                    Intent newCaptureBroadcast = new Intent("SATELLITE");
                    context.sendBroadcast(newCaptureBroadcast);
                    captureReceived = false;
                }
                numberOfReceivedParts = 0;
            }
            else {
                // Every capture entry exists of 3 parts
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
                // Checks if the capture is received in the right format (should consist of 18 characters)
                if (prefMessage.length() == 18) {
                    updateSharedPreferences(prefMessage);
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
                    pushNotification(nMessage);
                    captureReceived = true;
                }
                numberOfReceivedParts = 0;
            }

        }

        private void pushNotification(String notificationMessage) {
            // Add PendingIntent to notification so the MainActivity can be launched by touching the notification
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            Notification.Builder notificationBuilder = new Notification.Builder(context)
                    .setContentTitle("New weather satellite picture!")
                    .setContentText(notificationMessage)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true) // Notifioation should cancel when it has been touched
                    .setSmallIcon(R.drawable.ic_satellite);

            // Again due to the changes of Android Oreo and later, we have to use a notification channel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationBuilder.setChannelId(notificationChannel.getId());
                notificationManager.notify(notificationID, notificationBuilder.build());
            } else {
                notificationManager.notify(notificationID, notificationBuilder.build());
            }
            notificationID++;
        }

        private void updateSharedPreferences(String prefMessage) {
            // Adds the satellite capture to the StringList of the application's SharedPreferences
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