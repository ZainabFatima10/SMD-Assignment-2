package com.zainabfatima.i221064;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

/** Screen 3 - Create account (sign up). */
public class CreateAccountActivity extends AppCompatActivity {

    private boolean agreed = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_account_screen);

        NavHelper.click(this, R.id.backbutton, this::finish);
        NavHelper.click(this, R.id.loginLink, this::finish);   // "Already have an account? Log in"

        // Terms checkbox: dim it when unchecked
        final View checkbox = findViewById(R.id.checkbox);
        NavHelper.click(this, R.id.checkbox, () -> {
            agreed = !agreed;
            checkbox.setAlpha(agreed ? 1f : 0.35f);
        });

        NavHelper.click(this, R.id.button2, () -> {
            if (!agreed) {
                NavHelper.toast(this, "Please agree to the Terms and Privacy Policy");
                return;
            }
            Intent i = new Intent(this, StartSocialMediaActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });

        NavHelper.click(this, R.id.FemaleChoice, () -> NavHelper.toast(this, "Female"));
        NavHelper.click(this, R.id.MaleChoice, () -> NavHelper.toast(this, "Male"));
        NavHelper.click(this, R.id.CustomChoice, () -> NavHelper.toast(this, "Custom"));
        NavHelper.click(this, R.id.DayView3RL, () -> NavHelper.toast(this, "Day"));
        NavHelper.click(this, R.id.MonthView3RL, () -> NavHelper.toast(this, "Month"));
        NavHelper.click(this, R.id.YearView3RL, () -> NavHelper.toast(this, "Year"));
    }
}
