package be.kuleuven.ee5.eliasstalpaert.sosarlink;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Splash screen will be visible until the MainActivity is finished with loading
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

        finish();
    }
}
