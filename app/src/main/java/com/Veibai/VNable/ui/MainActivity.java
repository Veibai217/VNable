package com.Veibai.VNable.ui;

import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.Veibai.VNable.R;
import com.Veibai.VNable.databinding.ActivityMainBinding;
import com.Veibai.VNable.util.LauncherArt;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends VnActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        int art = LauncherArt.pick(this, "game_launcher");
        if (art != 0) binding.ivLobbyBg.setImageResource(art);

        binding.btnContinue.setOnClickListener(v ->
                PackPickerDialog.show(this, pack -> GameActivity.startNew(this, pack)));
        binding.btnSave.setOnClickListener(v ->
                SaveSlotsDialog.show(this, false, null,
                        slot -> GameActivity.startLoad(this, slot)));
        binding.btnPacks.setOnClickListener(v -> PackManagerActivity.start(this));
        binding.btnSettings.setOnClickListener(v -> SettingsActivity.start(this));
        binding.btnExit.setOnClickListener(v -> confirmExit());

        getOnBackPressedDispatcher().addCallback(this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        confirmExit();
                    }
                });

        playEntrance();
    }

    private void playEntrance() {
        List<View> sequence = Arrays.asList(
                binding.tvLogo,
                binding.logoUnderline,
                binding.tvSubtitle,
                binding.btnContinue,
                binding.btnSave,
                binding.btnPacks,
                binding.btnSettings,
                binding.btnExit);

        DecelerateInterpolator ease = new DecelerateInterpolator(1.6f);
        for (int i = 0; i < sequence.size(); i++) {
            View v = sequence.get(i);
            v.setAlpha(0f);
            v.setTranslationY(dp(14));
            v.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(80L + 60L * i)
                    .setDuration(420L)
                    .setInterpolator(ease)
                    .start();
        }

        binding.ivLobbyBg.setAlpha(0f);
        binding.ivLobbyBg.animate().alpha(1f).setDuration(600L).start();
    }

    private void confirmExit() {
        ConfirmDialog.with(this)
                .message(R.string.dialog_exit_msg)
                .positive(R.string.action_confirm, this::finishAffinity)
                .negative(R.string.action_cancel, null)
                .show();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
