package be.kuleuven.ee5.eliasstalpaert.sosarlink;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.adeel.library.easyFTP;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import static android.content.Context.JOB_SCHEDULER_SERVICE;

public class PassesFragment extends Fragment {

    private static final String TAG = PassesFragment.class.getSimpleName();
    private static boolean prompt_displayed = false;
    private static final int DEFAULT_INTERVAL = 2;
    public static final String JOB_INTERVAL_KEY = "jobinterval";
    public static final String JOB_ENABLE_KEY = "jobenable";


    private RecyclerView mRecyclerView;
    private RecyclerAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private BroadcastReceiver mNotificationReceiver;
    private MainActivity mainActivity;
    private ArrayList<RecyclerItem> mRecyclerList;
    private SharedPreferences sharedPreferences;

    private View fragmentView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setHasOptionsMenu(true);

        mainActivity = (MainActivity) getActivity();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity);
        mainActivity.setTitle("Captures");

        refreshJob();

        mNotificationReceiver = new BroadcastReceiver() {
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
        fragmentView = inflater.inflate(R.layout.fragment_passes, container, false);
        mRecyclerView = fragmentView.findViewById(R.id.recyclerView);
        setupRecyclerView();

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mainActivity.registerReceiver(mNotificationReceiver, new IntentFilter("SATELLITE"));
        updateRecyclerView();

        if (sharedPreferences.getString(FtpFragment.FTPIP_KEY, null) == null) {
            Log.d(TAG, "server_ip is null af (resume)");
            if (!prompt_displayed) promptForIp();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mainActivity.unregisterReceiver(mNotificationReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.passes_menu, menu);
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
                if (sharedPreferences.getBoolean(PassesFragment.JOB_ENABLE_KEY, true)) {
                    refreshJob();
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            new Job.ConnectTask(mainActivity, sharedPreferences.getString(FtpFragment.FTPIP_KEY, null)).execute("");
                            Log.d(TAG, "ConnectTask started");
                        }
                    }).start();
                    Toast.makeText(mainActivity, "Refreshed manually", Toast.LENGTH_SHORT);
                }
                break;
            case R.id.jobSettingsMenu:
                jobSettingsDialog();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setCustomBackground() {
        mRecyclerView.setBackgroundResource(R.drawable.background_space1);
        mainActivity.getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.spaceAccent)));
    }

    private void jobSettingsDialog() {
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle("Polling Settings");
        builder.setCancelable(false);
        builder.setView(R.layout.job_settings);
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        Button save = alertDialog.findViewById(R.id.savePollingSettings);
        Button cancel = alertDialog.findViewById(R.id.cancelPollingSettings);

        final EditText interval = alertDialog.findViewById(R.id.pollingInt);
        if (interval != null) {
            interval.setText(String.valueOf(sharedPreferences.getInt(PassesFragment.JOB_INTERVAL_KEY, 0)));
        }
        //interval.setText();

        final Switch enable = alertDialog.findViewById(R.id.pollingEnable);
        enable.setChecked(sharedPreferences.getBoolean(PassesFragment.JOB_ENABLE_KEY, true));


        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    int num = Integer.parseInt(interval.getText().toString());
                    if (num <= 0) {
                        throw new NumberFormatException();
                    }
                    editor.putInt(PassesFragment.JOB_INTERVAL_KEY, num);
                    editor.putBoolean(PassesFragment.JOB_ENABLE_KEY, enable.isChecked());
                    editor.apply();
                    Toast.makeText(mainActivity, "Polling settings saved", Toast.LENGTH_SHORT).show();
                    refreshJob();
                    alertDialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(mainActivity, "Please enter a valid interval", Toast.LENGTH_LONG).show();
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


    private void setupRecyclerView() {
        mRecyclerView.setHasFixedSize(true); //better performance
        this.setCustomBackground();
        mLayoutManager = new LinearLayoutManager(mainActivity);
        ((LinearLayoutManager) mLayoutManager).setReverseLayout(true);
        ((LinearLayoutManager) mLayoutManager).setStackFromEnd(true);
        mRecyclerList = new ArrayList<>();
        mAdapter = new RecyclerAdapter(mRecyclerList);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setOnItemClickListener(new RecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                getImageFromRecyclerItem(mRecyclerList.get(position));
            }
        });

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                removeRecyclerItemByPosition(viewHolder.getAdapterPosition());
            }
        }).attachToRecyclerView(mRecyclerView);
    }

    private void updateRecyclerView() {

        ArrayList<String> stringList = MainActivity.getArrayList(MainActivity.LIST_NAME, mainActivity);
        if (stringList != null) {
            Log.d(TAG, "Stringlist found");
            Iterator<String> iterator = stringList.iterator();
            while (iterator.hasNext()) {
                String next = iterator.next();
                addToRecyclerViewIfNotPresent(next);
            }
            mAdapter.notifyDataSetChanged();
            mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
        } else {
            Log.d(TAG, "Stringlist not found");
        }

    }

    private void addToRecyclerViewIfNotPresent(String s) {
        String satellite_name = s.substring(0, 6);
        String concatTimeDate = s.substring(6, 18);
        Iterator<RecyclerItem> itemIterator = mRecyclerList.iterator();
        boolean found = false;
        while (itemIterator.hasNext() && found == false) {
            RecyclerItem next = itemIterator.next();
            if (next.getText1().equals(satellite_name) && next.getText2().equals(concatTimeDate)) {
                found = true;
            }
        }
        if (found == false) {
            mRecyclerView.smoothScrollToPosition(0);
            mRecyclerList.add(new RecyclerItem(satellite_name, concatTimeDate));
        }
    }

    private void getImageFromRecyclerItem(RecyclerItem recyclerItem) {
        String ftpIp = sharedPreferences.getString(FtpFragment.FTPIP_KEY, null);
        String ftpUsername = sharedPreferences.getString(FtpFragment.FTPUSER_KEY, null);
        String ftpPassword = sharedPreferences.getString(FtpFragment.FTPPASS_KEY, null);

        if (ftpIp == null) {
            promptForIp();
        }
        if (ftpPassword == null || ftpUsername == null) {
            firstTimeFtpCredentials();
        } else {
            String image_name = getImageNameFromRecyclerItem(recyclerItem);
            TextView textView = new TextView(mainActivity);
            textView.setText(image_name);
            AlertDialog alertDialog = new AlertDialog.Builder(mainActivity)
                    .setTitle("Downloading...")
                    .setView(textView)
                    .setCancelable(false)
                    .create();
            alertDialog.show();

            new DownloadTask(image_name, alertDialog).execute();
        }
    }

    private String getImageNameFromRecyclerItem(RecyclerItem recyclerItem) {
        String image_name = "error.jpg";
        String satelliteName = recyclerItem.getText1();
        String concatTimeDate = recyclerItem.getText2();
        String hours = concatTimeDate.substring(0, 2);
        String minutes = concatTimeDate.substring(2, 4);
        String seconds = concatTimeDate.substring(4, 6);
        String year = concatTimeDate.substring(6, 8);
        String month = concatTimeDate.substring(8, 10);
        String day = concatTimeDate.substring(10, 12);

        if (satelliteName.contains("NOAA")) {
            image_name = satelliteName + "20" + year + month + day + "-" + hours + minutes + seconds + ".png";
        } else if (satelliteName.contains("METEOR")) {
            image_name = "METEOR-M220" + year + month + day + "-" + hours + minutes + seconds + ".png";
        }

        return image_name;
    }

    private void removeRecyclerItemByPosition(int position) {
        RecyclerItem recyclerItem = mRecyclerList.get(position);
        if (recyclerItem != null) {
            String toBeRemoved_string = recyclerItem.getText1() + recyclerItem.getText2();
            ArrayList<String> stringList = MainActivity.getArrayList(MainActivity.LIST_NAME, mainActivity);
            if (stringList != null) {
                Iterator<String> iterator = stringList.iterator();
                boolean found = false;
                while (iterator.hasNext() && found == false) {
                    String next = iterator.next();
                    if (next.equals(toBeRemoved_string)) {
                        iterator.remove();
                        found = true;
                        MainActivity.saveArrayList(stringList, MainActivity.LIST_NAME, mainActivity);
                        Log.d(TAG, "Element removed from SharedPreferences");
                    }
                }
                if (found == false) {
                    Log.d(TAG, "Element not found in SharedPreferences");
                }
            } else {
                Log.d(TAG, "Stringlist not found");
            }

            String image_name = getImageNameFromRecyclerItem(recyclerItem);
            removeImageFromCache(image_name);
        }
        (mAdapter).removeItemAtPosition(position);
    }

    private void removeAllRecyclerItems() {
        for (RecyclerItem recyclerItem : mRecyclerList) {
            removeImageFromCache(getImageNameFromRecyclerItem(recyclerItem));
        }
        mRecyclerList.clear();
        ArrayList<String> stringList = MainActivity.getArrayList(MainActivity.LIST_NAME, mainActivity);
        stringList.clear();
        MainActivity.saveArrayList(stringList, MainActivity.LIST_NAME, mainActivity);
        mAdapter.notifyDataSetChanged();
        Toast.makeText(mainActivity, "All captures deleted", Toast.LENGTH_SHORT).show();
    }


    private void promptForIp() {
        prompt_displayed = true;
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle("First Time Setup: IP");
        builder.setCancelable(false);
        builder.setView(R.layout.ip_alarmdialog);
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        Button submitIpButton = alertDialog.findViewById(R.id.submitIpButton);
        Button defaultGatewayButton = alertDialog.findViewById(R.id.defaultGatewayButton);

        final EditText ipEditText = alertDialog.findViewById(R.id.ipEditText);

        submitIpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ip = ipEditText.getText().toString();
                editor.putString(FtpFragment.FTPIP_KEY, ip);
                editor.apply();
                Toast.makeText(mainActivity, "IP set manually to: " + ip, Toast.LENGTH_SHORT).show();
                alertDialog.dismiss();
                prompt_displayed = false;
                refreshJob();
            }
        });
        defaultGatewayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String gatewayIp = getGatewayIp(mainActivity);
                editor.putString(FtpFragment.FTPIP_KEY, gatewayIp);
                editor.apply();
                Toast.makeText(mainActivity, "IP set to: " + gatewayIp, Toast.LENGTH_SHORT).show();
                alertDialog.dismiss();
                prompt_displayed = false;
                refreshJob();
            }
        });


    }

    public static String getGatewayIp(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();

        String gatewayIP = formatIP(dhcpInfo.gateway);
        return gatewayIP;
    }

    public static String formatIP(int ip) {
        return String.format(
                "%d.%d.%d.%d",
                (ip & 0xff),
                (ip >> 8 & 0xff),
                (ip >> 16 & 0xff),
                (ip >> 24 & 0xff)
        );
    }

    private void firstTimeFtpCredentials() {
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle("First Time Setup: FTP");
        builder.setView(R.layout.ftp_credentials);
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        final EditText username = alertDialog.findViewById(R.id.ftp_username);
        final EditText password = alertDialog.findViewById(R.id.ftp_password);
        password.setTransformationMethod(PasswordTransformationMethod.getInstance());

        Button submit = alertDialog.findViewById(R.id.ftpCredSubmit);
        Button cancel = alertDialog.findViewById(R.id.ftpCredCancel);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putString(FtpFragment.FTPUSER_KEY, username.getText().toString());
                editor.putString(FtpFragment.FTPPASS_KEY, password.getText().toString());
                editor.apply();
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


    private boolean refreshJob() {
        boolean enable = sharedPreferences.getBoolean(PassesFragment.JOB_ENABLE_KEY, true);
        int interval = sharedPreferences.getInt(PassesFragment.JOB_INTERVAL_KEY, 0);

        JobScheduler mJobScheduler = (JobScheduler) mainActivity.getSystemService(JOB_SCHEDULER_SERVICE);
        if (enable) {
            String ip = sharedPreferences.getString(FtpFragment.FTPIP_KEY, null);
            if (ip == null) {
                Log.d(TAG, "server_ip is null af (refreshJob)");
                promptForIp();
                return false;
            } else {
                if (interval <= 0) {
                    interval = PassesFragment.DEFAULT_INTERVAL;
                    sharedPreferences.edit().putInt(PassesFragment.JOB_INTERVAL_KEY, PassesFragment.DEFAULT_INTERVAL).apply();
                }

                mJobScheduler.cancelAll();

                PersistableBundle extras = new PersistableBundle();
                extras.putString(FtpFragment.FTPIP_KEY, ip);
                extras.putInt(PassesFragment.JOB_INTERVAL_KEY, interval);

                JobInfo.Builder mJobBuilder =
                        new JobInfo.Builder(1,
                                new ComponentName(mainActivity, Job.class))
                                .setPersisted(true)
                                .setExtras(extras)
                                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);

                int resultCode = mJobScheduler.schedule(mJobBuilder.build());
                if (resultCode == JobScheduler.RESULT_SUCCESS) {
                    Log.d(TAG, "Jobs refreshed with interval: " + interval + " minutes");
                    return true;
                } else {
                    Log.d(TAG, "Failed to refresh jobs");
                    return false;
                }
            }
        } else {
            mJobScheduler.cancelAll();
            Log.d(TAG, "Canceled all jobs");
            return true;
        }
    }

    private void removeImageFromCache(String image_name) {
        String filePath = mainActivity.getFilesDir() + File.separator + image_name;
        File fileToBeDeleted = new File(filePath);
        try {
            if (fileToBeDeleted.exists()) {
                boolean deleted = fileToBeDeleted.delete();
                if (deleted) {
                    Log.d(TAG, image_name + " deleted");
                } else {
                    Log.d(TAG, "Could not find " + image_name + " in files dir");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
                Bitmap bitmap = BitmapFactory.decodeFile(localPath, options);
                if (options.outWidth != -1 && options.outHeight != -1) {
                    Intent galleryIntent = new Intent(android.content.Intent.ACTION_VIEW);

                    Uri uri = FileProvider.getUriForFile(mainActivity, BuildConfig.APPLICATION_ID + ".provider", imagePath);

                    galleryIntent.setDataAndType(uri, "image/*");
                    galleryIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    mainActivity.startActivity(galleryIntent);
                } else {
                    Toast.makeText(mainActivity, "File not present on server", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "File not valid (not on server), deleting... " + imagePath.delete());
                }
            } else {
                Log.d(TAG, "File not found");
            }
        }
    }


    private class DownloadTask extends AsyncTask<String, Void, String> {
        private String image_name;
        private String local_path;
        private AlertDialog alertDialog;

        DownloadTask(String image_name, AlertDialog alertDialog) {
            this.image_name = image_name;
            this.local_path = mainActivity.getFilesDir().toString() + File.separator + image_name;
            this.alertDialog = alertDialog;
            mainActivity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        }

        @Override
        protected String doInBackground(String... params) {
            try {
                File file = new File(local_path);
                if (!file.exists()) {
                    Log.d(TAG, "Download started");
                    easyFTP ftp = new easyFTP();
                    String username = sharedPreferences.getString(FtpFragment.FTPUSER_KEY, null);
                    String ip = sharedPreferences.getString(FtpFragment.FTPIP_KEY, null);
                    String password = sharedPreferences.getString(FtpFragment.FTPPASS_KEY, null);
                    ftp.connect(ip, username, password);
                    if (ftp.getFtpClient().isConnected()) {
                        if (ftp.getFtpClient().login(username, password)) {
                            ftp.downloadFile(getString(R.string.path_of_images) + image_name, local_path);
                            Log.d(TAG, "Download Successfull");
                            return local_path;
                        } else return null;
                    } else return null;
                } else return local_path;

            } catch (Exception e) {
                e.printStackTrace();
                String t = "Failure : " + e.getLocalizedMessage();
                Log.d(TAG, t);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            mainActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            alertDialog.dismiss();
            Log.d(TAG, "result: " + s);
            viewFile(s);
        }
    }
}
