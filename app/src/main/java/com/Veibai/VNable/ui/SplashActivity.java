package com.Veibai.VNable.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.DecelerateInterpolator;

import com.Veibai.VNable.databinding.ActivitySplashBinding;
import com.Veibai.VNable.util.BlurFx;
import com.Veibai.VNable.util.LauncherArt;

public class SplashActivity extends VnActivity {

    private static final long AUTO_ADVANCE_MS = 2200;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean advanced = false;

    @Override
    protected boolean isSplash() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySplashBinding binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        int art = LauncherArt.pick(this, "src_launcher");
        if (art != 0) binding.ivSplash.setImageResource(art);

        BlurFx.blurImage(binding.ivSplash);

        binding.ivSplash.setScaleX(1f);
        binding.ivSplash.setScaleY(1f);
        binding.ivSplash.animate()
                .scaleX(1.08f).scaleY(1.08f)
                .setDuration(2400L)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        binding.tvSplashBrand.animate()
                .alpha(1f)
                .setStartDelay(420L)
                .setDuration(520L)
                .start();
        binding.splashUnderline.animate()
                .alpha(1f)
                .setStartDelay(700L)
                .setDuration(420L)
                .start();

        binding.getRoot().setOnClickListener(v -> goLobby());
        handler.postDelayed(this::goLobby, AUTO_ADVANCE_MS);
    }

    private void goLobby() {
        if (advanced) return;
        advanced = true;
        handler.removeCallbacksAndMessages(null);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
