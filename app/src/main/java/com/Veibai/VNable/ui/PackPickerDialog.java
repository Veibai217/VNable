package com.Veibai.VNable.ui;

import android.app.Activity;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;

import com.Veibai.VNable.R;
import com.Veibai.VNable.data.ScriptPack;
import com.Veibai.VNable.databinding.DialogPackPickerBinding;
import com.Veibai.VNable.databinding.ItemPackRowBinding;
import com.Veibai.VNable.util.Background;

import java.util.List;

public final class PackPickerDialog {

    public interface Listener {
        void onPick(ScriptPack pack);
    }

    private PackPickerDialog() {
    }

    public static void show(Activity activity, Listener listener) {
        DialogPackPickerBinding binding =
                DialogPackPickerBinding.inflate(activity.getLayoutInflater());
        Dialog dialog = Modals.show(activity, binding.getRoot());
        binding.btnCancel.setOnClickListener(v -> dialog.dismiss());
        binding.progressScan.setVisibility(View.VISIBLE);

        Background.io(() -> {
            List<ScriptPack> packs = ScriptPack.listInstalled(activity);
            Background.main(() -> {
                if (!dialog.isShowing() || activity.isDestroyed() || activity.isFinishing()) {
                    return;
                }
                binding.progressScan.setVisibility(View.GONE);
                if (packs.isEmpty()) {
                    dialog.dismiss();
                    ConfirmDialog.with(activity)
                            .message(R.string.pack_none_hint)
                            .positive(R.string.pack_go_import,
                                    () -> PackManagerActivity.start(activity))
                            .negative(R.string.action_cancel, null)
                            .show();
                    return;
                }

                LayoutInflater inflater = activity.getLayoutInflater();
                for (ScriptPack pack : packs) {
                    ItemPackRowBinding row = ItemPackRowBinding.inflate(inflater);
                    row.tvPackName.setText(pack.name);
                    row.tvPackSub.setText(activity.getString(R.string.pack_sub_fmt,
                            pack.ver, pack.uid));
                    row.getRoot().setOnClickListener(v -> {
                        dialog.dismiss();
                        listener.onPick(pack);
                    });
                    binding.boxPacks.addView(row.getRoot());
                }
            });
        });
    }
}
