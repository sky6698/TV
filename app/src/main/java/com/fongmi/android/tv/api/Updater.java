package com.fongmi.android.tv.api;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.Github;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogUpdateBinding;
import com.fongmi.android.tv.net.Download;
import com.fongmi.android.tv.net.OkHttp;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.io.File;
import java.util.Locale;

public class Updater implements Download.Callback {

    private DialogUpdateBinding binding;
    private AlertDialog dialog;

    private static class Loader {
        static volatile Updater INSTANCE = new Updater();
    }

    public static Updater get() {
        return Loader.INSTANCE;
    }

    private File getFile() {
        return FileUtil.getCacheFile(BuildConfig.FLAVOR + ".apk");
    }

    private String getJson() {
        return Github.get().getKitkatPath("/release/" + BuildConfig.FLAVOR + ".json");
    }

    private String getApk() {
        return Github.get().getKitkatPath("/release/" + BuildConfig.FLAVOR + ".apk");
    }

    public Updater reset() {
        Prefers.putUpdate(true);
        return this;
    }

    public void start(Activity activity) {
        this.binding = DialogUpdateBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).setCancelable(false).create();
        App.execute(this::doInBackground);
    }

    private void doInBackground() {
        try {
            JSONObject object = new JSONObject(OkHttp.newCall(getJson()).execute().body().string());
            String name = object.optString("name");
            String desc = object.optString("desc");
            int code = object.optInt("code");
            boolean need = code > BuildConfig.VERSION_CODE && Prefers.getUpdate();
            if (need) App.post(() -> show(name, desc));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void show(String version, String desc) {
        binding.version.setText(ResUtil.getString(R.string.update_version, version));
        binding.confirm.setOnClickListener(this::confirm);
        binding.cancel.setOnClickListener(this::cancel);
        binding.desc.setText(desc);
        dialog.show();
    }

    private void dismiss() {
        if (dialog != null) dialog.dismiss();
    }

    private void cancel(View view) {
        Prefers.putUpdate(false);
        dismiss();
    }

    private void confirm(View view) {
        binding.confirm.setEnabled(false);
        Download.create(getApk(), getFile(), this).start();
    }

    @Override
    public void progress(int progress) {
        binding.confirm.setText(String.format(Locale.getDefault(), "%1$d%%", progress));
    }

    @Override
    public void error(String message) {
        Notify.show(message);
        dismiss();
    }

    @Override
    public void success(File file) {
        FileUtil.openFile(getFile());
        dismiss();
    }
}