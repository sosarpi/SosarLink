package be.kuleuven.ee5.eliasstalpaert.sosarlink;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.adeel.library.easyFTP;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import static android.content.Context.JOB_SCHEDULER_SERVICE;

public class CapturesFragment extends Fragment {

    private static final String TAG = CapturesFragment.class.getSimpleName();
    private static final int DEFAULT_POLLING_INTERVAL = 2;
    private static final String REMOTE_IMAGES_DIRECTORY = "/files";
    public static final int POLLINGJOB_ID = 1;
    public static final String POLLING_INTERVAL_KEY = "jobinterval";
    public static final String POLLING_ENABLE_KEY = "jobenable";

    private static boolean prompt_displayed = false;

    private RecyclerView recyclerView;
    private RecyclerAdapter recyclerAdapter;

    private BroadcastReceiver notificationReceiver;
    private MainActivity mainActivity;
    private ArrayList<RecyclerItem> recyclerList;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPreferencesEditor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = (MainActivity) getActivity();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity);
        sharedPreferencesEditor = sharedPreferences.edit();

        //Assigning BroadcastReceiver who receives a broadcast from PollingJob.ConnectTask whenever it adds a new capture to the stringList in SharedPreferences
        notificationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateRecyclerView();
                Log.d(TAG, "Broadcast received");
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_captures, container, false);
        recyclerView = fragmentView.findViewById(R.id.recyclerView);

        setupCapturesUI();
        setupRecyclerView();

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mainActivity.registerReceiver(notificationReceiver, new IntentFilter("SATELLITE"));
        updateRecyclerView();

        if (sharedPreferences.getString(SettingsFragment.IP_KEY, null) == null) {
            if (!prompt_displayed) promptServerIp();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mainActivity.unregisterReceiver(notificationReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.captures_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id) {
            case R.id.deleteAllMenu:
                removeAllRecyclerItems();
                break;
            case R.id.refreshButton:
                if (sharedPreferences.getBoolean(CapturesFragment.POLLING_ENABLE_KEY, true)) {
                    refreshPollingJob();
                } else {
                    //Start a single ConnectTask
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            new PollingJob.ConnectTask(mainActivity, sharedPreferences.getString(SettingsFragment.IP_KEY, null)).execute("");
                            Log.d(TAG, "ConnectTask started");
                        }
                    }).start();
                }
                Toast.makeText(mainActivity, "Refreshed manually", Toast.LENGTH_SHORT).show();
                break;
            case R.id.jobSettingsMenu:
                promptPollingSettings();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupCapturesUI() {
        this.setHasOptionsMenu(true);
        mainActivity.setTitle("Captures");
        recyclerView.setBackgroundResource(R.drawable.background_space1);
        mainActivity.getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.spaceAccent)));
    }



    private AlertDialog buildAlertDialog(String title, int layoutId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle(title);
        builder.setCancelable(false); //AlertDialog can't be dismissed, so the user has to perform an appropriate action
        builder.setView(layoutId);
        return builder.create();
    }



    private void setupRecyclerView() {
        recyclerList = new ArrayList<>();

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mainActivity);

        //New entries are placed at the top of the RecyclerView
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);

        recyclerAdapter = new RecyclerAdapter(recyclerList);
        recyclerAdapter.setOnItemClickListener(new RecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                getImageFromRecyclerItem(recyclerList.get(position));
            }
        });

        //Better performance: if RecyclerView has fixed size, other layouts in the hierarchy don't have to dynamically adjust when items are added/removed
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(recyclerAdapter);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                removeRecyclerItemByPosition(viewHolder.getAdapterPosition());
            }
        }).attachToRecyclerView(recyclerView);
    }

    private void updateRecyclerView() {
        ArrayList<String> stringList = MainActivity.getArrayList(MainActivity.LIST_NAME, mainActivity);

        if (stringList != null) {
            Log.d(TAG, "Stringlist found");
            for (String s : stringList) addToRecyclerViewIfNotPresent(s);
            recyclerAdapter.notifyDataSetChanged();
            //Automatically scroll to the top of the RecyclerView when a new entry is added
            recyclerView.smoothScrollToPosition(recyclerAdapter.getItemCount());
        } else {
            Log.d(TAG, "Stringlist not found");
        }

    }

    private void addToRecyclerViewIfNotPresent(String stringListEntry) {
        String satelliteName = stringListEntry.substring(0, 6);
        String timeDate = stringListEntry.substring(6, 18);
        boolean found = false;
        for (RecyclerItem recyclerItem : recyclerList) {
            if (recyclerItem.getSatelliteName().equals(satelliteName) && recyclerItem.getTimeDate().equals(timeDate))
                found = true;
        }
        if (!found) {
            recyclerView.smoothScrollToPosition(0);
            recyclerList.add(new RecyclerItem(satelliteName, timeDate));
        }
    }

    private void getImageFromRecyclerItem(RecyclerItem recyclerItem) {
        String serverIp = sharedPreferences.getString(SettingsFragment.IP_KEY, null);
        String ftpUsername = sharedPreferences.getString(SettingsFragment.FTPUSER_KEY, null);
        String ftpPassword = sharedPreferences.getString(SettingsFragment.FTPPASS_KEY, null);

        if (serverIp == null) {
            promptServerIp();
        }
        if (ftpPassword == null || ftpUsername == null) {
            promptFtpLogin();
        } else {
            String image_name = getImageNameFromRecyclerItem(recyclerItem);
            if (image_name != null) {

                AlertDialog alertDialog = buildAlertDialog("Downloading...", R.layout.download_alertdialog);
                alertDialog.show();
                alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        //Returning the possibility to interact with the activity to the user
                        mainActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    }
                });

                TextView textView = alertDialog.findViewById(R.id.downloadImageName);
                textView.setText(image_name + "\nto\n" + mainActivity.getFilesDir());

                //Removing user interaction with the activity when the image is downloading
                mainActivity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                new DownloadTask(image_name, alertDialog).execute();
            } else
                Toast.makeText(mainActivity, "Failed: Invalid capture", Toast.LENGTH_LONG).show();
        }
    }

    private String getImageNameFromRecyclerItem(RecyclerItem recyclerItem) {
        String imageName = null;
        String satelliteName = recyclerItem.getSatelliteName();
        String timeDate = recyclerItem.getTimeDate();
        String hours = timeDate.substring(0, 2);
        String minutes = timeDate.substring(2, 4);
        String seconds = timeDate.substring(4, 6);
        String year = timeDate.substring(6, 8);
        String month = timeDate.substring(8, 10);
        String day = timeDate.substring(10, 12);

        if (satelliteName.contains("NOAA")) {
            imageName = satelliteName + "20" + year + month + day + "-" + hours + minutes + seconds + ".png";
        } else if (satelliteName.contains("METEOR")) {
            imageName = "METEOR-M220" + year + month + day + "-" + hours + minutes + seconds + ".bmp";
        }

        return imageName;
    }

    private void removeRecyclerItemByPosition(int position) {
        RecyclerItem recyclerItem = recyclerList.get(position);
        if (recyclerItem != null) {
            //Remove the entry from the StringList that keeps track of all the satellite captures (stored in SharedPreferences)
            String toBeRemoved_string = recyclerItem.getSatelliteName() + recyclerItem.getTimeDate();
            ArrayList<String> stringList = MainActivity.getArrayList(MainActivity.LIST_NAME, mainActivity);
            if (stringList != null) {
                Iterator<String> iterator = stringList.iterator();
                boolean found = false;

                while (iterator.hasNext() && !found) {
                    String next = iterator.next();
                    if (next.equals(toBeRemoved_string)) {
                        iterator.remove();
                        found = true;
                        MainActivity.saveArrayList(stringList, MainActivity.LIST_NAME, mainActivity);
                        Log.d(TAG, "Element removed from SharedPreferences");
                    }
                }
                if (!found) {
                    Log.d(TAG, "Element not found in SharedPreferences");
                }
            } else {
                Log.d(TAG, "Stringlist not found");
            }
            //If present, remove the downloaded image from the internal storage
            removeImageFromStorage(getImageNameFromRecyclerItem(recyclerItem));
        }
        (recyclerAdapter).removeItemAtPosition(position);
    }

    private void removeAllRecyclerItems() {
        if(recyclerList.size() > 0){
            for (RecyclerItem recyclerItem : recyclerList) {
                removeImageFromStorage(getImageNameFromRecyclerItem(recyclerItem));
            }
            recyclerList.clear();
            recyclerAdapter.notifyDataSetChanged();
            Toast.makeText(mainActivity, "All captures deleted", Toast.LENGTH_SHORT).show();
        }

        ArrayList<String> stringList = MainActivity.getArrayList(MainActivity.LIST_NAME, mainActivity);
        if(stringList != null){
            stringList.clear();
            MainActivity.saveArrayList(stringList, MainActivity.LIST_NAME, mainActivity);

        }
    }


    private void promptServerIp() {
        //In order to avoid multiple prompts being launched, a static boolean is used (may happen if there is a method used of the MainActivity-instance)
        prompt_displayed = true;

        final AlertDialog alertDialog = buildAlertDialog("Setup: IP", R.layout.ip_alertdialog);
        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                prompt_displayed = false;
                Toast.makeText(mainActivity, "IP set to: " + sharedPreferences.getString(SettingsFragment.IP_KEY, null), Toast.LENGTH_SHORT).show();
                refreshPollingJob();
            }
        });
        alertDialog.show();

        Button submitIpButton = alertDialog.findViewById(R.id.submitIpButton);
        Button defaultGatewayButton = alertDialog.findViewById(R.id.defaultGatewayButton);
        final EditText ipEditText = alertDialog.findViewById(R.id.ipEditText);

        submitIpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ip = ipEditText.getText().toString();
                if (mainActivity.isAnIpv4Address(ip)) {
                    sharedPreferencesEditor.putString(SettingsFragment.IP_KEY, ip);
                    sharedPreferencesEditor.apply();
                    alertDialog.dismiss();

                } else
                    Toast.makeText(mainActivity, "Failed: Enter a valid IP" + ip, Toast.LENGTH_SHORT).show();
            }
        });

        defaultGatewayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sharedPreferencesEditor.putString(SettingsFragment.IP_KEY, mainActivity.getGatewayIp());
                sharedPreferencesEditor.apply();
                alertDialog.dismiss();
            }
        });


    }

    private void promptPollingSettings() {

        final AlertDialog alertDialog = buildAlertDialog("Polling Settings", R.layout.polling_settings);
        alertDialog.show();

        Button save = alertDialog.findViewById(R.id.savePollingSettings);
        Button cancel = alertDialog.findViewById(R.id.cancelPollingSettings);

        final EditText interval = alertDialog.findViewById(R.id.pollingInt);
        final Switch enable = alertDialog.findViewById(R.id.pollingEnable);

        interval.setText(String.valueOf(sharedPreferences.getInt(CapturesFragment.POLLING_INTERVAL_KEY, 0)));
        enable.setChecked(sharedPreferences.getBoolean(CapturesFragment.POLLING_ENABLE_KEY, true));

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    int num = Integer.parseInt(interval.getText().toString());
                    if (num <= 0) {
                        throw new NumberFormatException();
                    }
                    sharedPreferencesEditor.putInt(CapturesFragment.POLLING_INTERVAL_KEY, num);
                    sharedPreferencesEditor.putBoolean(CapturesFragment.POLLING_ENABLE_KEY, enable.isChecked());
                    sharedPreferencesEditor.apply();
                    Toast.makeText(mainActivity, "Polling settings saved", Toast.LENGTH_SHORT).show();
                    refreshPollingJob();
                    alertDialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(mainActivity, "Failed: Please enter a valid interval", Toast.LENGTH_LONG).show();
                }
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.cancel();
            }
        });
    }

    private void promptFtpLogin() {
        final AlertDialog alertDialog = buildAlertDialog("Setup: FTP", R.layout.ftp_credentials);
        alertDialog.show();

        final EditText username = alertDialog.findViewById(R.id.ftp_username);
        final EditText password = alertDialog.findViewById(R.id.ftp_password);
        //Hide password text with dots
        password.setTransformationMethod(PasswordTransformationMethod.getInstance());

        Button submit = alertDialog.findViewById(R.id.ftpCredSubmit);
        Button cancel = alertDialog.findViewById(R.id.ftpCredCancel);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sharedPreferencesEditor.putString(SettingsFragment.FTPUSER_KEY, username.getText().toString());
                sharedPreferencesEditor.putString(SettingsFragment.FTPPASS_KEY, password.getText().toString());
                sharedPreferencesEditor.apply();
                Toast.makeText(mainActivity, "Credentials submitted", Toast.LENGTH_SHORT).show();
                alertDialog.dismiss();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.cancel();
            }
        });
    }


    private void refreshPollingJob() {
        boolean pollingEnabled = sharedPreferences.getBoolean(CapturesFragment.POLLING_ENABLE_KEY, true);
        int pollingInterval = sharedPreferences.getInt(CapturesFragment.POLLING_INTERVAL_KEY, 0);
        JobScheduler jobScheduler = (JobScheduler) mainActivity.getSystemService(JOB_SCHEDULER_SERVICE);

        if (pollingEnabled) {
            String ip = sharedPreferences.getString(SettingsFragment.IP_KEY, null);
            if (ip == null) {
                promptServerIp();
            }
            else {
                if (pollingInterval <= 0) {
                    pollingInterval = CapturesFragment.DEFAULT_POLLING_INTERVAL;
                    sharedPreferencesEditor.putInt(CapturesFragment.POLLING_INTERVAL_KEY, CapturesFragment.DEFAULT_POLLING_INTERVAL).apply();
                }

                jobScheduler.cancelAll();
                //Give server IP and polling interval as extras to the job, so it can give it to the next job (job refreshes itself and needs a reference to these variables)
                PersistableBundle extras = new PersistableBundle();
                extras.putString(SettingsFragment.IP_KEY, ip);
                extras.putInt(CapturesFragment.POLLING_INTERVAL_KEY, pollingInterval);

                JobInfo.Builder mJobBuilder =
                        new JobInfo.Builder(CapturesFragment.POLLINGJOB_ID,
                                new ComponentName(mainActivity, PollingJob.class))
                                //Scheduled jobs should persist between reboots of the device
                                .setPersisted(true)
                                .setExtras(extras)
                                //The device should be connected to a WiFi network in order to let the job run (checking on SSID is only available since Android P, so this is the best we can do for now)
                                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);

                int resultCode = jobScheduler.schedule(mJobBuilder.build());
                if (resultCode == JobScheduler.RESULT_SUCCESS) {
                    Log.d(TAG, "Jobs refreshed with pollingInterval: " + pollingInterval + " minutes");
                } else {
                    Log.d(TAG, "Failed to refresh jobs");
                }
            }
        }
        else {
            jobScheduler.cancelAll();
            Log.d(TAG, "Canceled all jobs");
        }
    }

    private void removeImageFromStorage(String imageName) {
        String filePath = mainActivity.getFilesDir() + File.separator + imageName;
        File fileToBeDeleted = new File(filePath);
        try {
            if (fileToBeDeleted.exists()) {
                boolean deleted = fileToBeDeleted.delete();
                if (deleted) {
                    Log.d(TAG, imageName + " deleted");
                } else {
                    Log.d(TAG, "Could not find " + imageName + " in files dir");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private class DownloadTask extends AsyncTask<String, Void, String> {
        private String imageName;
        private String localPath;
        private AlertDialog alertDialog;

        DownloadTask(String image_name, AlertDialog alertDialog) {
            this.imageName = image_name;
            this.localPath = mainActivity.getFilesDir().toString() + File.separator + image_name;
            this.alertDialog = alertDialog;
        }

        @Override
        protected String doInBackground(String... params) {
            //Returns null if something went wrong concerning FTP Settings
            try {
                File file = new File(localPath);
                if (!file.exists()) {
                    easyFTP ftp = new easyFTP();

                    String username = sharedPreferences.getString(SettingsFragment.FTPUSER_KEY, null);
                    String ip = sharedPreferences.getString(SettingsFragment.IP_KEY, null);
                    String password = sharedPreferences.getString(SettingsFragment.FTPPASS_KEY, null);

                    ftp.connect(ip, username, password);

                    if (ftp.getFtpClient().isConnected()) {

                        if (ftp.getFtpClient().login(username, password)) {
                            ftp.downloadFile(CapturesFragment.REMOTE_IMAGES_DIRECTORY + File.separator +  imageName, localPath);
                            return localPath;
                        } else return null;

                    } else return null;

                } else return localPath;

            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "Failure: " + e.getLocalizedMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            alertDialog.dismiss();
            viewFile(s);
        }
    }

    private void viewFile(String localPath) {
        if (localPath == null) {
            Toast.makeText(mainActivity, "FTP Error: check ftp settings otherwise check log for exception", Toast.LENGTH_LONG).show();
        } else {
            File imagePath = new File(localPath);

            if (imagePath.exists()) {

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(localPath, options);
                //Check if the file that is present on the system, is a valid image file
                if (options.outWidth != -1 && options.outHeight != -1) {
                    Uri uri = FileProvider.getUriForFile(mainActivity, BuildConfig.APPLICATION_ID + ".provider", imagePath);

                    Intent galleryIntent = new Intent(android.content.Intent.ACTION_VIEW);
                    galleryIntent.setDataAndType(uri, "image/*");
                    galleryIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    mainActivity.startActivity(galleryIntent);
                } else {
                    //If the present file isn't an image, this means it wasn't on the server (easyFTP always creates a file when trying to download something (container for the inputstream))
                    Toast.makeText(mainActivity, "Failed: File not present on server", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "File not valid (not on server), deleting... " + imagePath.delete());
                }
            } else {
                Log.d(TAG, "File not found");
            }
        }
    }
}
