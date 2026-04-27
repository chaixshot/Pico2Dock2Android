package com.hamer.pico2dock;

import com.reandroid.apkeditor.merge.MergerOptions;

public class Merger extends com.reandroid.apkeditor.merge.Merger {
    String apkName;
    static MainActivity mainActivity = MainActivity.getInstance();

    public Merger(MergerOptions options, String name) {
        super(options);

        apkName = name;

    }

    @Override
    public void logMessage(String msg) {
        super.logMessage(msg);

        if (!mainActivity.MainTask.isCancelled())
            mainActivity.ChangeStateText("## Merger\nMerging multiple split **" + apkName + "**...\n\n``" + msg + "``");
    }

    @Override
    public void logMessage(String tag, String msg) {
        super.logMessage(tag, msg);

        if (!mainActivity.MainTask.isCancelled())
            mainActivity.ChangeStateText("## Merger\nMerging multiple split **" + apkName + "**...\n\n``" + msg + "``");
    }

    @Override
    public void logVerbose(String msg) {
        super.logVerbose(msg);

        if (!mainActivity.MainTask.isCancelled())
            mainActivity.ChangeStateText("## Merger\nMerging multiple split **" + apkName + "**...\n\n``" + msg + "``");
    }

    @Override
    public void logVerbose(String tag, String msg) {
        super.logVerbose(tag, msg);

        if (!mainActivity.MainTask.isCancelled())
            mainActivity.ChangeStateText("## Merger\nMerging multiple split **" + apkName + "**...\n\n``" + msg + "``");
    }

    @Override
    public void logError(String msg, Throwable tr) {
        super.logError(msg, tr);

        if (!mainActivity.MainTask.isCancelled())
            mainActivity.ChangeStateText("## Merger\nMerging multiple split **" + apkName + "**...\n\n``" + msg + "``");
    }

    @Override
    public void logWarn(String msg) {
        super.logWarn(msg);

        if (!mainActivity.MainTask.isCancelled())
            mainActivity.ChangeStateText("## Current Statu\nMerging **" + apkName + "**...\n\n``" + msg + "``");
    }
}
