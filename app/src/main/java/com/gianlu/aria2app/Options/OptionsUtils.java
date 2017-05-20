package com.gianlu.aria2app.Options;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;

import com.gianlu.aria2app.Adapters.OptionsAdapter;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.JTA2InitializingException;
import com.gianlu.aria2app.Prefs;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// FIXME: stupid things apparently stopped working, damn
public class OptionsUtils {

    private static void showGlobalDialog(Activity activity, List<Option> options) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.globalOptions)
                .setNegativeButton(android.R.string.cancel, null);

        RecyclerView list = new RecyclerView(activity);
        list.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));

        final OptionsAdapter adapter = new OptionsAdapter(activity, options);
        list.setAdapter(adapter);

        builder.setView(list)
                .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO: Handle apply
                    }
                });

        _showDialog(activity, list, builder);
    }

    private static void _showDialog(Activity activity, final View layout, AlertDialog.Builder builder) {
        final Dialog dialog = builder.create();
        final Window window = dialog.getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

            ViewTreeObserver vto = layout.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    WindowManager.LayoutParams params = new WindowManager.LayoutParams();
                    params.copyFrom(window.getAttributes());
                    params.width = dialog.getWindow().getDecorView().getWidth();
                    params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                    dialog.getWindow().setAttributes(params);

                    layout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
        }

        CommonUtils.showDialog(activity, dialog);
    }

    private static void showDialog(Activity activity, List<Option> options) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.options)
                .setNegativeButton(android.R.string.cancel, null);

        RecyclerView list = new RecyclerView(activity);
        list.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));

        final OptionsAdapter adapter = new OptionsAdapter(activity, options);
        list.setAdapter(adapter);

        builder.setView(list)
                .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO: Handle apply
                    }
                });

        _showDialog(activity, list, builder);
    }

    public static void showGlobalDialog(final Activity activity, final boolean quick) {
        final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(activity, R.string.gathering_information);
        CommonUtils.showDialog(activity, pd);

        JTA2 jta2;
        try {
            jta2 = JTA2.instantiate(activity);
        } catch (JTA2InitializingException ex) {
            CommonUtils.UIToast(activity, Utils.ToastMessages.FAILED_LOADING, ex);
            pd.dismiss();
            return;
        }

        jta2.getGlobalOption(new JTA2.IOption() {
            @Override
            public void onOptions(Map<String, String> options) {
                if (quick) {
                    Set<String> quickOptions = Prefs.getSet(activity, Prefs.Keys.A2_GLOBAL_QUICK_OPTIONS, new HashSet<String>());
                    if (quickOptions.isEmpty()) {
                        CommonUtils.UIToast(activity, Utils.ToastMessages.NO_QUICK_OPTIONS);
                        pd.dismiss();
                        return;
                    }

                    showGlobalDialog(activity, Option.fromOptionsMap(options, quickOptions));
                } else {
                    showGlobalDialog(activity, Option.fromOptionsMap(options));
                }

                pd.dismiss();
            }

            @Override
            public void onException(Exception ex) {
                CommonUtils.UIToast(activity, Utils.ToastMessages.FAILED_LOADING, ex);
                pd.dismiss();
            }
        });
    }

    public static void showDialog(final Activity activity, Download download, final boolean quick) {
        final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(activity, R.string.gathering_information);
        CommonUtils.showDialog(activity, pd);

        JTA2 jta2;
        try {
            jta2 = JTA2.instantiate(activity);
        } catch (JTA2InitializingException ex) {
            CommonUtils.UIToast(activity, Utils.ToastMessages.FAILED_LOADING, ex);
            pd.dismiss();
            return;
        }

        jta2.getOption(download.gid, new JTA2.IOption() {
            @Override
            public void onOptions(Map<String, String> options) {
                if (quick) {
                    Set<String> quickOptions = Prefs.getSet(activity, Prefs.Keys.A2_QUICK_OPTIONS, new HashSet<String>());
                    if (quickOptions.isEmpty()) {
                        CommonUtils.UIToast(activity, Utils.ToastMessages.NO_QUICK_OPTIONS);
                        pd.dismiss();
                        return;
                    }

                    showDialog(activity, Option.fromOptionsMap(options, quickOptions));
                } else {
                    showDialog(activity, Option.fromOptionsMap(options));
                }

                pd.dismiss();
            }

            @Override
            public void onException(Exception ex) {
                CommonUtils.UIToast(activity, Utils.ToastMessages.FAILED_LOADING, ex);
                pd.dismiss();
            }
        });
    }
}