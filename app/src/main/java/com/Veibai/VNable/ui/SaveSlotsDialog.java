package com.Veibai.VNable.ui;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;

import com.Veibai.VNable.R;
import com.Veibai.VNable.data.SaveManager;
import com.Veibai.VNable.data.ScriptPack;
import com.Veibai.VNable.databinding.DialogSaveSlotsBinding;
import com.Veibai.VNable.databinding.ItemSaveSlotBinding;
import com.Veibai.VNable.util.ThemeColors;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class SaveSlotsDialog {

    public interface Listener {
        void onPick(int slot);
    }

    private SaveSlotsDialog() {
    }

    public static void show(Activity activity, boolean saveMode, String packUid,
                            Listener listener) {
        show(activity, saveMode, packUid, listener, null);
    }

    public static void show(Activity activity, boolean saveMode, String packUid,
                            Listener listener, Runnable onCancel) {
        DialogSaveSlotsBinding root = DialogSaveSlotsBinding.inflate(activity.getLayoutInflater());
        SaveManager saves = new SaveManager(activity);

        root.tvTitle.setText(saveMode
                ? R.string.save_slots_title_save : R.string.save_slots_title_load);

        Dialog dialog = Modals.show(activity, root.getRoot());
        if (onCancel != null) {
            dialog.setOnCancelListener(d -> onCancel.run());
        }
        root.btnCancel.setOnClickListener(v -> dialog.cancel());

        int[] containers = {R.id.slot_1, R.id.slot_2, R.id.slot_3};
        List<ScriptPack> installed = ScriptPack.listInstalled(activity);
        for (int i = 0; i < SaveManager.SLOTS; i++) {
            final int slot = i + 1;
            android.widget.LinearLayout box = root.getRoot().findViewById(containers[i]);
            ItemSaveSlotBinding row = ItemSaveSlotBinding.inflate(activity.getLayoutInflater());
            SaveManager.SaveData data = saves.load(slot);
            bindSlot(activity, saves, row, slot, data, installed);
            row.rowSlot.setOnClickListener(v -> {
                SaveManager.SaveData latest = saves.load(slot);
                if (saveMode) {
                    if (latest != null) {
                        confirmOverwrite(activity, () -> {
                            listener.onPick(slot);
                            dialog.dismiss();
                        });
                    } else {
                        listener.onPick(slot);
                        dialog.dismiss();
                    }
                } else if (latest != null) {
                    listener.onPick(slot);
                    dialog.dismiss();
                }
            });
            box.addView(row.getRoot());
        }
    }

    private static void confirmOverwrite(Activity activity, Runnable onYes) {
        ConfirmDialog.with(activity)
                .message(R.string.overwrite_confirm)
                .positive(R.string.action_confirm, onYes::run)
                .negative(R.string.action_cancel, null)
                .show();
    }

    private static void bindSlot(Activity activity, SaveManager saves,
                                 ItemSaveSlotBinding row, int slot,
                                 SaveManager.SaveData data, List<ScriptPack> installed) {
        row.tvSlotNum.setText(String.format(Locale.US, "%02d", slot));

        boolean hasData = data != null;
        applyState(activity, row, hasData);
        if (hasData) {
            row.tvSlotTitle.setText(data.label == null || data.label.isEmpty()
                    ? activity.getString(R.string.save_slot_empty) : data.label);
            String owner = data.packUid;
            for (ScriptPack pack : installed) {
                if (pack.uid.equals(data.packUid)) {
                    owner = pack.name;
                    break;
                }
            }
            String time = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
                    .format(new Date(data.savedAt));
            row.tvSlotSub.setText(activity.getString(R.string.save_slot_sub_fmt,
                    owner == null ? "-" : owner, time));
            row.ivSlotDelete.setVisibility(View.VISIBLE);
            row.ivSlotDelete.setOnClickListener(v ->
                    ConfirmDialog.with(activity)
                            .message(R.string.delete_confirm)
                            .positive(R.string.action_delete, () -> {
                                saves.delete(slot);
                                row.tvSlotTitle.setText(R.string.save_slot_empty);
                                row.tvSlotSub.setText("");
                                row.ivSlotDelete.setVisibility(View.GONE);
                                applyState(activity, row, false);
                            })
                            .negative(R.string.action_cancel, null)
                            .show());
        } else {
            row.tvSlotTitle.setText(R.string.save_slot_empty);
            row.tvSlotSub.setText("");
            row.ivSlotDelete.setVisibility(View.GONE);
        }
    }

    private static void applyState(Activity activity, ItemSaveSlotBinding row, boolean filled) {
        row.badgeSlot.setBackgroundResource(filled
                ? R.drawable.bg_badge_filled : R.drawable.bg_badge_empty);
        row.tvSlotNum.setTextColor(ThemeColors.resolve(activity, filled
                ? R.attr.vnOnAccent : R.attr.vnAccent));
    }
}
