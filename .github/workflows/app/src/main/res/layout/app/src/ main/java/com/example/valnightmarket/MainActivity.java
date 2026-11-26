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
        etAccess.setText(prefs.getString("access_token", ""));
        container.addView(etAccess);

        EditText etEntitlements = new EditText(this);
        etEntitlements.setHint("Entitlements Token");
        etEntitlements.setText(prefs.getString("entitlements_token", ""));
        container.addView(etEntitlements);

        new AlertDialog.Builder(this)
                .setTitle("Riot 계정 연동")
                .setView(container)
                .setPositiveButton("저장 및 조회", (dialog, which) -> {
                    prefs.edit()
                            .putString("shard", etShard.getText().toString().trim().toLowerCase(Locale.ROOT))
                            .putString("puuid", etPuuid.getText().toString().trim())
                            .putString("access_token", etAccess.getText().toString().trim().replaceFirst("^Bearer\\s+", ""))
                            .putString("entitlements_token", etEntitlements.getText().toString().trim())
                            .apply();
                    fetchStoreData();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void loadSampleOrSavedData() {
        dailyOffers.clear();
        dailyOffers.add(new StoreOffer("sample-1", 1775, 0));
        dailyOffers.add(new StoreOffer("sample-2", 2175, 0));
        dailyOffers.add(new StoreOffer("sample-3", 1275, 0));
        dailyOffers.add(new StoreOffer("sample-4", 875, 0));
        dailyOffers.get(0).skinName = "프라임 밴달";
        dailyOffers.get(1).skinName = "리버 팬텀";
        dailyOffers.get(2).skinName = "오니 오퍼레이터";
        dailyOffers.get(3).skinName = "정찰 고스트";

        nightMarketOffers.clear();
        nightMarketOffers.add(new StoreOffer("nm-1", 1775, 35));
        nightMarketOffers.add(new StoreOffer("nm-2", 1275, 28));
        nightMarketOffers.add(new StoreOffer("nm-3", 875, 42));
        nightMarketOffers.add(new StoreOffer("nm-4", 1775, 22));
        nightMarketOffers.get(0).skinName = "알고리즘 밴달";
        nightMarketOffers.get(1).skinName = "메이지펑크 셰리프";
        nightMarketOffers.get(2).skinName = "아발란체 클래식";
        nightMarketOffers.get(3).skinName = "스펙트럼 가디언";

        renderCards(isNightMarketMode ? nightMarketOffers : dailyOffers);
        startTimer(3600_000L);
    }

    private void fetchStoreData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String shard = prefs.getString("shard", "kr");
        String puuid = prefs.getString("puuid", "");
        String accessToken = prefs.getString("access_token", "");
        String entitlements = prefs.getString("entitlements_token", "");

        if (puuid.isEmpty() || accessToken.isEmpty() || entitlements.isEmpty()) {
            return;
        }

        executor.execute(() -> {
            try {
                Map<String, SkinMeta> skinMap = fetchSkinLevelMap();
                String endpoint = "https://pd." + shard + ".a.pvp.net/store/v2/storefront/" + puuid;
                HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setRequestProperty("X-Riot-Entitlements-JWT", entitlements);
                conn.setRequestProperty("X-Riot-ClientPlatform", CLIENT_PLATFORM);
                conn.setRequestProperty("X-Riot-ClientVersion", CLIENT_VERSION);

                if (conn.getResponseCode() != 200) {
                    runOnUiThread(() -> Toast.makeText(this, "스토어 응답 코드: " + conn.getResponseCode(), Toast.LENGTH_SHORT).show());
                    return;
                }

                String body = readStream(conn);
                JSONObject root = new JSONObject(body);

                List<StoreOffer> parsedDaily = new ArrayList<>();
                JSONObject skinsPanel = root.optJSONObject("SkinsPanelLayout");
                if (skinsPanel != null) {
                    JSONArray singleOffers = skinsPanel.optJSONArray("SingleItemStoreOffers");
                    long remainSec = skinsPanel.optLong("SingleItemOffersRemainingDurationInSeconds", 3600L);
                    runOnUiThread(() -> startTimer(remainSec * 1000L));

                    if (singleOffers != null) {
                        for (int i = 0; i < Math.min(4, singleOffers.length()); i++) {
                            JSONObject obj = singleOffers.getJSONObject(i);
                            String offerId = obj.optString("OfferID", "");
                            int cost = extractVpCost(obj.optJSONObject("Cost"));
                            StoreOffer offer = new StoreOffer(offerId, cost, 0);
                            applySkinMeta(offer, skinMap.get(offerId));
                            parsedDaily.add(offer);
                        }
                    }
                }

                List<StoreOffer> parsedNight = new ArrayList<>();
                JSONObject bonusStore = root.optJSONObject("BonusStore");
                if (bonusStore != null) {
                    JSONArray bonusOffers = bonusStore.optJSONArray("BonusStoreOffers");
                    if (bonusOffers != null) {
                        for (int i = 0; i < Math.min(4, bonusOffers.length()); i++) {
                            JSONObject item = bonusOffers.getJSONObject(i);
                            JSONObject offerObj = item.optJSONObject("Offer");
                            int discount = item.optInt("DiscountPercent", 0);
                            String offerId = offerObj != null ? offerObj.optString("OfferID", "") : "";
                            int baseCost = offerObj != null ? extractVpCost(offerObj.optJSONObject("Cost")) : 0;
                            int discountedCost = Math.round(baseCost * (100 - discount) / 100.0f);
                            StoreOffer offer = new StoreOffer(offerId, discountedCost, discount);
                            applySkinMeta(offer, skinMap.get(offerId));
                            parsedNight.add(offer);
                        }
                    }
                }

                runOnUiThread(() -> {
                    if (!parsedDaily.isEmpty()) {
                        dailyOffers.clear();
                        dailyOffers.addAll(parsedDaily);
                    }
                    if (!parsedNight.isEmpty()) {
                        nightMarketOffers.clear();
                        nightMarketOffers.addAll(parsedNight);
                    }
                    renderCards(isNightMarketMode ? nightMarketOffers : dailyOffers);
                    Toast.makeText(this, "상점 정보를 갱신했습니다.", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "조회 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void applySkinMeta(StoreOffer offer, SkinMeta meta) {
        if (meta != null) {
            if (!meta.displayName.isEmpty() && !"null".equals(meta.displayName)) {
                offer.skinName = meta.displayName;
            }
            if (!meta.displayIcon.isEmpty() && !"null".equals(meta.displayIcon)) {
                offer.iconUrl = meta.displayIcon;
            }
        } else {
            offer.skinName = offer.offerId;
        }
    }

    private int extractVpCost(JSONObject costObj) {
        if (costObj == null) return 0;
        String vpUuid = "85ad13f7-3d1b-5128-9eb2-7cd8ee0b5741";
        return costObj.optInt(vpUuid, 0);
    }

    private Map<String, SkinMeta> fetchSkinLevelMap() {
        Map<String, SkinMeta> map = new HashMap<>();
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("https://valorant-api.com/v1/weapons/skinlevels?language=ko-KR").openConnection();
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() == 200) {
                JSONArray arr = new JSONObject(readStream(conn)).optJSONArray("data");
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject item = arr.getJSONObject(i);
                        String uuid = item.optString("uuid", "");
                        String name = item.optString("displayName", "");
                        String icon = item.optString("displayIcon", "");
                        if (!uuid.isEmpty()) {
                            map.put(uuid, new SkinMeta(name, icon));
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return map;
    }

    private void loadSkinImage(String urlStr, ImageView targetView) {
        if (urlStr == null || urlStr.isEmpty() || "null".equals(urlStr)) {
            targetView.setVisibility(View.GONE);
            return;
        }

        Bitmap cached = bitmapCache.get(urlStr);
        if (cached != null) {
            targetView.setImageBitmap(cached);
            targetView.setVisibility(View.VISIBLE);
            return;
        }

        targetView.setVisibility(View.GONE);
        executor.execute(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setDoInput(true);
                conn.connect();
                try (InputStream in = conn.getInputStream()) {
                    Bitmap bmp = BitmapFactory.decodeStream(in);
                    if (bmp != null) {
                        bitmapCache.put(urlStr, bmp);
                        runOnUiThread(() -> {
                            if (urlStr.equals(targetView.getTag())) {
                                targetView.setImageBitmap(bmp);
                                targetView.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }
            } catch (Exception ignored) {
            }
        });
    }

    private String readStream(HttpURLConnection conn) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private void renderCards(List<StoreOffer> offers) {
        gridCards.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int cardWidth = (getResources().getDisplayMetrics().widthPixels - (int) (44 * density)) / 2;
        int cardHeight = (int) (185 * density);
        int margin = (int) (6 * density);

        for (StoreOffer offer : offers) {
            CardView card = new CardView(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = cardWidth;
            params.height = cardHeight;
            params.setMargins(margin, margin, margin, margin);
            card.setLayoutParams(params);
            card.setRadius(12 * density);
            card.setCardBackgroundColor(offer.flipped ? 0xFF1F2933 : 0xFFFF4655);
            card.setCardElevation(4 * density);

            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setGravity(Gravity.CENTER);
            int pad = (int) (10 * density);
            content.setPadding(pad, pad, pad, pad);

            ImageView ivSkin = new ImageView(this);
            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (int) (68 * density)
            );
            imgParams.bottomMargin = (int) (6 * density);
            ivSkin.setLayoutParams(imgParams);
            ivSkin.setScaleType(ImageView.ScaleType.FIT_CENTER);

            TextView tvCard = new TextView(this);
            tvCard.setGravity(Gravity.CENTER);
            tvCard.setTextColor(0xFFFFFFFF);
            tvCard.setTextSize(14f);

            content.addView(ivSkin);
            content.addView(tvCard);
            card.addView(content);

            updateCardContent(ivSkin, tvCard, offer);
            card.setOnClickListener(v -> flipCard(card, ivSkin, tvCard, offer));
            gridCards.addView(card);
        }
    }

    private void flipCard(CardView card, ImageView ivSkin, TextView tvCard, StoreOffer offer) {
        offer.flipped = !offer.flipped;
        card.animate()
                .rotationY(90f)
                .setDuration(150)
                .withEndAction(() -> {
                    card.setCardBackgroundColor(offer.flipped ? 0xFF1F2933 : 0xFFFF4655);
                    updateCardContent(ivSkin, tvCard, offer);
                    card.setRotationY(-90f);
                    card.animate().rotationY(0f).setDuration(150).start();
                })
                .start();
    }

    private void updateCardContent(ImageView ivSkin, TextView tvCard, StoreOffer offer) {
        if (!offer.flipped) {
            ivSkin.setTag(null);
            ivSkin.setVisibility(View.GONE);
            tvCard.setText("탭해서 공개\n" + (offer.discountPercent > 0 ? "-" + offer.discountPercent + "% 할인" : "DAILY OFFER"));
        } else {
            ivSkin.setTag(offer.iconUrl);
            loadSkinImage(offer.iconUrl, ivSkin);
            String discountText = offer.discountPercent > 0 ? "\n(-" + offer.discountPercent + "%)" : "";
            tvCard.setText(offer.skinName + "\n" + offer.costVp + " VP" + discountText);
        }
    }

    private void startTimer(long millisInFuture) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        countDownTimer = new CountDownTimer(millisInFuture, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long totalSec = millisUntilFinished / 1000L;
                long h = totalSec / 3600L;
                long m = (totalSec % 3600L) / 60L;
                long s = totalSec % 60L;
                tvTimer.setText(String.format(Locale.getDefault(), "갱신까지: %02d:%02d:%02d", h, m, s));
            }

            @Override
            public void onFinish() {
                tvTimer.setText("상점 갱신 시간이 지났습니다");
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        executor.shutdownNow();
    }
}
