package com.Veibai.VNable.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.Veibai.VNable.R;
import com.Veibai.VNable.data.GameBook;
import com.Veibai.VNable.data.PackImporter;
import com.Veibai.VNable.data.PackRes;
import com.Veibai.VNable.data.ScriptPack;
import com.Veibai.VNable.databinding.ActivityPackManagerBinding;
import com.Veibai.VNable.databinding.ItemPackCardBinding;
import com.Veibai.VNable.util.Background;

import java.util.List;

public class PackManagerActivity extends VnActivity {

    private static final String[] ZIP_MIME_TYPES = {
            "application/zip", "application/x-zip-compressed", "application/octet-stream"};

    private ActivityPackManagerBinding binding;
    private int refreshGeneration = 0;
    private final ActivityResultLauncher<String[]> pickPack =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onPicked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPackManagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnImport.setOnClickListener(v -> openFilePicker());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void openFilePicker() {
        pickPack.launch(ZIP_MIME_TYPES);
    }

    public static void start(android.content.Context from) {
        from.startActivity(new Intent(from, PackManagerActivity.class));
    }

    private void onPicked(Uri uri) {
        if (uri == null) return;
        binding.importProgress.setVisibility(View.VISIBLE);
        binding.btnImport.setEnabled(false);
        Background.io(() -> {
            PackImporter.Result result = PackImporter.importPack(this, uri);
            Background.main(() -> {
                if (isDestroyed() || isFinishing()) return;
                binding.importProgress.setVisibility(View.GONE);
                binding.btnImport.setEnabled(true);
                if (result.succeeded()) {
                    Toast.makeText(this,
                            getString(R.string.pack_import_done, result.pack.name),
                            Toast.LENGTH_SHORT).show();
                    GameBook.invalidate(result.pack.uid);
                    PackRes.clearPack(result.pack.uid);
                } else {
                    Toast.makeText(this,
                            getString(R.string.pack_import_failed, result.error),
                            Toast.LENGTH_LONG).show();
                }
                refresh();
            });
        });
    }

    private void refresh() {
        final int gen = ++refreshGeneration;
        Background.io(() -> {
            List<ScriptPack> packs = ScriptPack.listInstalled(this);
            Background.main(() -> {
                if (gen != refreshGeneration || isDestroyed() || isFinishing()) return;
                renderPacks(packs);
            });
        });
    }

    private void renderPacks(List<ScriptPack> packs) {
        binding.boxPacks.removeAllViews();
        binding.tvEmpty.setVisibility(packs.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = getLayoutInflater();
        for (ScriptPack pack : packs) {
            ItemPackCardBinding card = ItemPackCardBinding.inflate(inflater);
            bindCard(card, pack);
            binding.boxPacks.addView(card.getRoot());
        }
    }

    private void bindCard(ItemPackCardBinding card, ScriptPack pack) {
        card.tvPackName.setText(pack.name);
        card.tvPackSub.setText(getString(R.string.pack_author_fmt, pack.displayAuthor()));
        card.boxTags.setVisibility(View.GONE);

        card.btnUninstall.setOnClickListener(v -> ConfirmDialog.with(this)
                .message(getString(R.string.pack_uninstall_confirm, pack.name))
                .positive(R.string.action_delete, () -> {
                    GameBook.invalidate(pack.uid);
                    PackRes.clearPack(pack.uid);
                    Background.io(() -> {
                        pack.uninstall();
                        Background.main(this::refresh);
                    });
                })
                .negative(R.string.action_cancel, null)
                .show());
    }

}
