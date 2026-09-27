package com.Veibai.VNable.ui;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.Veibai.VNable.R;
import com.Veibai.VNable.data.Condition;
import com.Veibai.VNable.data.GameBook;
import com.Veibai.VNable.data.Inventory;
import com.Veibai.VNable.data.Jump;
import com.Veibai.VNable.data.PackRes;
import com.Veibai.VNable.data.SaveManager;
import com.Veibai.VNable.data.ScriptPack;
import com.Veibai.VNable.data.Settings;
import com.Veibai.VNable.data.ValRegistry;
import com.Veibai.VNable.data.VnButton;
import com.Veibai.VNable.data.VnScene;
import com.Veibai.VNable.databinding.ActivityGameBinding;
import com.Veibai.VNable.databinding.DialogInventoryBinding;
import com.Veibai.VNable.databinding.ItemInventoryEntryBinding;
import com.Veibai.VNable.media.AmbiencePlayer;

import org.json.JSONObject;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class GameActivity extends VnActivity {

    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_SLOT = "slot";
    public static final String EXTRA_PACK_UID = "pack_uid";
    private static final String MODE_NEW = "new";
    private static final String MODE_LOAD = "load";
    private static final String ENTRY_REF = "/Main.json/main";

    private static final String STATE_PACK = "state_pack";
    private static final String STATE_SCENE = "state_scene";
    private static final String STATE_ITEMS = "state_items";

    private static final long CHAR_OUT_MS = 220L;
    private static final long CHAR_IN_MS = 320L;

    private ActivityGameBinding binding;
    private SaveManager saves;
    private final Inventory inventory = new Inventory();
    private final AmbiencePlayer audio = new AmbiencePlayer();

    private ScriptPack pack;
    private ValRegistry valRegistry;
    private int maxBitmapDim;

    private String currentRef = null;
    private VnScene current = null;
    private String fullText = "";
    private int typeIndex = 0;
    private long typeDelayMs = Settings.TextSpeed.NORMAL.delayMs;
    private boolean typing = false;
    private boolean ended = false;
    private String currentBgRef = null;
    private boolean bgOnA = true;
    private boolean pendingFinishAfterSave = false;
    private ObjectAnimator hintPulse;

    private View activeCharFrame = null;
    private String activeCharRef = null;
    private int charGeneration = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable typeTick = new Runnable() {
        @Override
        public void run() {
            if (!typing) return;
            typeIndex++;
            binding.tvDialog.setText(fullText.substring(0, typeIndex));
            if (typeIndex >= fullText.length()) {
                typing = false;
                onTypingDone();
            } else {
                handler.postDelayed(this, typeDelayMs);
            }
        }
    };

    public static void startNew(Activity from, ScriptPack pack) {
        Intent intent = new Intent(from, GameActivity.class);
        intent.putExtra(EXTRA_MODE, MODE_NEW);
        intent.putExtra(EXTRA_PACK_UID, pack.uid);
        from.startActivity(intent);
    }

    public static void startLoad(Activity from, int slot) {
        Intent intent = new Intent(from, GameActivity.class);
        intent.putExtra(EXTRA_MODE, MODE_LOAD);
        intent.putExtra(EXTRA_SLOT, slot);
        from.startActivity(intent);
    }

    @Override
    protected boolean recreateOnThemeChange() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        saves = new SaveManager(this);
        String stateScene = savedInstanceState == null
                ? null : savedInstanceState.getString(STATE_SCENE);
        SaveManager.SaveData restored = null;
        String packUid;
        if (stateScene != null) {
            restored = new SaveManager.SaveData();
            restored.sceneRef = stateScene;
            restored.packUid = savedInstanceState.getString(STATE_PACK);
            restored.items = parseItems(savedInstanceState.getString(STATE_ITEMS));
            packUid = restored.packUid;
        } else if (MODE_LOAD.equals(getIntent().getStringExtra(EXTRA_MODE))) {
            restored = saves.load(getIntent().getIntExtra(EXTRA_SLOT, 1));
            packUid = restored == null ? null : restored.packUid;
        } else {
            packUid = getIntent().getStringExtra(EXTRA_PACK_UID);
        }
        pack = ScriptPack.findByUid(this, packUid);
        if (pack == null) {
            Toast.makeText(this, R.string.pack_missing, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        valRegistry = ValRegistry.load(pack);
        maxBitmapDim = PackRes.screenMaxDim(this);

        binding = ActivityGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        binding.dialogBox.setOnClickListener(v -> onDialogTap());
        binding.btnSysSave.setOnClickListener(v ->
                SaveSlotsDialog.show(this, true, pack.uid, this::saveToSlot));
        binding.btnSysItems.setOnClickListener(v -> showInventory());
        binding.btnSysSettings.setOnClickListener(v -> SettingsActivity.start(this));
        binding.btnSysLobby.setOnClickListener(v -> confirmBackToLobby());

        getOnBackPressedDispatcher().addCallback(this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        confirmBackToLobby();
                    }
                });

        if (restored == null) {
            startNewGame();
        } else if (stateScene != null) {
            SaveManager.jsonToItems(restored.items, inventory);
            enterScene(restored.sceneRef);
        } else {
            restoreFromSave(restored, getIntent().getIntExtra(EXTRA_SLOT, 1));
        }
    }

    private static JSONObject parseItems(String raw) {
        if (raw != null) {
            try {
                return new JSONObject(raw);
            } catch (Exception ignored) {
            }
        }
        return new JSONObject();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pack != null) outState.putString(STATE_PACK, pack.uid);
        if (currentRef != null) outState.putString(STATE_SCENE, currentRef);
        outState.putString(STATE_ITEMS, SaveManager.itemsToJson(inventory).toString());
    }

    private void startNewGame() {
        inventory.restore(null);
        enterScene(ENTRY_REF);
    }

    private void restoreFromSave(SaveManager.SaveData data, int slot) {
        SaveManager.jsonToItems(data.items, inventory);
        floatHint(getString(R.string.loaded_from_save, slot));
        enterScene(data.sceneRef);
    }

    private void enterScene(String ref) {
        VnScene scene = GameBook.resolve(pack, ref);
        if (scene == null) {
            floatHint(getString(R.string.scene_missing_fmt, ref));
            handler.postDelayed(this::finish, 1800);
            return;
        }

        current = scene;
        currentRef = ref;
        ended = false;
        stopHintPulse();
        binding.optionsBox.removeAllViews();

        renderScene(scene);
        startTyping(scene.actorMsg == null ? "" : scene.actorMsg);
    }

    private void renderScene(VnScene scene) {
        swapBackground(scene.bg, PackRes.bitmap(pack, scene.bg, maxBitmapDim));

        if (scene.actorRes == null) {
            hideActor();
        } else {
            boolean left = !VnScene.SIDE_RIGHT.equals(scene.actorSide);
            showActor(scene.actorRes, left,
                    PackRes.bitmap(pack, scene.actorRes, maxBitmapDim / 2));
        }

        binding.tvActorName.setVisibility(
                TextUtils.isEmpty(scene.actorName) ? View.GONE : View.VISIBLE);
        binding.tvActorName.setText(scene.actorName);

        File track = PackRes.audio(pack, scene.audioDir);
        audio.play(this, track, scene.audioLoop, Settings.audioEnabled(this));
    }

    private void applyDeltas(Map<String, Integer> deltas, boolean gain) {
        if (deltas == null || deltas.isEmpty()) return;
        for (Map.Entry<String, Integer> e : deltas.entrySet()) {
            if (gain) inventory.add(e.getKey(), e.getValue());
            else inventory.min(e.getKey(), e.getValue());
        }
        feedbackItems(deltas, gain);
    }

    private Jump runCondition(Condition cond) {
        while (cond != null) {
            Object branch = cond.test(inventory) ? cond.thenBranch : cond.elseBranch;
            if (branch instanceof Jump) return (Jump) branch;
            if (branch instanceof Condition) {
                cond = (Condition) branch;
                continue;
            }
            return null;
        }
        return null;
    }

    private void swapBackground(String ref, Bitmap bitmap) {
        if (Objects.equals(ref, currentBgRef)) return;
        View show = bgOnA ? binding.ivBgB : binding.ivBgA;
        View hide = bgOnA ? binding.ivBgA : binding.ivBgB;
        bgOnA = !bgOnA;
        currentBgRef = ref;
        show.animate().cancel();
        show.setAlpha(0f);
        ((ImageView) show).setImageBitmap(bitmap);
        show.animate().alpha(1f).setDuration(420).start();
        hide.animate().cancel();
        hide.animate().alpha(0f).setDuration(420).start();
    }

    private void showActor(String ref, boolean left, Bitmap bitmap) {
        View targetFrame = left ? binding.frameCharLeft : binding.frameCharRight;
        ImageView targetView = left ? binding.ivCharLeft : binding.ivCharRight;

        if (targetFrame == activeCharFrame && ref.equals(activeCharRef)) return;

        final int gen = ++charGeneration;
        if (activeCharFrame == null) {
            fadeCharIn(gen, targetFrame, targetView, ref, bitmap);
            return;
        }
        final View oldFrame = activeCharFrame;
        activeCharFrame = null;
        activeCharRef = null;
        oldFrame.animate().cancel();
        oldFrame.animate().alpha(0f).scaleX(0.96f).scaleY(0.96f)
                .setDuration(CHAR_OUT_MS)
                .withEndAction(() -> {
                    if (gen != charGeneration) return;
                    oldFrame.setVisibility(View.GONE);
                    oldFrame.setScaleX(1f);
                    oldFrame.setScaleY(1f);
                    fadeCharIn(gen, targetFrame, targetView, ref, bitmap);
                }).start();
    }

    private void hideActor() {
        if (activeCharFrame == null) return;
        final int gen = ++charGeneration;
        final View frame = activeCharFrame;
        activeCharFrame = null;
        activeCharRef = null;
        frame.animate().cancel();
        frame.animate().alpha(0f).scaleX(0.96f).scaleY(0.96f)
                .setDuration(CHAR_OUT_MS)
                .withEndAction(() -> {
                    if (gen != charGeneration) return;
                    frame.setVisibility(View.GONE);
                    frame.setScaleX(1f);
                    frame.setScaleY(1f);
                }).start();
    }

    private void fadeCharIn(int gen, View frame, ImageView view, String ref, Bitmap bitmap) {
        if (gen != charGeneration) return;
        view.setImageBitmap(bitmap);
        frame.setAlpha(0f);
        frame.setScaleX(0.96f);
        frame.setScaleY(0.96f);
        frame.setVisibility(View.VISIBLE);
        activeCharFrame = frame;
        activeCharRef = ref;
        frame.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(CHAR_IN_MS)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void startTyping(String text) {
        handler.removeCallbacks(typeTick);
        typing = true;
        typeIndex = 0;
        fullText = text;
        Settings.TextSpeed speed = Settings.textSpeed(this);
        typeDelayMs = speed.delayMs;
        binding.tvDialog.setText("");
        if (text.isEmpty() || speed == Settings.TextSpeed.INSTANT) {
            typeIndex = text.length();
            binding.tvDialog.setText(text);
            typing = false;
            onTypingDone();
            return;
        }
        handler.postDelayed(typeTick, typeDelayMs);
    }

    private void completeTypingInstantly() {
        typing = false;
        handler.removeCallbacks(typeTick);
        typeIndex = fullText.length();
        binding.tvDialog.setText(fullText);
        onTypingDone();
    }

    private void onTypingDone() {
        if (current == null) return;
        if (current.hasButtons()) {
            showOptions(current.buttons);
        } else if (current.next != null || current.cond != null) {
            startHintPulse();
        } else {
            enterEndState();
        }
    }

    private void leaveScene(VnButton btn) {
        if (current == null) return;
        if (btn != null && btn.jump != null) {
            enterScene(btn.jump.ref());
            return;
        }
        if (btn != null) {
            String action = btn.action;
            if ("add".equals(action)) {
                applyDeltas(current.add, true);
            } else if ("sub".equals(action)) {
                applyDeltas(current.sub, false);
            }
        }
        Jump redirect = runCondition(current.cond);
        if (redirect != null) {
            enterScene(redirect.ref());
            return;
        }
        if (current.next != null) {
            enterScene(current.next.ref());
        } else {
            enterEndState();
        }
    }

    private void onDialogTap() {
        if (typing) {
            completeTypingInstantly();
            return;
        }
        if (ended || current == null) return;
        if (current.hasButtons()) return;
        leaveScene(null);
    }

    private void showOptions(List<VnButton> buttons) {
        boolean horizontal = buttons.size() == 2;
        binding.optionsBox.setOrientation(
                horizontal ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        int i = 0;
        for (VnButton btn : buttons) {
            View card = makeOptionCard(btn.text, i);
            card.setOnClickListener(v -> onOption(btn));
            LinearLayout.LayoutParams lp = horizontal
                    ? new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                    : new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (horizontal) {
                lp.setMargins(i == 0 ? 0 : dp(5), 0, i == 0 ? dp(5) : 0, 0);
            } else if (i > 0) {
                lp.topMargin = dp(10);
            }
            binding.optionsBox.addView(card, lp);

            card.setAlpha(0f);
            card.setTranslationY(dp(10));
            card.animate().alpha(1f).translationY(0f)
                    .setStartDelay(90L * i).setDuration(260)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
            i++;
        }
    }

    private View makeOptionCard(String text, int index) {
        View card = LayoutInflater.from(this)
                .inflate(R.layout.item_option_button, binding.optionsBox, false);
        TextView label = card.findViewById(R.id.tv_option);
        TextView num = card.findViewById(R.id.tv_option_index);
        label.setText(text);
        num.setText(String.format(Locale.US, "%02d", index + 1));
        return card;
    }

    private void onOption(VnButton btn) {
        leaveScene(btn);
    }

    private void feedbackItems(Map<String, Integer> map, boolean gain) {
        String msg;
        if (map.size() == 1) {
            Map.Entry<String, Integer> only = map.entrySet().iterator().next();
            msg = getString(gain ? R.string.item_gain_fmt : R.string.item_lose_fmt,
                    only.getKey(), only.getValue());
        } else {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> e : map.entrySet()) {
                if (sb.length() > 0) sb.append("、");
                sb.append(e.getKey()).append(" ×").append(e.getValue());
            }
            msg = getString(gain ? R.string.item_gain_list_fmt : R.string.item_lose_list_fmt,
                    sb.toString());
        }
        floatHint(msg);
    }

    private void enterEndState() {
        ended = true;
        typing = false;
        stopHintPulse();
        binding.optionsBox.removeAllViews();
        String endMark = "— " + getString(R.string.story_end) + " —";
        binding.tvDialog.setText(TextUtils.isEmpty(fullText)
                ? endMark : fullText + "\n\n" + endMark);

        binding.optionsBox.setOrientation(LinearLayout.VERTICAL);
        View card = makeOptionCard(getString(R.string.back_to_lobby), 0);
        card.setOnClickListener(v -> finish());
        binding.optionsBox.addView(card, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        card.setAlpha(0f);
        card.animate().alpha(1f).setDuration(260).start();
    }

    private void startHintPulse() {
        stopHintPulse();
        hintPulse = ObjectAnimator.ofFloat(binding.ivContinueHint, "alpha", 0.25f, 1f);
        hintPulse.setDuration(750);
        hintPulse.setRepeatCount(ObjectAnimator.INFINITE);
        hintPulse.setRepeatMode(ObjectAnimator.REVERSE);
        hintPulse.start();
    }

    private void stopHintPulse() {
        if (binding == null) return;
        if (hintPulse != null) {
            hintPulse.cancel();
            hintPulse = null;
        }
        binding.ivContinueHint.animate().cancel();
        binding.ivContinueHint.setAlpha(0f);
    }

    private void saveToSlot(int slot) {
        SaveManager.SaveData data = new SaveManager.SaveData();
        data.packUid = pack.uid;
        data.sceneRef = currentRef;
        String label = current != null && current.actorMsg != null ? current.actorMsg : "";
        data.label = label.length() > 18 ? label.substring(0, 18) + "…" : label;
        data.savedAt = System.currentTimeMillis();
        data.items = SaveManager.itemsToJson(inventory);
        saves.save(slot, data);
        floatHint(getString(R.string.save_done, slot));
        if (pendingFinishAfterSave) {
            pendingFinishAfterSave = false;
            finish();
        }
    }

    private void confirmBackToLobby() {
        ConfirmDialog.with(this)
                .message(R.string.lobby_confirm_msg)
                .neutral(R.string.action_cancel, null)
                .negative(R.string.action_back_direct, this::finish)
                .positive(R.string.action_save_and_back, () -> {
                    pendingFinishAfterSave = true;
                    SaveSlotsDialog.show(this, true, pack.uid, this::saveToSlot,
                            () -> pendingFinishAfterSave = false);
                })
                .show();
    }

    private void showInventory() {
        DialogInventoryBinding b = DialogInventoryBinding.inflate(getLayoutInflater());

        boolean any = false;
        for (ValRegistry.Item item : valRegistry.all()) {
            int count = inventory.count(item.id);
            if (count <= 0) continue;
            any = true;
            ItemInventoryEntryBinding row = ItemInventoryEntryBinding.inflate(getLayoutInflater());
            if (item.icon != null) {
                row.ivItemIcon.setImageBitmap(PackRes.decode(pack.uid, item.icon, dp(40)));
            } else {
                row.ivItemIcon.setImageBitmap(null);
            }
            row.tvItemName.setText(item.id);
            row.tvItemCount.setText(String.format(Locale.getDefault(), "×%d", count));
            b.boxItems.addView(row.getRoot());
        }
        b.tvItemsEmpty.setVisibility(any ? View.GONE : View.VISIBLE);

        Dialog dialog = Modals.show(this, b.getRoot());
        b.btnInvDone.setOnClickListener(v -> dialog.dismiss());
    }

    private final Runnable hideHint = () ->
            binding.tvFloatHint.animate().alpha(0f).setDuration(320L).start();

    private void floatHint(String msg) {
        handler.removeCallbacks(hideHint);
        binding.tvFloatHint.setText(msg);
        binding.tvFloatHint.animate().cancel();
        binding.tvFloatHint.animate().alpha(1f).setDuration(180L).start();
        handler.postDelayed(hideHint, 1600L);
    }

    private void applyFontScale() {
        float sp;
        switch (Settings.fontSize(this)) {
            case SMALL:
                sp = 16f;
                break;
            case LARGE:
                sp = 21f;
                break;
            case STANDARD:
            default:
                sp = 18f;
                break;
        }
        binding.tvDialog.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyFontScale();
        if (current != null) {
            boolean audioOn = Settings.audioEnabled(this);
            File track = PackRes.audio(pack, current.audioDir);
            audio.play(this, track, current.audioLoop, audioOn);
            audio.resume(audioOn);
        }
    }

    @Override
    protected void onPause() {
        audio.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        stopHintPulse();
        audio.release();
        super.onDestroy();
    }
}
