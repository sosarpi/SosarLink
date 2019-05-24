package be.kuleuven.ee5.eliasstalpaert.sosarlink;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import static android.content.Context.JOB_SCHEDULER_SERVICE;

public class PassesFragment extends Fragment {

    private static final int DOWNLOAD_FILES_REQUEST = 0;
    private static final String TAG = PassesFragment.class.getSimpleName();


    private RecyclerView mRecyclerView;
    private RecyclerAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private BroadcastReceiver mNotificationReceiver;
    private MainActivity mainActivity;
    private ArrayList<RecyclerItem> mRecyclerList;
    private SharedPreferences sharedPreferences;

    private String imageToBeOpened;
    private Boolean callback;

    private View fragmentView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_passes, container, false);
        mRecyclerView = fragmentView.findViewById(R.id.recyclerView);
        imageToBeOpened = null;
        buildRecyclerView();

        return fragmentView;
    }

    public void promptForIp() {
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle("First Time Setup of IP");
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
                refreshJob();
            }
        });


    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setHasOptionsMenu(true);

        mainActivity = (MainActivity) getActivity();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity);
        mainActivity.setTitle("Captures");

        mNotificationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateRecyclerView();
                Log.d(TAG, "Broadcast received");
            }
        };
    }

    public boolean refreshJob() {
        String ip = sharedPreferences.getString(FtpFragment.FTPIP_KEY, null);
        if(ip == null) {
            promptForIp();
            return false;
        }
        else{
            JobScheduler mJobScheduler = (JobScheduler) mainActivity.getSystemService(JOB_SCHEDULER_SERVICE);
            mJobScheduler.cancelAll();

            PersistableBundle extras = new PersistableBundle();
            extras.putString(FtpFragment.FTPIP_KEY,ip);

            JobInfo.Builder mJobBuilder =
                    new JobInfo.Builder(1,
                            new ComponentName(mainActivity, Job.class))
                            .setPersisted(true)
                            .setExtras(extras)
                            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);

            int resultCode = mJobScheduler.schedule(mJobBuilder.build());
            if (resultCode == JobScheduler.RESULT_SUCCESS) {
                Log.d(TAG, "Jobs refreshed successfully");
                return true;
            } else {
                Log.d(TAG, "Failed to refresh jobs");
                return false;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(sharedPreferences.getString(FtpFragment.FTPIP_KEY, null) == null) {
            promptForIp();
        }

        mainActivity.registerReceiver(mNotificationReceiver, new IntentFilter("SATELLITE"));
        updateRecyclerView();
    }

    @Override
    public void onPause() {
        super.onPause();
        mainActivity.unregisterReceiver(mNotificationReceiver);
    }

    private void buildRecyclerView() {
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
                removeRecyclerItems(viewHolder.getAdapterPosition());
            }
        }).attachToRecyclerView(mRecyclerView);
    }

    public void getImageFromRecyclerItem(RecyclerItem recyclerItem) {
        String ftpIp = sharedPreferences.getString(FtpFragment.FTPIP_KEY, null);
        String ftpUsername = sharedPreferences.getString(FtpFragment.FTPUSER_KEY, null);
        String ftpPassword = sharedPreferences.getString(FtpFragment.FTPPASS_KEY, null);
        String localDir = sharedPreferences.getString(FtpFragment.LOCALDIR_KEY, null);



        if(ftpIp == null) {
            promptForIp();
            ftpIp = sharedPreferences.getString(FtpFragment.FTPIP_KEY, null);
        }
        if(ftpPassword == null || ftpUsername == null) {
            firstTimeFtpCredentials();
        }
        else if(isLocalDirValid()) {
            String image_name = getImageNameFromCapture(recyclerItem);
            this.setImageToBeOpened(image_name);

            File file = new File(localDir + File.separator + image_name);
            if(file.exists()){
                viewFile(image_name,localDir);
            }
            else{
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_PICK);
                Uri ftpUri = Uri.parse("ftp://" + ftpIp);
                intent.setDataAndType(ftpUri, "vnd.android.cursor.dir/lysesoft.andftp.uri");
                intent.putExtra("command_type", "download");
                intent.putExtra("ftp_username", ftpUsername);
                intent.putExtra("ftp_password", ftpPassword);
                intent.putExtra("ftp_overwrite","skip");
                intent.putExtra("progress_title", "Downloading picture...");
                intent.putExtra("remote_file1", "/files/" + image_name);
                intent.putExtra("local_folder", localDir);
                intent.putExtra("close_ui", "true");
                intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(intent, DOWNLOAD_FILES_REQUEST);
            }
        }
    }

    public boolean isLocalDirValid(){
        String localDir = sharedPreferences.getString(FtpFragment.LOCALDIR_KEY,null);
        if(localDir == null){

            FtpFragment ftpFragment = new FtpFragment();
            Bundle args = new Bundle();
            args.putBoolean("callback",true);
            ftpFragment.setArguments(args);

            mainActivity.getmNavigationView().setCheckedItem(R.id.nav_ftp);
            mainActivity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    ftpFragment).commit();
            return false;
        }
        return true;
    }

    public static String getGatewayIp(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();

        String gatewayIP = formatIP(dhcpInfo.gateway);
        return  gatewayIP;
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

    public void firstTimeFtpCredentials(){
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle("First Time Setup of FTP Credentials");
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
                isLocalDirValid();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.cancel();
            }
        });
    }

    public void updateRecyclerView() {

        Log.d(TAG, "Updating UI");

        ArrayList<String> stringList = MainActivity.getArrayList(MainActivity.LIST_NAME,mainActivity);
        if (stringList != null) {
            Log.d(TAG, "Stringlist found");
            Iterator<String> iterator = stringList.iterator();
            while (iterator.hasNext()) {
                String next = iterator.next();
                parseStringListEntry(next);
            }
            mAdapter.notifyDataSetChanged();
            mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
        } else {
            Log.d(TAG, "Stringlist not found");
        }

    }

    public void parseStringListEntry(String s) {
        String satellite_name = s.substring(0, 6);
        String concatTimeDate = s.substring(6, 18);
        Iterator<RecyclerItem> itemIterator = mRecyclerList.iterator();
        boolean found = false;
        while(itemIterator.hasNext() && found == false) {
            RecyclerItem next = itemIterator.next();
            if(next.getText1().equals(satellite_name) && next.getText2().equals(concatTimeDate)) {
                found = true;
            }
        }
        if(found == false) {
            mRecyclerView.smoothScrollToPosition(0);
            mRecyclerList.add(new RecyclerItem(0, satellite_name, concatTimeDate));
        }
    }

    public String getImageNameFromCapture(RecyclerItem capture){
        String image_name = "error.jpg";
        String satelliteName = capture.getText1();
        String concatTimeDate = capture.getText2();
        String hours = concatTimeDate.substring(0, 2);
        String minutes = concatTimeDate.substring(2, 4);
        String seconds = concatTimeDate.substring(4, 6);
        String year = concatTimeDate.substring(6, 8);
        String month = concatTimeDate.substring(8, 10);
        String day = concatTimeDate.substring(10, 12);

        if(satelliteName.contains("NOAA")){
            image_name = satelliteName + "20" + year + month + day + "-" + hours + minutes + seconds + ".png";
        }
        else if(satelliteName.contains("METEOR")){
            image_name = "METEOR-M220" + year + month + day + "-" + hours + minutes + seconds + ".png";
        }

        return image_name;
    }

    public void removeRecyclerItems(int position) {
        RecyclerItem toBeRemoved_item = mRecyclerList.get(position);
        if (toBeRemoved_item != null) {
            String toBeRemoved_string = toBeRemoved_item.getText1() + toBeRemoved_item.getText2();
            ArrayList<String> stringList = MainActivity.getArrayList(MainActivity.LIST_NAME,mainActivity);
            if (stringList != null) {
                Iterator<String> iterator = stringList.iterator();
                boolean found = false;
                while (iterator.hasNext() && found == false) {
                    String next = iterator.next();
                    if (next.equals(toBeRemoved_string)) {
                        iterator.remove();
                        found = true;
                        MainActivity.saveArrayList(stringList,MainActivity.LIST_NAME,mainActivity);
                        Log.d(TAG, "Element removed from SharedPreferences");
                    }
                }
                if (found == false) {
                    Log.d(TAG, "Element not found in SharedPreferences");
                }
            } else {
                Log.d(TAG, "Stringlist not found");
            }


            /*
            String image_name = getImageNameFromCapture(toBeRemoved_item);
            String localDir = sharedPreferences.getString(FtpFragment.LOCALDIR_KEY, null);
            if(localDir != null){
                String filepath = localDir + File.separator + image_name;
                int rows_deleted = mainActivity.getContentResolver().delete(FileProvider.getUriForFile(mainActivity,BuildConfig.APPLICATION_ID + ".provider",new File(filepath)),null,null);
                File file = new File(filepath);
                try{
                    if(file.exists()){
                        Log.d("ImageDeleter", "Trying to delete: " + filepath + "   " + rows_deleted);
                        boolean deleted = file.getCanonicalFile().delete();
                        if(deleted){
                            Log.d("ImageDeleter", "Image successfully deleted");
                        }
                        else{
                            Log.d("ImageDeleter", "Failed to delete image");
                        }
                    }
                    else{
                        Log.d("ImageDeleter", "File not found");
                    }
                }
                catch (Exception e){
                    if(e instanceof IOException){
                        Log.d("IOException", "File to be deleted not found");
                    }
                }

            }
            */
        }
        (mAdapter).removeItem(position);
    }

    public void removeAllCaptures() {
        mRecyclerList.clear();
        ArrayList<String> stringList = MainActivity.getArrayList(MainActivity.LIST_NAME,mainActivity);
        stringList.clear();
        MainActivity.saveArrayList(stringList,MainActivity.LIST_NAME,mainActivity);
        mAdapter.notifyDataSetChanged();
        Toast.makeText(mainActivity,"All captures deleted",Toast.LENGTH_SHORT).show();
    }

    public void setCustomBackground() {
        mRecyclerView.setBackgroundResource(R.drawable.background_space1);
        mainActivity.getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.spaceAccent)));
    }

    public void setImageToBeOpened(String imageName){
        this.imageToBeOpened = imageName;
    }

    public String getImageToBeOpened(){
        return this.imageToBeOpened;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.passes_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if(id == R.id.deleteAll) {
            removeAllCaptures();
        }
        if(id == R.id.refreshButton) {
            if(refreshJob()){
                Toast.makeText(mainActivity, "Manually refreshed jobs", Toast.LENGTH_SHORT);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public void viewFile(String file_name, String directory) {
        File imagePath = new File(directory + File.separator + file_name);
        Intent galleryIntent = new Intent(android.content.Intent.ACTION_VIEW);

        Uri uri = FileProvider.getUriForFile(mainActivity, BuildConfig.APPLICATION_ID + ".provider", imagePath);

        galleryIntent.setDataAndType(uri, "image/*");
        galleryIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        mainActivity.startActivity(galleryIntent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);

        if(requestCode == DOWNLOAD_FILES_REQUEST) {
            if (data != null) {
                String status = data.getStringExtra("TRANSFERSTATUS");
                Log.d(TAG, "Transfer status: " + status);
                if (status.equals("COMPLETED")) {

                    String localDir = sharedPreferences.getString(FtpFragment.LOCALDIR_KEY, null);
                    if(localDir != null){
                        viewFile(this.getImageToBeOpened(), localDir);
                    }
                }
                else if (status.equals("FAILED")){
                    Toast.makeText(mainActivity, "Download failed",Toast.LENGTH_SHORT).show();
                }
            }
        }

    }

}
