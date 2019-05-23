package be.kuleuven.ee5.eliasstalpaert.sosarlink;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";
    public static final String LIST_NAME = "satellite";

    private DrawerLayout drawer;
    private NavigationView mNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.clear();
        editor.apply();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mNavigationView = findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                new PassesFragment()).commit();
        mNavigationView.setCheckedItem(R.id.nav_passes);

        initialJob(); //Schedule Jobs

    }

    public NavigationView getmNavigationView() {
        return mNavigationView;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void initialJob() {
        /*
        ComponentName componentName = new ComponentName(this, Job.class);
        JobInfo info = new JobInfo.Builder(1, componentName)
                .setPersisted(true)            //Na reboot zal job nog altijd onthouden worden.
                .setPeriodic(1 * 60 * 1000)    //Job iedere 30 minuten. Minimum mogelijk in te stellen is 15 minuten.
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .build();
        */

        JobInfo.Builder mJobBuilder =
                new JobInfo.Builder(1,
                        new ComponentName(this, Job.class))
                .setPersisted(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);

        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        int resultCode = scheduler.schedule(mJobBuilder.build());
        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Initial job successfully scheduled");
        } else {
            Log.d(TAG, "Failed to schedule initial job");
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

    public static void saveArrayList(ArrayList<String> list, String key, Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString(key, json);
        editor.apply();     // This line is IMPORTANT !!!
        Log.d("Static", "ArrayList saved");
    }

    public static ArrayList<String> getArrayList(String key, Context context){
        Log.d("Static", "Trying to receive ArrayList");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Gson gson = new Gson();
        String json = prefs.getString(key, null);
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        return gson.fromJson(json, type);
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

}

