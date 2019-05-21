package be.kuleuven.ee5.eliasstalpaert.sosarlink;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Iterator;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";

    private DrawerLayout drawer;
    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                new PassesFragment()).commit();
        navigationView.setCheckedItem(R.id.nav_passes);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor=sharedPreferences.edit();
        editor.clear();
        editor.apply();

        scheduleJobs(); //Schedule Jobs

    }

    public void scheduleJobs() {
        ComponentName componentName = new ComponentName(this, Job.class);
        JobInfo info = new JobInfo.Builder(1, componentName)
                .setPersisted(true)            //Na reboot zal job nog altijd onthouden worden.
                .setPeriodic(1 * 60 * 1000)    //Job iedere 30 minuten. Minimum mogelijk in te stellen is 15 minuten.
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .build();

        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        int resultCode = scheduler.schedule(info);
        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Periodically checking scheduled");
        } else {
            Log.d(TAG, "Scheduling periodically checking failed");
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.nav_ftp:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        new FtpFragment()).commit();
                break;
            case R.id.nav_passes:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        new PassesFragment()).commit();
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if(drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else{
            super.onBackPressed();
        }
    }

}

