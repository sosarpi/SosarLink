package be.kuleuven.ee5.eliasstalpaert.sosarlink;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.nfc.Tag;
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


public class FtpFragment extends Fragment {

    private static final String TAG = FtpFragment.class.getSimpleName();
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
                Toast.makeText(mainActivity,"FTP Settings updated",Toast.LENGTH_SHORT).show();
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
            PassesFragment passesFragment = new PassesFragment();

            mainActivity.getmNavigationView().setCheckedItem(R.id.nav_passes);
            mainActivity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    passesFragment).commit();
        }
        else{
            ftpIp.setText(ftpip);
        }
    }

    public void setDefaultIP() {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mainActivity).edit();
        String gatewayIp = PassesFragment.getGatewayIp(mainActivity);
        editor.putString(FtpFragment.FTPIP_KEY, gatewayIp);
        Toast.makeText(mainActivity, "IP set to: " + gatewayIp,Toast.LENGTH_SHORT).show();
        editor.apply();
        ftpIp.setText(gatewayIp);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.ftp_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id) {
            case R.id.setDefaultIp :
                setDefaultIP();
                break;
                default:
                    break;
        }

        return super.onOptionsItemSelected(item);
    }

}