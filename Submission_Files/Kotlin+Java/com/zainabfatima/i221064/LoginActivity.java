package com.zainabfatima.i221064;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/** Screen 2 - Log in. */
public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_screen);

        // "Log in" button and the recent-login card both enter the app
        NavHelper.click(this, R.id.button, this::openHome);
        NavHelper.click(this, R.id.cardView2, this::openHome);

        NavHelper.click(this, R.id.button2, () ->
                startActivity(new Intent(this, CreateAccountActivity.class)));

        NavHelper.click(this, R.id.text4, () -> NavHelper.toast(this, "Forgot password"));
        NavHelper.click(this, R.id.cardView3, () -> NavHelper.toast(this, "Email or mobile number"));
        NavHelper.click(this, R.id.cardView4, () -> NavHelper.toast(this, "Password"));
    }

    private void openHome() {
        startActivity(new Intent(this, StartSocialMediaActivity.class));
        finish();
    }
}
