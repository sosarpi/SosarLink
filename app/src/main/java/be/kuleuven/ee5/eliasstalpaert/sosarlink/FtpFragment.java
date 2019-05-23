package be.kuleuven.ee5.eliasstalpaert.sosarlink;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
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

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

import net.rdrei.android.dirchooser.DirectoryChooserActivity;
import net.rdrei.android.dirchooser.DirectoryChooserConfig;

import java.io.File;

public class FtpFragment extends Fragment {

    private static final int REQUEST_DIRECTORY = 1;
    public static final String LOCALDIR_KEY = "localDir";
    public static final String FTPIP_KEY = "ftpIp";
    public static final String FTPUSER_KEY = "ftpUser";
    public static final String FTPPASS_KEY = "ftpPass";

    private EditText ftpIp, ftpUsername, ftpPassword;
    private Button submitButton;
    private View fragmentView;
    private MainActivity mainActivity;

    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_ftp, container, false);
        ftpIp = fragmentView.findViewById(R.id.ftpIp);
        submitButton = fragmentView.findViewById(R.id.submitButton);
        ftpUsername = fragmentView.findViewById(R.id.usernameFtp);
        ftpPassword = fragmentView.findViewById(R.id.passwordFtp);
        ftpPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateFtpSettings();
                Toast.makeText(mainActivity,"FTP Settings updated",Toast.LENGTH_SHORT);
            }
        });

        this.setHasOptionsMenu(true);

        return fragmentView;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        mainActivity.setTitle("FTP");
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity);

    }

    @Override
    public void onResume() {
        super.onResume();

        if(sharedPreferences.getString(FtpFragment.LOCALDIR_KEY, null) == null) {
            Toast.makeText(mainActivity, "Please choose a default save directory", Toast.LENGTH_LONG);
            changeDefaultSaveDir();
        }

        String username = sharedPreferences.getString(FtpFragment.FTPUSER_KEY, null);
        String password = sharedPreferences.getString(FtpFragment.FTPPASS_KEY, null);
        String ftpip = sharedPreferences.getString(FtpFragment.FTPIP_KEY, null);
        if(username != null || password != null){
            ftpUsername.setText(username);
            ftpPassword.setText(password);
        }
        else {
            ftpUsername.setText("");
            ftpPassword.setText("");
        }
        if(ftpip == null) {
            PassesFragment.setDefaultIP(mainActivity);
            ftpip = sharedPreferences.getString(FtpFragment.FTPIP_KEY, null);
        }
        ftpIp.setText(ftpip);


    }

    public void updateFtpSettings(){
        String username = ftpUsername.getText().toString();
        String password = ftpPassword.getText().toString();
        String ip_address = ftpIp.getText().toString();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(FTPUSER_KEY, username);
        editor.putString(FTPPASS_KEY, password);
        editor.putString(FTPIP_KEY, ip_address);
        editor.apply();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);

        switch (requestCode) {
            case REQUEST_DIRECTORY:

                    //String chosen_dir = data.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR);
                    Uri uri = data.getData();
                    String chosen_dir = getRealPathFromURI(mainActivity,uri);//assign it to a string(your choice).
                    Log.d("DirChoose", "Directory chosen: " + chosen_dir);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(FtpFragment.LOCALDIR_KEY, chosen_dir);
                    editor.apply();
                break;
            default:
                break;
        }

    }

    private String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } catch (Exception e) {
            Log.e("ERROR", "getRealPathFromURI Exception : " + e.toString());
            return "";
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void changeDefaultSaveDir() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.DIR_SELECT;
        //properties.root = new File(DialogConfigs.DEFAULT_DIR);
        properties.root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        properties.error_dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        properties.offset = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        properties.extensions = null;

        FilePickerDialog dialog = new FilePickerDialog(mainActivity,properties);
        dialog.setTitle("Select a Directory");

        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                String fullpath = "";
                for(int i = 0; i < files.length; i++){
                    fullpath = fullpath + files[i];
                }
                Log.d("FilePicker", fullpath);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(FtpFragment.LOCALDIR_KEY, fullpath);
                editor.apply();
            }
        });

        dialog.show();

        /*
        Log.d("DefDir", Environment.getExternalStorageDirectory().toString());
        Toast.makeText(mainActivity, "Choose default save directory", Toast.LENGTH_LONG).show();

        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        startActivityForResult(Intent.createChooser(i, "Choose default save directory"), REQUEST_DIRECTORY);
        /*
        Intent chooserIntent = new Intent(mainActivity, DirectoryChooserActivity.class);
        chooserIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                .newDirectoryName("SaveDirectory")
                .initialDirectory(Environment.getExternalStorageDirectory().toString())
                .allowReadOnlyDirectory(true)
                .allowNewDirectoryNameModification(true)
                .build();

        chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_CONFIG, config);

        startActivityForResult(chooserIntent, REQUEST_DIRECTORY);
        */
    }
/*
    public void resetFtpDefaults(){
        WifiManager wifiManager = (WifiManager) mainActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();

        String gatewayIP = formatIP(dhcpInfo.gateway);
        ftpIp.setText(gatewayIP);

        ftpUsername.setText("pi");
        ftpPassword.setText("sosarpi");
        ftpPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
    }

    public void requestFtp() {

        if(localDir == null) {
            Toast.makeText(mainActivity, "Set a default save directory first", Toast.LENGTH_LONG).show();
        }
        else {
            getImageFromFtp(localDir);
        }
    }
    /*
    public void getImageFromFtp(String dir) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        Uri ftpUri = Uri.parse("ftp://" + ftpIp.getText().toString());
        intent.setDataAndType(ftpUri, "vnd.android.cursor.dir/lysesoft.andftp.uri");
        intent.putExtra("command_type", "download");
        intent.putExtra("ftp_username", ftpUsername.getText().toString());
        intent.putExtra("ftp_password", ftpPassword.getText().toString());
        intent.putExtra("ftp_overwrite","skip");
        intent.putExtra("progress_title", "Downloading picture...");
        intent.putExtra("remote_file1", "/files/test.jpg");
        intent.putExtra("local_folder", dir);
        intent.putExtra("close_ui", "true");
        intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(intent, DOWNLOAD_FILES_REQUEST);
    }

    private String formatIP(int ip) {
        return String.format(
                "%d.%d.%d.%d",
                (ip & 0xff),
                (ip >> 8 & 0xff),
                (ip >> 16 & 0xff),
                (ip >> 24 & 0xff)
        );
    }
*/
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.ftp_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if(id == R.id.changeDefaultDir) {
            changeDefaultSaveDir();
        }

        return super.onOptionsItemSelected(item);
    }

}