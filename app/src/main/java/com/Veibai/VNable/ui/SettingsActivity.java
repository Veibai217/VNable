package com.Veibai.VNable.ui;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.Veibai.VNable.R;
import com.Veibai.VNable.data.Settings;
import com.Veibai.VNable.data.ThemeMode;
import com.Veibai.VNable.databinding.ActivitySettingsBinding;
import com.Veibai.VNable.util.ThemeColors;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends VnActivity {

    private ActivitySettingsBinding binding;
    private final List<View> swatches = new ArrayList<>();

    public static void start(android.content.Context from) {
        from.startActivity(new android.content.Intent(from, SettingsActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        bindThemeSwatches();
        bindLanguageSection();
        bindTextSections();
        bindAudioSwitch();
        binding.tvVersion.setText(Settings.VERSION);
    }

    private void bindThemeSwatches() {
        ThemeMode current = Settings.theme(this);
        int selectedStroke = ThemeColors.resolve(this, R.attr.vnTextPrimary);
        binding.tvThemeName.setText(getString(current.labelRes));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(34), dp(34));
        lp.setMargins(0, 0, dp(10), 0);

        for (ThemeMode mode : ThemeMode.values()) {
            View swatch = new View(this);
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(mode.swatchColor);
            swatch.setBackground(circle);
            swatch.setContentDescription(
                    getString(R.string.settings_theme_fmt, getString(mode.labelRes)));
            swatch.setLayoutParams(lp);
            swatch.setOnClickListener(v -> {
                if (Settings.theme(this) == mode) return;
                Settings.setTheme(this, mode);
                recreate();
            });
            binding.boxThemeSwatches.addView(swatch);
            swatches.add(swatch);
        }
        paintSelection(current, selectedStroke);
    }

    private void paintSelection(ThemeMode selected, int strokeColor) {
        binding.tvThemeName.setText(getString(selected.labelRes));
        for (int i = 0; i < swatches.size(); i++) {
            GradientDrawable bg = (GradientDrawable) swatches.get(i).getBackground();
            if (ThemeMode.values()[i] == selected) {
                bg.setStroke(dp(3), strokeColor);
                swatches.get(i).setScaleX(1.12f);
                swatches.get(i).setScaleY(1.12f);
            } else {
                bg.setStroke(0, 0);
                swatches.get(i).setScaleX(1f);
                swatches.get(i).setScaleY(1f);
            }
        }
    }

    private void bindLanguageSection() {
        LocaleListCompat current = AppCompatDelegate.getApplicationLocales();
        int selected;
        if (current.isEmpty()) {
            selected = 0; 
        } else {
            String tag = current.get(0) == null ? "" : current.get(0).toLanguageTag();
            if (tag.startsWith("zh")) selected = 1;
            else if (tag.startsWith("en")) selected = 2;
            else selected = 0;
        }
        paintSegments(languageSegments(), selected);

        binding.segLangSystem.setOnClickListener(v ->
                applyLanguage(LocaleListCompat.getEmptyLocaleList()));
        binding.segLangZh.setOnClickListener(v ->
                applyLanguage(LocaleListCompat.forLanguageTags("zh-CN")));
        binding.segLangEn.setOnClickListener(v ->
                applyLanguage(LocaleListCompat.forLanguageTags("en")));
    }

    private List<TextView> languageSegments() {
        List<TextView> segs = new ArrayList<>();
        segs.add(binding.segLangSystem);
        segs.add(binding.segLangZh);
        segs.add(binding.segLangEn);
        return segs;
    }

    private void applyLanguage(LocaleListCompat locales) {
        if (locales.toLanguageTags()
                .equals(AppCompatDelegate.getApplicationLocales().toLanguageTags())) {
            return;
        }
        AppCompatDelegate.setApplicationLocales(locales);
    }

    private void bindTextSections() {
        Settings.TextSpeed[] speeds = Settings.TextSpeed.values();
        List<TextView> speedSegs = new ArrayList<>();
        speedSegs.add(binding.segSpeedSlow);
        speedSegs.add(binding.segSpeedNormal);
        speedSegs.add(binding.segSpeedFast);
        speedSegs.add(binding.segSpeedInstant);
        paintSegments(speedSegs, Settings.textSpeed(this).ordinal());
        for (int i = 0; i < speedSegs.size(); i++) {
            final int idx = i;
            speedSegs.get(i).setOnClickListener(v -> {
                Settings.setTextSpeed(this, speeds[idx]);
                paintSegments(speedSegs, idx);
            });
        }

        Settings.FontSize[] fonts = Settings.FontSize.values();
        List<TextView> fontSegs = new ArrayList<>();
        fontSegs.add(binding.segFontSmall);
        fontSegs.add(binding.segFontStandard);
        fontSegs.add(binding.segFontLarge);
        paintSegments(fontSegs, Settings.fontSize(this).ordinal());
        for (int i = 0; i < fontSegs.size(); i++) {
            final int idx = i;
            fontSegs.get(i).setOnClickListener(v -> {
                Settings.setFontSize(this, fonts[idx]);
                paintSegments(fontSegs, idx);
            });
        }
    }

    private void bindAudioSwitch() {
        binding.swAudio.setChecked(Settings.audioEnabled(this));
        binding.swAudio.setOnCheckedChangeListener(
                (view, value) -> Settings.setAudioEnabled(this, value));
    }

    private static void paintSegments(List<TextView> segs, int selected) {
        for (int i = 0; i < segs.size(); i++) {
            TextView seg = segs.get(i);
            boolean on = i == selected;
            seg.setBackgroundResource(on ? R.drawable.bg_segment_selected : 0);
            seg.setTextColor(ThemeColors.resolve(seg.getContext(), on
                    ? R.attr.vnAccentBright : R.attr.vnTextSecondary));
            seg.setClickable(true);
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
