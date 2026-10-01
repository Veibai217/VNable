package com.Veibai.VNable.ui;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ScrollView;

import com.Veibai.VNable.R;
import com.Veibai.VNable.util.Glass;

public final class Modals {

    private Modals() {
    }

    public static Dialog show(Activity activity, View content) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        ScrollView scroll = new ScrollView(activity);
        scroll.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        scroll.addView(content, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog.setContentView(scroll);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.CENTER);
            window.setWindowAnimations(R.style.Animation_VN_Modal);
        }
        Glass.behind(window);

        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        int width = Math.min((int) (dm.widthPixels * 0.88f),
                (int) (activity.getResources().getDimension(R.dimen.modal_max_width)));
        int maxH = (int) (dm.heightPixels * 0.82f);
        scroll.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(maxH, View.MeasureSpec.AT_MOST));
        int height = scroll.getMeasuredHeight() >= maxH
                ? maxH : ViewGroup.LayoutParams.WRAP_CONTENT;

        dialog.show();
        if (window != null) window.setLayout(width, height);
        return dialog;
    }
}
