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


public class SettingsFragment extends Fragment {

    public static final String IP_KEY = "serverIp";
    public static final String FTPUSER_KEY = "ftpUser";
    public static final String FTPPASS_KEY = "ftpPass";

    private EditText ipEdit, userEdit, passEdit;
    private Button submitButton;
    private View fragmentView;

    private MainActivity mainActivity;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPreferencesEditor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainActivity = (MainActivity) getActivity();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity);
        sharedPreferencesEditor = sharedPreferences.edit();

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_settings, container, false);
        ipEdit = fragmentView.findViewById(R.id.ftpIp);
        submitButton = fragmentView.findViewById(R.id.submitButton);
        userEdit = fragmentView.findViewById(R.id.usernameFtp);
        passEdit = fragmentView.findViewById(R.id.passwordFtp);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateFtpPreferences();
            }
        });

        setupSettingsUI();

        return fragmentView;
    }

    private void setupSettingsUI() {
        this.setHasOptionsMenu(true);
        mainActivity.setTitle("Settings");
        mainActivity.getNavigationView().setCheckedItem(R.id.nav_ftp);
        passEdit.setTransformationMethod(PasswordTransformationMethod.getInstance());

        refreshUIData();
    }

    private void refreshUIData(){
        String username = sharedPreferences.getString(SettingsFragment.FTPUSER_KEY, null);
        String password = sharedPreferences.getString(SettingsFragment.FTPPASS_KEY, null);
        String server_ip = sharedPreferences.getString(SettingsFragment.IP_KEY, null);
        if (username != null && password != null) {
            userEdit.setText(username);
            passEdit.setText(password);
        } else {
            userEdit.setText("");
            passEdit.setText("");
        }
        ipEdit.setText(server_ip); //server_ip != null
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.settings_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.setDefaultIp) setDefaultIP();

        return super.onOptionsItemSelected(item);
    }

    private void setDefaultIP() {
        String gatewayIp = mainActivity.getGatewayIp();
        sharedPreferencesEditor.putString(SettingsFragment.IP_KEY, gatewayIp);
        Toast.makeText(mainActivity, "IP set to: " + gatewayIp, Toast.LENGTH_SHORT).show();
        sharedPreferencesEditor.apply();
        ipEdit.setText(gatewayIp);
    }

    private void updateFtpPreferences() {
        String username = userEdit.getText().toString();
        if (!mainActivity.hasAlphanumeric(username)) username = null;
        String password = passEdit.getText().toString();
        if (!mainActivity.hasAlphanumeric(password)) password = null;
        String ip_address = ipEdit.getText().toString();
        if (mainActivity.isAnIpv4Address(ip_address)){
            sharedPreferencesEditor.putString(FTPUSER_KEY, username);
            sharedPreferencesEditor.putString(FTPPASS_KEY, password);
            sharedPreferencesEditor.putString(IP_KEY, ip_address);
            sharedPreferencesEditor.apply();
            Toast.makeText(mainActivity, "Settings updated", Toast.LENGTH_SHORT).show();
        }
        else Toast.makeText(mainActivity, "Failed: Enter a valid IP to save settings", Toast.LENGTH_LONG).show();
    }

}