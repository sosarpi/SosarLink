package be.kuleuven.ee5.eliasstalpaert.sosarlink;

import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class FtpFragment extends Fragment {

    private EditText ftpIp, ftpUsername, ftpPassword;
    private Button ftpButton;
    private View fragmentView;


    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getActivity().setTitle("FTP");
    }


    private void setupViews() {
        ftpIp = fragmentView.findViewById(R.id.ftpIp);
        ftpButton = fragmentView.findViewById(R.id.ftpButton);
        ftpUsername = fragmentView.findViewById(R.id.usernameFtp);
        ftpPassword = fragmentView.findViewById(R.id.passwordFtp);
        ftpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectToFtp();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        fragmentView = inflater.inflate(R.layout.fragment_ftp, container, false);
        setupViews();

        WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();

        String gatewayIP = formatIP(dhcpInfo.gateway);
        ftpIp.setText(gatewayIP);

        ftpUsername.setText("pi");
        ftpPassword.setText("sosarpi");
        ftpPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
        return fragmentView;
    }

    public void connectToFtp() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        String link = "ftp://" + ftpIp.getText().toString();
        intent.setData(Uri.parse(link));
        intent.putExtra("ftp_username",ftpUsername.getText().toString());
        intent.putExtra("ftp_password",ftpPassword.getText().toString());
        startActivity(intent);
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
}
