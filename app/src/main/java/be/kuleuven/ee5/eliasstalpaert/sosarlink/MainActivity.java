package be.kuleuven.ee5.eliasstalpaert.sosarlink;

import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.math.BigInteger;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {

    Button ftpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ftpButton = findViewById(R.id.connectFtpButton);
        ftpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectToFtp();
            }
        });
    }

    public void connectToFtp() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();

        String ftpHost = formatIP(dhcpInfo.gateway);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("ftp://" + ftpHost));
        intent.putExtra("ftp_username","stalpaard");
        intent.putExtra("ftp_password","7426985");
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
