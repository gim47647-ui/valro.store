package com.example.valnightmarket;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "val_store_prefs";
    private static final String CLIENT_PLATFORM = "ew0KCSJwbGF0Zm9ybVR5cGUiOiAiUEMiLA0KCSJwbGF0Zm9ybU9TIjogIldpbmRvd3MiLA0KCSJwbGF0Zm9ybU9TVmVyc2lvbiI6ICIxMC4wLjE5MDQyLjEuMjU2LjY0Yml0IiwNCgkicGxhdGZvcm1DaGlwc2V0IjogIlVua25vd24iDQp9";
    private static final String CLIENT_VERSION = "release-09.00-shipping-14-2620400";

    private GridLayout gridCards;
    private TextView tvTimer;
    private Button btnDaily;
    private Button btnNightMarket;

    private boolean isNightMarketMode = false;
    private final List<StoreOffer> dailyOffers = new ArrayList<>();
    private final List<StoreOffer> nightMarketOffers = new ArrayList<>();
    private final Map<String, Bitmap> bitmapCache = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private CountDownTimer countDownTimer;

    private static class SkinMeta {
        final String displayName;
        final String displayIcon;

        SkinMeta(String displayName, String displayIcon) {
            this.displayName = displayName;
            this.displayIcon = displayIcon;
        }
    }

    private static class StoreOffer {
        final String offerId;
        final int costVp;
        final int discountPercent;
        String skinName = "스킨 정보 불러오는 중";
        String iconUrl = "";
        boolean flipped = false;

        StoreOffer(String offerId, int costVp, int discountPercent) {
            this.offerId = offerId;
            this.costVp = costVp;
            this.discountPercent = discountPercent;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridCards = findViewById(R.id.gridCards);
        tvTimer = findViewById(R.id.tvTimer);
        btnDaily = findViewById(R.id.btnDaily);
        btnNightMarket = findViewById(R.id.btnNightMarket);
        Button btnAuth = findViewById(R.id.btnAuth);

        btnAuth.setOnClickListener(v -> showAuthDialog());

        btnDaily.setOnClickListener(v -> {
            isNightMarketMode = false;
            updateTabButtons();
            renderCards(dailyOffers);
        });

        btnNightMarket.setOnClickListener(v -> {
            isNightMarketMode = true;
            updateTabButtons();
            renderCards(nightMarketOffers);
        });

        loadSampleOrSavedData();
        fetchStoreData();
    }

    private void updateTabButtons() {
        btnDaily.setBackgroundColor(isNightMarketMode ? 0xFF1F2933 : 0xFFFF4655);
        btnNightMarket.setBackgroundColor(isNightMarketMode ? 0xFFFF4655 : 0xFF1F2933);
    }

    private void showAuthDialog() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        EditText etShard = new EditText(this);
        etShard.setHint("Shard (예: kr, ap, na, eu)");
        etShard.setText(prefs.getString("shard", "kr"));
        container.addView(etShard);

        EditText etPuuid = new EditText(this);
        etPuuid.setHint("PUUID");
        etPuuid.setText(prefs.getString("puuid", ""));
        container.addView(etPuuid);

        EditText etAccess = new EditText(this);
        etAccess.setHint("Access Token (Bearer 제외 가능)");
        etAccess.set
