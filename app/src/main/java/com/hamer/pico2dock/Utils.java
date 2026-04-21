package com.hamer.pico2dock;

import android.content.res.Resources;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class Utils {
    static MainActivity mainActivity = MainActivity.getInstance();

    public static File GetKeystoreFile() {
        File keystore;

        Resources resources = mainActivity.getResources();
        try {
            // Open the audio file from the raw folder
            InputStream inputStream = resources.openRawResource(R.raw.keystore);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            inputStream.close();

            // Create a new File Object
            keystore = new File(mainActivity.getExternalFilesDir(null), "keystore.jks");
            FileOutputStream outputStream = new FileOutputStream(keystore);
            outputStream.write(bytes);
            outputStream.close();

            return keystore;
        } catch (IOException e) {
//            e.printStackTrace();
            return null;
        }
    }

    public static void CleanupDir(String path) {
        File file = new File(path);

        if (file.isDirectory()) {
            String[] children = file.list();
            for (String child : children) {
                CleanupDir(path + "/" + child);
            }
        }
        file.delete();
    }

    public static void CleanupTempDir() {
        CleanupDir("storage/emulated/0/Pico2Dock/Worker");
        CleanupDir("storage/emulated/0/Pico2Dock/Unsign");
    }

    public static String generateString(int length) {
        final String valid = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder res = new StringBuilder();
        Random rnd = new Random();
        while (length-- > 0) {
            res.append(valid.charAt(rnd.nextInt(valid.length())));
        }
        return res.toString();
    }

    public static void FileviewApply(String[] files) {
        ListView fileView = (ListView) mainActivity.findViewById(R.id.ListViewFiles);

        ListAdapter myAdapter = new ArrayAdapter<String>(mainActivity, android.R.layout.simple_list_item_activated_1, mainActivity.APKFiles);
        fileView.setAdapter(myAdapter);
        fileView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }

    public static void FileviewSelect(Integer index) {
        ListView fileView = (ListView) mainActivity.findViewById(R.id.ListViewFiles);

        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fileView.setItemChecked(index, true);
            }
        });
    }

    public static void FileviewChangeText(Integer index, String text) {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.APKFiles[index] = text;
                Utils.FileviewApply(mainActivity.APKFiles);
            }
        });
    }

    public static void FileviewClearTag() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Integer index = 0;
                for (String path : mainActivity.APKFiles) {
                    String newPath = path.replace("🛠️ ", "").replace("✅ ", "");

                    mainActivity.APKFiles[index] = newPath;

                    index++;
                }

                FileviewApply(mainActivity.APKFiles);
            }
        });
    }
}
