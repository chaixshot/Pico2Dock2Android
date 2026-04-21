package com.hamer.pico2dock;

import android.app.Activity;
import android.content.Context;

import com.reandroid.apkeditor.Main;
import com.reandroid.apkeditor.decompile.DecompileOptions;

public class Decompiler extends com.reandroid.apkeditor.decompile.Decompiler {
    String apkName;

    public Decompiler(DecompileOptions options, String name) {
        super(options);

        apkName = name;
    }

    @Override
    public void logMessage(String msg) {
        super.logMessage(msg);

        MainActivity.getInstance().ChangeStateText("### Current Status\n---\nDecompiling **" + apkName + "**...\n\n```" + msg + "```");
    }

    @Override
    public void logVerbose(String msg) {
        super.logVerbose(msg);
    }

    @Override
    public void logError(String msg, Throwable tr) {
        super.logError(msg, tr);
    }
}
