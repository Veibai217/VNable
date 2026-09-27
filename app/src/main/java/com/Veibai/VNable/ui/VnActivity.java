package com.Veibai.VNable.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.Veibai.VNable.data.Settings;
import com.Veibai.VNable.data.ThemeMode;
import com.Veibai.VNable.util.Immersive;

public abstract class VnActivity extends AppCompatActivity {

    private ThemeMode appliedTheme = ThemeMode.AMBER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        appliedTheme = Settings.theme(this);
        setTheme(isSplash() ? appliedTheme.splashStyleRes : appliedTheme.styleRes);
        super.onCreate(savedInstanceState);
    }

    protected boolean isSplash() {
        return false;
    }

    protected boolean recreateOnThemeChange() {
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Immersive.apply(getWindow());
        if (Settings.theme(this) != appliedTheme) {
            appliedTheme = Settings.theme(this);
            if (recreateOnThemeChange()) recreate();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) Immersive.apply(getWindow());
    }
}
