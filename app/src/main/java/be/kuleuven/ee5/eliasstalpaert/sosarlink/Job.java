package be.kuleuven.ee5.eliasstalpaert.sosarlink;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

public class Job extends JobService {
    private static final String TAG = "Job";
    //JobCancelled boolean nodig.
    // !!! Zelf verantwoordelijk voor beëindigen van de voltooide background jobs!!!
    private boolean jobCancelled = false;

    //onStartJob called when job launched
    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "Job started");
        doBackGroundWork(params);

        return true;
    }

    //Here our actual job work is done
    private void doBackGroundWork(final JobParameters params) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // IMPLEMENT OUR STUFF HERE!!
                // Momenteel dummy job die 10x om seconde msg print
                new ConnectTask(getApplicationContext()).execute("");
                /*
                for (int i = 0; i < 10; i++) {
                    Log.d(TAG, "run: " + i);
                    //BELANGRIJK: ZELF JOB TERMINATEN INDIEN VOLTOOID. MBV JobCancelled BOOLEAN.
                    if (jobCancelled) {
                        return;
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                */
                Log.d(TAG, "Job finished");
                jobFinished(params, false);
            }
        }).start();
    }

    //onStopJob is called when Job failes/gets interrupted.
    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "Job cancelled before completion");
        jobCancelled = true;
        return true;
    }
}
