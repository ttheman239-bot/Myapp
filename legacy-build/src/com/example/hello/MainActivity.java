package com.example.hello;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Lightweight (no-Compose, no-Gradle) build of the research app. Loads
 * research.json from assets and renders a simple ranked list of pairs.
 * Built with aapt + dx inside the sandbox, targets API 23.
 */
public class MainActivity extends Activity {

    private static final int PRIMARY = 0xFF6650A4;
    private static final int PRIMARY_DARK = 0xFF4B3592;
    private static final int SURFACE = 0xFFFAF8FF;
    private static final int ON_SURFACE = 0xFF1C1B1F;
    private static final int MUTED = 0xFF79747E;
    private static final int OK_GREEN = 0xFF1B5E20;
    private static final int WARN_YELLOW = 0xFFF9A825;
    private static final int WEAK_GREY = 0xFF757575;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(SURFACE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, dp(24), pad, dp(32));
        scroll.addView(root);

        // Header
        TextView title = new TextView(this);
        title.setText("Trade Delay Cap");
        title.setTextColor(PRIMARY_DARK);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Time-Zone Arbitrage Research");
        sub.setTextColor(MUTED);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        sub.setPadding(0, dp(2), 0, dp(16));
        root.addView(sub);

        // Load research.json
        List<Pair> pairs;
        String error = null;
        try {
            pairs = loadResearch();
        } catch (Exception e) {
            pairs = new ArrayList<>();
            error = e.getClass().getSimpleName() + ": " + e.getMessage();
        }

        if (error != null) {
            TextView err = new TextView(this);
            err.setText("โหลด research.json ไม่สำเร็จ\n" + error);
            err.setTextColor(Color.RED);
            root.addView(err);
            setContentView(scroll);
            return;
        }

        // Section heading
        TextView heading = new TextView(this);
        heading.setText("อันดับคู่ตลาดที่น่าเล่น (rank by |Sharpe|)");
        heading.setTextColor(ON_SURFACE);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        heading.setPadding(0, dp(4), 0, dp(8));
        root.addView(heading);

        int rank = 1;
        for (Pair p : pairs) {
            root.addView(buildPairCard(rank++, p));
            root.addView(spacer(dp(8)));
        }

        // Footer disclaimer
        TextView footer = new TextView(this);
        footer.setText("\u26A0 \u0e40\u0e2d\u0e01\u0e2a\u0e32\u0e23\u0e40\u0e1e\u0e37\u0e48\u0e2d\u0e01\u0e32\u0e23\u0e28\u0e36\u0e01\u0e29\u0e32\u0e40\u0e17\u0e48\u0e32\u0e19\u0e31\u0e49\u0e19 \u0e44\u0e21\u0e48\u0e43\u0e0a\u0e48\u0e04\u0e33\u0e41\u0e19\u0e30\u0e19\u0e33\u0e01\u0e32\u0e23\u0e25\u0e07\u0e17\u0e38\u0e19\n" +
                "\u0e14\u0e39 research/timezone_arbitrage.md \u0e2a\u0e33\u0e2b\u0e23\u0e31\u0e1a\u0e23\u0e32\u0e22\u0e25\u0e30\u0e40\u0e2d\u0e35\u0e22\u0e14\u0e40\u0e15\u0e47\u0e21");
        footer.setTextColor(MUTED);
        footer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        footer.setPadding(0, dp(16), 0, 0);
        root.addView(footer);

        setContentView(scroll);
    }

    // ---- Views ----

    private View buildPairCard(int rank, Pair p) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackgroundColor(Color.WHITE);
        int padX = dp(14);
        int padY = dp(12);
        card.setPadding(padX, padY, padX, padY);
        card.setGravity(Gravity.CENTER_VERTICAL);

        // Rank badge
        TextView badge = new TextView(this);
        badge.setText("#" + rank);
        badge.setTextColor(Color.WHITE);
        badge.setBackgroundColor(PRIMARY);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        badge.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(42), dp(42));
        blp.rightMargin = dp(12);
        badge.setLayoutParams(blp);
        card.addView(badge);

        // Text column
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        col.setLayoutParams(clp);

        TextView name = new TextView(this);
        name.setText(p.leadName + " \u2192 " + p.lagName);
        name.setTextColor(ON_SURFACE);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        col.addView(name);

        TextView stats = new TextView(this);
        stats.setText(String.format(
                "\u03B2=%+.3f  \u2022  t=%+.2f  \u2022  Sharpe=%+.2f",
                p.beta, p.tStat, p.sharpe));
        stats.setTextColor(MUTED);
        stats.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        col.addView(stats);

        TextView cat = new TextView(this);
        cat.setText(p.category + "  \u2022  hit " + String.format("%.0f%%", p.hitRate * 100));
        cat.setTextColor(PRIMARY);
        cat.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        col.addView(cat);

        card.addView(col);

        // Verdict chip
        TextView chip = new TextView(this);
        String label;
        int chipBg;
        int chipFg;
        double mag = Math.abs(p.sharpe);
        double tMag = Math.abs(p.tStat);
        if (mag >= 1.0 && tMag >= 3.0) {
            label = "\u0e19\u0e48\u0e32\u0e40\u0e25\u0e48\u0e19"; chipBg = OK_GREEN; chipFg = Color.WHITE;
        } else if (mag >= 0.5 && tMag >= 2.0) {
            label = "\u0e1e\u0e2d\u0e44\u0e14\u0e49"; chipBg = WARN_YELLOW; chipFg = Color.BLACK;
        } else {
            label = "\u0e2d\u0e22\u0e48\u0e32\u0e40\u0e25\u0e48\u0e19"; chipBg = WEAK_GREY; chipFg = Color.WHITE;
        }
        chip.setText(label);
        chip.setTextColor(chipFg);
        chip.setBackgroundColor(chipBg);
        chip.setTypeface(Typeface.DEFAULT_BOLD);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        int chipPad = dp(8);
        chip.setPadding(chipPad, dp(4), chipPad, dp(4));
        card.addView(chip);

        return card;
    }

    private View spacer(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, h));
        return v;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    // ---- Data loading ----

    private static class Pair {
        String leadName, lagName, category;
        double beta, tStat, sharpe, hitRate;
    }

    private List<Pair> loadResearch() throws IOException, JSONException {
        String text = readAsset("research.json");
        JSONObject root = new JSONObject(text);
        JSONArray arr = root.getJSONArray("pairs");
        List<Pair> out = new ArrayList<Pair>(arr.length());
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            Pair p = new Pair();
            p.leadName = o.getString("lead_name");
            p.lagName  = o.getString("lag_name");
            p.category = o.optString("category", "Equity\u2192Equity");
            p.beta     = o.getDouble("beta");
            p.tStat    = o.getDouble("t_stat");
            p.sharpe   = o.getDouble("backtest_sharpe");
            p.hitRate  = o.getDouble("hit_rate");
            out.add(p);
        }
        // Sort by |Sharpe| descending in case the asset wasn't pre-sorted
        Collections.sort(out, new Comparator<Pair>() {
            @Override public int compare(Pair a, Pair b) {
                return Double.compare(Math.abs(b.sharpe), Math.abs(a.sharpe));
            }
        });
        return out;
    }

    private String readAsset(String name) throws IOException {
        Context ctx = getApplicationContext();
        BufferedReader r = new BufferedReader(
                new InputStreamReader(ctx.getAssets().open(name), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            sb.append(line).append('\n');
        }
        r.close();
        return sb.toString();
    }
}
