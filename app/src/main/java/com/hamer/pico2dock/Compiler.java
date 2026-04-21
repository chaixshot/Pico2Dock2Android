package com.hamer.pico2dock;

import com.reandroid.apkeditor.compile.BuildOptions;

public class Compiler extends com.reandroid.apkeditor.compile.Builder {
    String apkName;

    public Compiler(BuildOptions options, String name) {
        super(options);

        apkName = name;
    }

    @Override
    public void logMessage(String msg) {
        super.logMessage(msg);

        MainActivity.getInstance().ChangeStateText("### Current Status\n---\nCompiling **" + apkName + "**...\n\n```" + msg + "```");
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
