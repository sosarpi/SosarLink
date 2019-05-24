package be.kuleuven.ee5.eliasstalpaert.sosarlink;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class FtpFragment extends Fragment {

    public static final String FTPIP_KEY = "ftpIp";
    public static final String FTPUSER_KEY = "ftpUser";
    public static final String FTPPASS_KEY = "ftpPass";

    private EditText ftpIp, ftpUsername, ftpPassword;
    private Button submitButton;
    private View fragmentView;
    private MainActivity mainActivity;

    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainActivity = (MainActivity) getActivity();
        mainActivity.setTitle("FTP");
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity);

    }

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

            mainActivity.getNavigationView().setCheckedItem(R.id.nav_passes);
            mainActivity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    passesFragment).commit();
        }
        else{
            ftpIp.setText(ftpip);
        }
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

    private boolean hasAlphanumeric(String s){
        return s.matches(".*\\w.*");
    }

    private void setDefaultIP() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String gatewayIp = PassesFragment.getGatewayIp(mainActivity);
        editor.putString(FtpFragment.FTPIP_KEY, gatewayIp);
        Toast.makeText(mainActivity, "IP set to: " + gatewayIp,Toast.LENGTH_SHORT).show();
        editor.apply();
        ftpIp.setText(gatewayIp);
    }

    private void updateFtpSettings(){
        String username = ftpUsername.getText().toString();
        if(!hasAlphanumeric(username)) username = null;
        String password = ftpPassword.getText().toString();
        if(!hasAlphanumeric(password)) password = null;
        String ip_address = ftpIp.getText().toString();
        if(!hasAlphanumeric(ip_address)) ip_address = null;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(FTPUSER_KEY, username);
        editor.putString(FTPPASS_KEY, password);
        editor.putString(FTPIP_KEY, ip_address);
        editor.apply();
    }

}