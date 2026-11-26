package com.example.valnightmarket;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private static final String VP_UUID = "85ad13f7-3d1b-5128-9eb2-7cd8ee0b5741";

    private static class OfferItem {
        String name;
        String tier;
        int color;
        int vp;
        boolean revealed = false;

        OfferItem(String name, String tier, int color, int vp) {
            this.name = name;
            this.tier = tier;
            this.color = color;
            this.vp = vp;
        }
    }

    private TextView tvTitle, tvTimer, tvStatus;
    private final FrameLayout[] cards = new FrameLayout[4];
    private final TextView[] cardTexts = new TextView[4];
    private final List<OfferItem> offers = new ArrayList<>();

    private boolean demoMode = true;
    private String shard = "kr";
    private String puuid = "";
    private String accessToken = "";
    private String entitlementsToken = "";
    private int remainingSec = 14 * 3600 + 25 * 60 + 10;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (remainingSec > 0) remainingSec--;
            int h = remainingSec / 3600;
            int m = (remainingSec % 3600) / 60;
            int s = remainingSec % 60;
            tvTimer.setText(String.format(Locale.getDefault(), "상점 초기화까지 %02d:%02d:%02d", h, m, s));
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTitle = findViewById(R.id.tvTitle);
        tvTimer = findViewById(R.id.tvTimer);
        tvStatus = findViewById(R.id.tvStatus);

        int[] cardIds = {R.id.card0, R.id.card1, R.id.card2, R.id.card3};
        int[] txtIds = {R.id.txtCard0, R.id.txtCard1, R.id.txtCard2, R.id.txtCard3};

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            cards[i] = findViewById(cardIds[i]);
            cardTexts[i] = findViewById(txtIds[i]);
            cards[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (idx < offers.size()) {
                        OfferItem item = offers.get(idx);
                        item.revealed = !item.revealed;
                        renderCard(idx, item);
                    }
                }
            });
        }

        Button btnFlipAll = findViewById(R.id.btnFlipAll);
        btnFlipAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < offers.size(); i++) {
                    offers.get(i).revealed = true;
                    renderCard(i, offers.get(i));
                }
            }
        });

        Button btnConfig = findViewById(R.id.btnConfig);
        btnConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showConfigDialog();
            }
        });

        loadDemoOffers();
        handler.post(timerRunnable);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(timerRunnable);
        super.onDestroy();
    }

    private void loadDemoOffers() {
        demoMode = true;
        tvTitle.setText("오늘의 상점 (데모)");
        tvStatus.setText("데모 모드입니다. [연동]을 눌러 토큰을 입력하면 실제 상점을 불러옵니다.");
        offers.clear();
        offers.add(new OfferItem("프라임 밴달", "Premium", Color.parseColor("#D1548D"), 1775));
        offers.add(new OfferItem("레버넌트 팬텀", "Exclusive", Color.parseColor("#F5955B"), 2175));
        offers.add(new OfferItem("알고리즘 오퍼레이터", "Ultra", Color.parseColor("#FAD663"), 2475));
        offers.add(new OfferItem("정찰 셰리프", "Deluxe", Color.parseColor("#009587"), 1275));
        renderAllCards();
    }

    private void renderAllCards() {
        for (int i = 0; i < 4; i++) {
            if (i < offers.size()) {
                renderCard(i, offers.get(i));
            } else {
                cardTexts[i].setText("EMPTY");
            }
        }
    }

    private void renderCard(int index, OfferItem item) {
        if (!item.revealed) {
            cards[index].setBackgroundColor(Color.parseColor("#16222F"));
            cardTexts[index].setTextColor(Color.parseColor("#FAD663"));
            cardTexts[index].setText("◆\nNIGHT OFFER\n탭하여 공개");
        } else {
            cards[index].setBackgroundColor(Color.parseColor("#1E2F40"));
            cardTexts[index].setTextColor(Color.parseColor("#ECE8E1"));
            cardTexts[index].setText(
                    "[" + item.tier.toUpperCase(Locale.ROOT) + "]\n\n" +
                    item.name + "\n\n" +
                    item.vp + " VP"
            );
        }
    }

    private void showConfigDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        final CheckBox cbDemo = new CheckBox(this);
        cbDemo.setText("데모 모드 사용");
        cbDemo.setChecked(demoMode);
        layout.addView(cbDemo);

        final EditText etShard = new EditText(this);
        etShard.setHint("샤드 (kr, ap, na, eu)");
        etShard.setText(shard);
        layout.addView(etShard);

        final EditText etPuuid = new EditText(this);
        etPuuid.setHint("PUUID");
        etPuuid.setText(puuid);
        layout.addView(etPuuid);

        final EditText etAccess = new EditText(this);
        etAccess.setHint("Access Token");
        etAccess.setText(accessToken);
        layout.addView(etAccess);

        final EditText etEnt = new EditText(this);
        etEnt.setHint("Entitlements Token");
        etEnt.setText(entitlementsToken);
        layout.addView(etEnt);

        new AlertDialog.Builder(this)
                .setTitle("상점 연동 설정")
                .setView(layout)
                .setPositiveButton("적용", (dialog, which) -> {
                    shard = etShard.getText().toString().trim().isEmpty() ? "kr" : etShard.getText().toString().trim();
                    puuid = etPuuid.getText().toString().trim();
                    accessToken = etAccess.getText().toString().trim();
                    entitlementsToken = etEnt.getText().toString().trim();
                    if (cbDemo.isChecked()) {
                        loadDemoOffers();
                    } else {
                        fetchRealStore();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void fetchRealStore() {
        if (puuid.isEmpty() || accessToken.isEmpty() || entitlementsToken.isEmpty()) {
            tvStatus.setText("PUUID, Access Token, Entitlements Token을 모두 입력하세요.");
            return;
        }
        demoMode = false;
        tvTitle.setText("오늘의 상점 (" + shard.toUpperCase(Locale.ROOT) + ")");
        tvStatus.setText("상점 정보를 불러오는 중...");

        new Thread(() -> {
            try {
                String clientVersion = getClientVersion();
                Map<String, String> skinNames = getKoreanSkinMap();

                URL url = new URL("https://pd." + shard + ".a.pvp.net/store/v3/storefront/" + puuid);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setRequestProperty("X-Riot-Entitlements-JWT", entitlementsToken);
                conn.setRequestProperty("X-Riot-ClientPlatform",
                        "ew0KCSJwbGF0Zm9ybVR5cGUiOiAiUEMiLA0KCSJwbGF0Zm9ybU9TIjogIldpbmRvd3MiLA0KCSJwbGF0Zm9ybU9TVmVyc2lvbiI6ICIxMC4wLjE5MDQyLjEuMjU2LjY0Yml0IiwNCgkicGxhdGZvcm1DaGlwc2V0IjogIlVua25vd24iDQp9");
                conn.setRequestProperty("X-Riot-ClientVersion", clientVersion);
                conn.setRequestProperty("Content-Type", "application/json");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write("{}".getBytes("UTF-8"));
                }

                if (conn.getResponseCode() != 200) {
                    throw new Exception("API 응답 오류: HTTP " + conn.getResponseCode());
                }

                String body = readStream(conn);
                JSONObject root = new JSONObject(body);
                JSONObject panel = root.getJSONObject("SkinsPanelLayout");
                remainingSec = panel.optInt("SingleItemOffersRemainingDurationInSeconds", remainingSec);
                JSONArray arr = panel.getJSONArray("SingleItemStoreOffers");

                final List<OfferItem> parsed = new ArrayList<>();
                for (int i = 0; i < Math.min(4, arr.length()); i++) {
                    JSONObject offer = arr.getJSONObject(i);
                    String offerId = offer.optString("OfferID", "");
                    JSONObject cost = offer.optJSONObject("Cost");
                    int vp = cost != null ? cost.optInt(VP_UUID, 0) : 0;
                    String name = skinNames.containsKey(offerId) ? skinNames.get(offerId) : "스킨 (" + offerId.substring(0, Math.min(6, offerId.length())) + ")";
                    parsed.add(new OfferItem(name, "Store Offer", Color.parseColor("#FF4655"), vp));
                }

                handler.post(() -> {
                    offers.clear();
                    offers.addAll(parsed);
                    renderAllCards();
                    tvStatus.setText("실제 상점 불러오기 완료. 카드를 탭하세요.");
                });
            } catch (final Exception e) {
                handler.post(() -> tvStatus.setText("연동 실패: " + e.getMessage()));
            }
        }).start();
    }

    private String getClientVersion() throws Exception {
        URL url = new URL("https://valorant-api.com/v1/version");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        String json = readStream(conn);
        return new JSONObject(json).getJSONObject("data").getString("riotClientVersion");
    }

    private Map<String, String> getKoreanSkinMap() throws Exception {
        Map<String, String> map = new HashMap<>();
        URL url = new URL("https://valorant-api.com/v1/weapons/skins?language=ko-KR");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        String json = readStream(conn);
        JSONArray skins = new JSONObject(json).getJSONArray("data");
        for (int i = 0; i < skins.length(); i++) {
            JSONObject skin = skins.getJSONObject(i);
            String displayName = skin.optString("displayName", "스킨");
            JSONArray levels = skin.optJSONArray("levels");
            if (levels != null) {
                for (int j = 0; j < levels.length(); j++) {
                    String uuid = levels.getJSONObject(j).optString("uuid", "");
                    if (!uuid.isEmpty()) {
                        map.put(uuid, displayName);
                    }
                }
            }
        }
        return map;
    }

    private String readStream(HttpURLConnection conn) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }
}

