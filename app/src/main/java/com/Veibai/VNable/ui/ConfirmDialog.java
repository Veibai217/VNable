package com.Veibai.VNable.ui;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.widget.TextView;

import com.Veibai.VNable.databinding.DialogConfirmBinding;

public final class ConfirmDialog {

    public interface Action {
        void run();
    }

    private ConfirmDialog() {
    }

    public static Builder with(Activity activity) {
        return new Builder(activity);
    }

    public static final class Builder {

        private final Activity activity;
        private CharSequence title;
        private CharSequence message;
        private CharSequence positiveText;
        private Action onPositive;
        private CharSequence negativeText;
        private Action onNegative;
        private CharSequence neutralText;
        private Action onNeutral;

        private Builder(Activity activity) {
            this.activity = activity;
        }

        public Builder title(int resId) {
            this.title = activity.getString(resId);
            return this;
        }

        public Builder message(int resId) {
            this.message = activity.getString(resId);
            return this;
        }

        public Builder message(CharSequence text) {
            this.message = text;
            return this;
        }

        public Builder positive(int resId, Action action) {
            this.positiveText = activity.getString(resId);
            this.onPositive = action;
            return this;
        }

        public Builder negative(int resId, Action action) {
            this.negativeText = activity.getString(resId);
            this.onNegative = action;
            return this;
        }

        public Builder neutral(int resId, Action action) {
            this.neutralText = activity.getString(resId);
            this.onNeutral = action;
            return this;
        }

        public void show() {
            DialogConfirmBinding b = DialogConfirmBinding.inflate(activity.getLayoutInflater());

            if (title != null) {
                b.tvConfirmTitle.setVisibility(View.VISIBLE);
                b.tvConfirmTitle.setText(title);
            }
            b.tvConfirmMsg.setText(message);

            Dialog dialog = Modals.show(activity, b.getRoot());
            bind(b.btnConfirmNeutral, neutralText, onNeutral, dialog);
            bind(b.btnConfirmNegative, negativeText, onNegative, dialog);
            bind(b.btnConfirmPrimary, positiveText, onPositive, dialog);
        }

        private static void bind(TextView btn, CharSequence text, Action action, Dialog dialog) {
            if (text == null) return;
            btn.setVisibility(View.VISIBLE);
            btn.setText(text);
            btn.setOnClickListener(v -> {
                dialog.dismiss();
                if (action != null) action.run();
            });
        }
    }
}
