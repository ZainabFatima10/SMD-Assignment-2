package com.zainabfatima.i221064;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

/** Screen 23 - Marketplace (Categories opens the DrawerLayout). All content is declared in XML. */
public class MarketplaceActivity extends AppCompatActivity {

    private DrawerLayout drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marketplace);
        NavHelper.setup(this, NavHelper.MARKET, "Marketplace");

        drawer = findViewById(R.id.drawer);

        findViewById(R.id.btnCategories).setOnClickListener(v -> drawer.openDrawer(Gravity.START));
        findViewById(R.id.btnSell).setOnClickListener(v -> NavHelper.toast(this, "Create a listing"));
        findViewById(R.id.btnLocation).setOnClickListener(v -> NavHelper.toast(this, "Karachi · 40 km"));

        int[] products = {R.id.p1, R.id.p2, R.id.p3, R.id.p4};
        int[] prices = {R.id.p1_price, R.id.p2_price, R.id.p3_price, R.id.p4_price};
        int[] titles = {R.id.p1_title, R.id.p2_title, R.id.p3_title, R.id.p4_title};
        for (int i = 0; i < products.length; i++) {
            final String msg = ((TextView) findViewById(titles[i])).getText() + " · "
                    + ((TextView) findViewById(prices[i])).getText();
            findViewById(products[i]).setOnClickListener(v -> NavHelper.toast(this, msg));
        }

        int[] cats = {R.id.cat1, R.id.cat2, R.id.cat3, R.id.cat4, R.id.cat5, R.id.cat6, R.id.cat7, R.id.cat8};
        for (int id : cats) {
            final TextView t = findViewById(id);
            t.setOnClickListener(v -> {
                drawer.closeDrawer(Gravity.START);
                NavHelper.toast(this, t.getText().toString());
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(Gravity.START)) drawer.closeDrawer(Gravity.START);
        else super.onBackPressed();
    }
}
