package com.hamer.pico2dock;

import static android.view.View.VISIBLE;

import static androidx.core.content.FileProvider.getUriForFile;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.apksigner.ApkSignerTool;
import com.developer.filepicker.controller.DialogSelectionListener;
import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;
import com.reandroid.apkeditor.compile.BuildOptions;
import com.reandroid.apkeditor.decompile.DecompileOptions;
import com.reandroid.apkeditor.merge.MergerOptions;
import com.reandroid.archive.ZipAlign;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import io.noties.markwon.Markwon;

public class MainActivity extends AppCompatActivity {
    String[] APKFiles;
    String[] APKFilesOut;
    File keystore;

    AsyncTask MainTask;

    Button ButtonStart;
    Button ButtonCancel;
    Button ButtonClear;
    TextView TextViewSelectHint;
    Switch SwtichHideDock;
    CheckBox CheckboxRePackage;
    CheckBox CheckboxRePackageAdv;
    ProgressBar StatusProgressBar;
    TextView PercentText;
    EditText TextRename;
    CheckBox CheckboxRename;

    boolean IsHideDock = false;
    boolean IsRePackage = false;
    boolean IsRePackageAdv = false;
    String NamePrefix;
    boolean IsRename = false;

    boolean IsProcessRunning = false;
    Long DoubleBack = System.currentTimeMillis() - 2000;
    private static MainActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        instance = this;

        keystore = Utils.GetKeystoreFile();

        ButtonStart = (Button) findViewById(R.id.ButtonStart);
        ButtonCancel = (Button) findViewById(R.id.ButtonCancel);
        ButtonClear = (Button) findViewById(R.id.ButtonClear);
        TextViewSelectHint = (TextView) findViewById(R.id.TextFileSelectHint);
        SwtichHideDock = (Switch) findViewById(R.id.SwitchHideDock);
        CheckboxRePackage = (CheckBox) findViewById(R.id.CheckboxRePackage);
        CheckboxRePackageAdv = (CheckBox) findViewById(R.id.CheckboxRePackageAdv);
        StatusProgressBar = (ProgressBar) findViewById(R.id.StatusProgressBar);
        PercentText = (TextView) findViewById(R.id.PercentText);
        TextRename = (EditText) findViewById(R.id.TextRename);
        CheckboxRename = (CheckBox) findViewById(R.id.CheckboxRename);

        ResetAppearance();
        ChangeButtonState();
    }

    @Override
    public void onBackPressed() {
        if (DoubleBack + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
        } else {
            DoubleBack = System.currentTimeMillis();
            Toast.makeText(this, "Press once again to Exit", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (IsProcessRunning)
            MainTask.cancel(true);
        Utils.CleanupTempDir();

        super.onDestroy();
        System.exit(0);
    }

    public static MainActivity getInstance() {
        return instance;
    }

    public void SelectFile(View view) {
        PermissionHelper.CheckWritePermission(() -> {
            DialogProperties properties = new DialogProperties();

            properties.selection_mode = DialogConfigs.MULTI_MODE;
            properties.selection_type = DialogConfigs.FILE_SELECT;
            properties.root = new File(DialogConfigs.DEFAULT_DIR);
            properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
            properties.offset = new File(DialogConfigs.DEFAULT_DIR);
            properties.extensions = new String[]{"apk", "xapk", "apkm", "apks"};
            properties.show_hidden_files = false;

            FilePickerDialog dialog = new FilePickerDialog(MainActivity.this, properties);
            dialog.setTitle("Select apk files");

            dialog.setDialogSelectionListener(new DialogSelectionListener() {
                @Override
                public void onSelectedFilePaths(String[] files) {
                    if (files.length > 0) {
                        APKFiles = files.clone();
                        APKFilesOut = files.clone();

                        FileviewHelper.Apply();
                        ChangeButtonState();
                    }
                }
            });

            dialog.show();
        });
    }

    public void ButtonStartPressed(View view) {
        IsHideDock = SwtichHideDock.isChecked();
        IsRePackage = CheckboxRePackage.isChecked();
        IsRePackageAdv = CheckboxRePackageAdv.isChecked();
        NamePrefix = TextRename.getText().toString();
        IsRename = CheckboxRename.isChecked();
        IsProcessRunning = true;

        FileviewHelper.ClearAllTag();
        ResetAppearance();
        ChangeButtonState();

        MainTask = new Worker().execute(APKFiles);
    }

    public void ButtonClearPressed(View view) {
        String[] empty = new String[]{};

        APKFiles = empty;
        APKFilesOut = empty;

        FileviewHelper.Apply();

        ChangeButtonState();
    }

    public void ButtonCancelPressed(View view) {
        if (!MainTask.isCancelled()) {
            ChangeStateText("## Current Status\nCanceling process please wait...");

            MainTask.cancel(true);

            view.setEnabled(false);
        }
    }

    private class Worker extends AsyncTask<String, String, String> {
        String errorMessage;

        @Override
        protected String doInBackground(String... apkFiles) {
            for (String file : apkFiles) {
                // skip is file error from previous task
                if (file.contains(Utils.FileIndicator.Error))
                    continue;

                ChangeStateText("## Current Status\nCleaning directory...");
                Utils.CleanupTempDir();

                errorMessage = "";

                File dirPico2Dock = new File("storage/emulated/0/Pico2Dock");
                File dirWorker = new File(dirPico2Dock, "Worker");
                File dirUnsign = new File(dirPico2Dock, "Unsign");
                File apkFile = new File(file);
                String apkName = apkFile.getName(); // Including extension
                String filePath = apkFile.getAbsolutePath().replace(apkName, "");
                File dirOut = new File(filePath, "Pico");
                File dirApkOut = new File(dirOut, "Pico_" + apkName);
                File dirApkUnsing = new File(dirUnsign, apkName);

                if (!dirPico2Dock.exists())
                    dirPico2Dock.mkdir();

                Utils.ProgressBar progressBar = new Utils.ProgressBar(apkFiles.length, 5);
                Integer index = Arrays.asList(apkFiles).indexOf(file);

                //?? -------------------- [[ File indicator ]] --------------------
                FileviewHelper.ChangeText(index, Utils.FileIndicator.Working + " " + file);
                FileviewHelper.Select(index);

                //?? -------------------- [[ Check file access ]] --------------------
                if (!apkFile.exists() || !apkFile.isFile() || !apkFile.canRead()) {
                    errorMessage = "Can't access file \"" + apkFile.getPath() + "\"";
                    FileviewHelper.ChangeText(index, Utils.FileIndicator.Error + " " + apkFile.getPath() + " " + Utils.FileIndicator.ErrorInfo + " " + errorMessage);

                    continue;
                }

                //?? -------------------- [[ Convert APKM to APK ]] --------------------
                if (Pattern.matches(".*\\.(xapk|apkm|apks)", file)) {
                    File dirMerger = new File(dirPico2Dock, "Merger");
                    File dirZipper = new File(dirPico2Dock, "Zipper");
                    File dirZipApk = new File(dirZipper, apkName);

                    progressBar.Step += 1;
                    progressBar.Increase(null);

                    try {
                        if (true) { // Clean unnecessary architecture
                            ChangeStateText("## Merger\n**" + apkName + "**\nRemoving unnecessary architecture...");

                            if (!dirZipper.exists())
                                dirZipper.mkdir();
                            if (!dirZipApk.exists())
                                dirZipApk.createNewFile();

                            Files.copy(apkFile.toPath(), dirZipApk.toPath(), StandardCopyOption.REPLACE_EXISTING);

                            final Boolean[] pickArm64v8a = {false};

                            ZipFile zipFile = new ZipFile(dirZipApk);
                            List<FileHeader> fileHeaders = zipFile.getFileHeaders();
                            List<String> filesToRemove = new ArrayList<String>();

                            // Find arm64_v8a
                            fileHeaders.forEach(fileHeader -> {
                                if (Pattern.matches(".*arm64_v8a.*", fileHeader.getFileName()))
                                    pickArm64v8a[0] = true;
                            });

                            // Removing unnecessary architecture
                            fileHeaders.forEach(fileHeader -> {
                                String fileName = fileHeader.getFileName();

                                if (Pattern.matches(".*config\\..{3,}(?<!dpi)\\.apk$", fileName)) { // is architecture file
                                    if (!Pattern.matches(".*arm64_v8a.*", fileName)) { // is not arm64_v8a
                                        if (Pattern.matches(".*armeabi_v7a.*", fileName)) { // is armeabi_v7a
                                            if (pickArm64v8a[0]) // is no arm64_v8a
                                                filesToRemove.add(fileName);
                                        } else
                                            filesToRemove.add(fileName);
                                    }
                                }
                            });
                            zipFile.removeFiles(filesToRemove);

                            // Change APK target to clean file
                            apkFile = dirZipApk;
                        }

                        ChangeStateText("## Merger\nMerging multiple split **" + apkName + "**...");

                        // Start merge split APK
                        String newName = apkName.replaceAll("\\.x?apk[ms]?", ".apk");
                        MergerOptions options = new MergerOptions();
                        options.inputFile = apkFile;
                        options.outputFile = new File(dirMerger, newName);

                        Merger executor = new Merger(options, apkName);
                        executor.runCommand();

                        // Change APK target to merged file
                        apkFile.delete();
                        apkName = newName;
                        apkFile = new File(dirMerger, newName);
                        dirApkOut = new File(dirOut, "Pico_" + apkName);
                        dirApkUnsing = new File(dirUnsign, apkName);
                    } catch (Exception error) {
                        if (isCancelled())
                            break;
                        else {
                            errorMessage = error.toString();
                            FileviewHelper.ChangeText(index, Utils.FileIndicator.Error + " " + apkFile.getPath() + " " + Utils.FileIndicator.ErrorInfo + " " + error.toString());
                            progressBar.Increase(5);

                            continue;
                        }

                    } catch (OutOfMemoryError error) {
                        if (isCancelled())
                            break;
                        else {
                            errorMessage = "Out of memory";
                            FileviewHelper.ChangeText(index, Utils.FileIndicator.Error + " " + apkFile.getPath() + " " + Utils.FileIndicator.ErrorInfo + " " + errorMessage);
                            progressBar.Increase(5);
                            cancel(true);

                            continue;
                        }
                    }
                }

                //?? -------------------- [[ Rename ]] --------------------
                if (dirApkOut.exists()) {
                    int count = 1;
                    while (dirApkOut.exists()) {
                        String newPath = String.format(dirOut + "/Pico_%s (%d).apk", apkName.substring(0, apkName.length() - 4), count);
                        dirApkOut = new File(newPath);
                        count++;
                    }
                    ;
                }

                //?? -------------------- [[ Start decompiler apk ]] --------------------
                if (isCancelled()) break;
                try {
                    ChangeStateText("## Decoder\nDecompiling resources of **" + apkName + "**...");
                    progressBar.Increase(null);

                    DecompileOptions options = new DecompileOptions();
                    options.inputFile = apkFile;
                    options.outputFile = dirWorker;
                    options.loadDex = 10; // 1.4.2++
                    options.noCache = true;
                    options.dex = true;

                    Decompiler executor = new Decompiler(options, apkName);
                    executor.runCommand();
                } catch (Exception error) {
                    if (isCancelled())
                        break;
                    else {
                        errorMessage = "```\n" + error.toString() + "\n```";
                        FileviewHelper.ChangeText(index, Utils.FileIndicator.Error + " " + apkFile.getPath() + " " + Utils.FileIndicator.ErrorInfo + " " + error.toString());
                        progressBar.Increase(4);

                        continue;
                    }
                } catch (OutOfMemoryError error) {
                    if (isCancelled())
                        break;
                    else {
                        errorMessage = "Out of memory";
                        FileviewHelper.ChangeText(index, Utils.FileIndicator.Error + " " + apkFile.getPath() + " " + Utils.FileIndicator.ErrorInfo + " " + errorMessage);
                        progressBar.Increase(4);
                        cancel(true);

                        continue;
                    }
                }

                //?? -------------------- [[ Edit AndroidManifest.xml ]] --------------------
                if (isCancelled()) break;
                try {
                    ChangeStateText("## Current Status\nModifing **AndroidManifest.xml** of **" + apkName + "**...");
                    progressBar.Increase(null);

                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    factory.setNamespaceAware(true);
                    DocumentBuilder builder = factory.newDocumentBuilder();

                    String android = "http://schemas.android.com/apk/res/android";
                    File xmlFile = new File(dirWorker, "/AndroidManifest.xml");
                    Document xmlDoc = builder.parse(xmlFile);
                    Element xmlRoot = xmlDoc.getDocumentElement();
                    Element application = (Element) xmlRoot.getElementsByTagName("application").item(0);

                    //** Adding element [root > application > activity]
                    if (true) {
                        Boolean isPortrait = false;

                        Element vrPosition = xmlDoc.createElement("meta-data");
                        vrPosition.setAttributeNS(android, "android:name", "pico.vr.position");
                        vrPosition.setAttributeNS(android, "android:value", IsHideDock ? "near_dialog" : "near");

                        Element vrPositionOverlay = xmlDoc.createElement("meta-data");
                        vrPositionOverlay.setAttributeNS(android, "android:name", "pico.vr.position.overlay");
                        vrPositionOverlay.setAttributeNS(android, "android:value", "far");

                        Element layout = xmlDoc.createElement("layout");
                        layout.setAttributeNS(android, "android:defaultWidth", "900.0dp");
                        layout.setAttributeNS(android, "android:defaultHeight", isPortrait ? "480.0dp" : "600.0dp");

                        // Add metaData to all activities elements under application
                        for (String tagName : new String[]{"activity", "activity-alias"}) {
                            NodeList activities = application.getElementsByTagName(tagName);

                            for (int itemIndex = 0; itemIndex < activities.getLength(); itemIndex++) {
                                Boolean isMainActivity = false;
                                Element activity = (Element) activities.item(itemIndex);

                                NodeList actions = activity.getElementsByTagName("action");
                                for (int i = 0; i < actions.getLength(); i++) {
                                    if ("android.intent.action.MAIN".equals(((Element) actions.item(i)).getAttributeNS(android, "name"))) {
                                        isMainActivity = true;
                                        break;
                                    }
                                }

                                Element vrMode = xmlDoc.createElement("meta-data");
                                vrMode.setAttributeNS(android, "android:name", "pvr.2dtovr.mode");
                                vrMode.setAttributeNS(android, "android:value", isMainActivity ? "6" : "2");

                                activity.appendChild(vrPosition.cloneNode(true));
                                activity.appendChild(vrPositionOverlay.cloneNode(true));
                                activity.appendChild(vrMode.cloneNode(true));
                                activity.appendChild(layout.cloneNode(true));

                                activity.setAttributeNS(android, "android:resizeableActivity", "true");
                                if (isMainActivity)
                                    activity.setAttributeNS(android, "android:screenOrientation", isPortrait ? "portrait" : "landscape");
                            }
                        }
                    }

                    //** Adding element [root]
                    if (true) {
                        Map<String, String> metaDataMap = new LinkedHashMap<>(); // LinkedHashMap preserves order
                        metaDataMap.put("pvr.2dtovr.mode", "6");
                        metaDataMap.put("pvr.display.orientation", "180");

                        for (Map.Entry<String, String> entry : metaDataMap.entrySet()) {
                            Element metaData = xmlDoc.createElement("meta-data");
                            metaData.setAttributeNS(android, "android:name", entry.getKey());
                            metaData.setAttributeNS(android, "android:value", entry.getValue());
                            xmlRoot.appendChild(metaData);
                        }
                    }

                    //** Adding element [root > application]
                    if (true) {
                        Map<String, String> metaDataMap = new LinkedHashMap<>(); // LinkedHashMap preserves order
                        metaDataMap.put("isPUI", "1");
                        metaDataMap.put("pvr.vrshell.mode", "1");
                        metaDataMap.put("com.pvr.hmd.trackingmode", "6dof");
                        metaDataMap.put("pico_permission_dim_show", "false");
                        metaDataMap.put("pvr.2dtovr.mode", "6");
                        metaDataMap.put("pvr.display.orientation", "180");
                        metaDataMap.put("feature", "2");
                        metaDataMap.put("feature_version", "2");
                        metaDataMap.put("feature.support_custom_panel", "1");
                        metaDataMap.put("channel_id", "PUI");

                        for (Map.Entry<String, String> entry : metaDataMap.entrySet()) {
                            Element metaData = xmlDoc.createElement("meta-data");
                            metaData.setAttributeNS(android, "android:name", entry.getKey());
                            metaData.setAttributeNS(android, "android:value", entry.getValue());
                            application.appendChild(metaData);
                        }
                    }

                    //** Random package name
                    if (IsRePackage) {
                        String packageName = xmlRoot.getAttribute("package");
                        String newPackageName = packageName + "DOCK";

                        // Change package name
                        xmlRoot.setAttribute("package", newPackageName);

                        if (IsRePackageAdv) {
                            String sharedId = xmlRoot.getAttributeNS(android, "sharedUserId");
                            if (sharedId != null && !sharedId.isEmpty()) {
                                xmlRoot.setAttributeNS(android, "android:sharedUserId", sharedId.replace(packageName, newPackageName));
                            }
                        }

                        // Update providers authorities attribute
                        NodeList providers = application.getElementsByTagName("provider");
                        for (int i = 0; i < providers.getLength(); i++) {
                            Element provider = (Element) providers.item(i);
                            String auth = provider.getAttributeNS(android, "authorities");
                            String newAuth = auth.contains(packageName) ? auth.replace(packageName, newPackageName) : auth + "DOCK";
                            provider.setAttributeNS(android, "android:authorities", newAuth);
                        }

                        // Change permissions
                        NodeList permissionsList = xmlRoot.getElementsByTagName("permission");
                        for (int i = 0; i < permissionsList.getLength(); i++) {
                            Element permission = (Element) permissionsList.item(i);
                            String name = permission.getAttributeNS(android, "name");
                            if (IsRePackageAdv) {
                                permission.setAttributeNS(android, "android:name", name.replace(packageName, newPackageName));
                            } else {
                                permission.setAttributeNS(android, "android:name", name + "DOCK");
                            }
                        }

                        NodeList usesPermissionsList = xmlRoot.getElementsByTagName("uses-permission");
                        for (int i = 0; i < usesPermissionsList.getLength(); i++) {
                            Element usesPermission = (Element) usesPermissionsList.item(i);
                            String name = usesPermission.getAttributeNS(android, "name");
                            if (IsRePackageAdv) {
                                usesPermission.setAttributeNS(android, "android:name", name.replace(packageName, newPackageName));
                            } else {
                                usesPermission.setAttributeNS(android, "android:name", name + "DOCK");
                            }
                        }

                        if (IsRePackageAdv) {
                            NodeList activityAliases = application.getElementsByTagName("activity-alias");
                            for (int i = 0; i < activityAliases.getLength(); i++) {
                                Element alias = (Element) activityAliases.item(i);
                                String name = alias.getAttributeNS(android, "name");
                                alias.setAttributeNS(android, "android:name", name.replace(packageName, newPackageName));
                            }
                        }
                    }

                    //** Change app name
                    if (!NamePrefix.isEmpty()) {
                        String app_name = application.getAttributeNS(android, "label");

                        if (IsRename) { // Replace name
                            application.setAttributeNS(android, "android:label", NamePrefix);
                        } else if (!Pattern.matches("@string/.+", app_name)) {
                            application.setAttributeNS(android, "android:label", app_name + NamePrefix);
                        } else { // Append name in resources
                            String stringID = app_name.replace("@string/", "");

                            // Search all directories for "strings.xml"
                            try (Stream<Path> paths = Files.walk(Paths.get(dirWorker.getPath(), "resources"))) {
                                paths.filter(p -> p.getFileName().toString().equals("strings.xml")).forEach(path -> {
                                    try {
                                        File stringXml = path.toFile();
                                        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stringXml);
                                        NodeList tags = doc.getElementsByTagName("string");
                                        boolean modified = false;

                                        // Find the specific element by attribute
                                        for (int i = 0; i < tags.getLength(); i++) {
                                            Element el = (Element) tags.item(i);
                                            if (el.getAttribute("name").equals(stringID)) {
                                                el.setTextContent(el.getTextContent() + NamePrefix);
                                                modified = true;
                                                break; // Stop looking in this file once found
                                            }
                                        }

                                        // Save if changed
                                        if (modified) {
                                            Transformer tf = TransformerFactory.newInstance().newTransformer();
                                            tf.transform(new DOMSource(doc), new StreamResult(stringXml));
                                        }
                                    } catch (Exception ignored) {
                                    }
                                });
                            } catch (Exception ignored) {
                            }
                        }
                    }

                    //** Save the modified AndroidManifest.xml
                    Transformer transManifest = TransformerFactory.newInstance().newTransformer();
                    transManifest.setOutputProperty(OutputKeys.INDENT, "yes");
                    transManifest.transform(new DOMSource(xmlDoc), new StreamResult(xmlFile));
                } catch (Exception error) {
                    if (isCancelled())
                        break;
                    else {
                        errorMessage = "```\n" + error.toString() + "\n```";
                        FileviewHelper.ChangeText(index, Utils.FileIndicator.Error + " " + apkFile.getPath() + " " + Utils.FileIndicator.ErrorInfo + " " + error.toString());
                        progressBar.Increase(3);

                        continue;
                    }
                }

                //?? -------------------- [[ Start compiler apk ]] --------------------
                if (isCancelled()) break;
                try {
                    ChangeStateText("## Encoder\nBuilding **" + apkName + "**...");
                    progressBar.Increase(null);

                    BuildOptions options = new BuildOptions();

                    options.inputFile = dirWorker;
                    options.outputFile = dirApkUnsing;
                    options.type = BuildOptions.TYPE_XML;
                    options.noCache = true;

                    Compiler executor = new Compiler(options, apkName);
                    executor.runCommand();
                } catch (Exception error) {
                    if (isCancelled())
                        break;
                    else {
                        errorMessage = "```\n" + error.toString() + "\n```";
                        FileviewHelper.ChangeText(index, Utils.FileIndicator.Error + " " + apkFile.getPath() + " " + Utils.FileIndicator.ErrorInfo + " " + error.toString());
                        progressBar.Increase(2);

                        continue;
                    }
                } catch (OutOfMemoryError error) {
                    if (isCancelled())
                        break;
                    else {
                        errorMessage = "Out of memory";
                        FileviewHelper.ChangeText(index, Utils.FileIndicator.Error + " " + apkFile.getPath() + " " + Utils.FileIndicator.ErrorInfo + " " + errorMessage);
                        progressBar.Increase(2);
                        cancel(true);

                        continue;
                    }
                }

                //?? -------------------- [[ Start signing apk ]] --------------------
                if (isCancelled()) break;
                try {
                    ChangeStateText("## Signer\nSigning **" + apkName + "**");
                    progressBar.Increase(null);

                    if (!dirOut.exists())
                        dirOut.mkdir();

                    // zipalign
                    File align = new File(dirApkUnsing.getAbsolutePath().replace(dirApkUnsing.getName(), "") + "align_" + dirApkUnsing.getName());
                    ZipAlign.alignApk(dirApkUnsing, align);
                    dirApkUnsing.delete();
                    align.renameTo(dirApkUnsing);

                    // Uber signer
                    String[] arg = new String[]{
                            "sign",
                            "--ks", keystore.getPath(),
                            "--key-pass", "pass:forpico2dock",
                            "--ks-pass", "pass:forpico2dock",
                            "--min-sdk-version", "29",
                            "--max-sdk-version", "29",
                            "--v4-signing-enabled", "false",
                            "--in", dirApkUnsing.getPath(),
                            "--out", dirApkOut.getPath(),
                    };
                    ApkSignerTool.main(arg);
                    File idsig = new File(dirApkOut, ".idsig");
                    idsig.delete();
                } catch (Exception error) {
                    if (isCancelled())
                        break;
                    else {
                        errorMessage = "```\n" + error.toString() + "\n```";
                        FileviewHelper.ChangeText(index, Utils.FileIndicator.Error + " " + apkFile.getPath() + " " + Utils.FileIndicator.ErrorInfo + " " + error.toString());
                        progressBar.Increase(1);

                        continue;
                    }
                }

                progressBar.Increase(null);
                FileviewHelper.ChangeText(index, Utils.FileIndicator.Success + " " + file);
                APKFilesOut[index] = dirApkOut.getPath();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Utils.CleanupTempDir();

            if (errorMessage != null && !errorMessage.isEmpty()) {
                PercentText.setText("Error");

                ChangeStateText("## ERROR\n\n" + errorMessage);
            } else {
                PercentText.setText("Successful");

                ChangeStateText("## Current Status\nAll files have been modified.\n* The APK files are in the Pico folder by the same directory as the original file.\n* Long click file in the box above to see the options.");
            }

            StatusProgressBar.setProgress(100);
            IsProcessRunning = false;
            ChangeButtonState();
        }

        @Override
        protected void onCancelled() {
            PercentText.setText("Terminated");

            Utils.CleanupTempDir();

            ChangeStateText("## Current Status\nProcess has been terminated.");

            StatusProgressBar.setProgress(100);
            IsProcessRunning = false;
            ChangeButtonState();
        }
    }

    //** UI
    private void ChangeButtonState() {
        if ((APKFiles != null && APKFiles.length > 0) && !IsProcessRunning)
            ButtonStart.setEnabled(true);
        else
            ButtonStart.setEnabled(false);

        if (IsProcessRunning && !ButtonStart.isEnabled())
            ButtonCancel.setEnabled(true);
        else
            ButtonCancel.setEnabled(false);

        if ((APKFiles != null && APKFiles.length > 0) && !ButtonCancel.isEnabled())
            ButtonClear.setEnabled(true);
        else
            ButtonClear.setEnabled(false);

        if (APKFiles != null && APKFiles.length > 0)
            TextViewSelectHint.setVisibility(View.GONE);
        else
            TextViewSelectHint.setVisibility(VISIBLE);

        SwtichHideDock.setEnabled(!IsProcessRunning);
        CheckboxRePackage.setEnabled(!IsProcessRunning);
        CheckboxRePackageAdv.setEnabled(!IsProcessRunning);
        TextRename.setEnabled(!IsProcessRunning);
        CheckboxRename.setEnabled(!IsProcessRunning);
    }

    public void ChangeStateText(String text) {
        TextView statusText = (TextView) findViewById(R.id.StatusText);
        final Markwon markwon = Markwon.create(this);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                markwon.setMarkdown(statusText, text);
            }
        });
    }

    public void OpenGithubPage(View view) {
        Uri uri = Uri.parse("https://github.com/chaixshot/Pico2DockAndroid");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    private void ResetAppearance() {
        StatusProgressBar.setProgress(0);
        StatusProgressBar.setVisibility(View.INVISIBLE);
        PercentText.setText("");
    }

    //** Permission
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 112) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                PermissionHelper.WritePermissionGranted();
        }
    }

    //** Context menu
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add("Install");
        menu.add("Remove");
        menu.add("Delete");
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        assert info != null;
        Context _this = this;

        String apkPath = APKFiles[info.position];
        String apkOutPath = APKFilesOut[info.position];

        File apkFile = new File(Utils.FileIndicator.ClearTag(apkPath));
        File apkOutFile = new File(apkOutPath);

        Boolean isConverted = apkPath.contains(Utils.FileIndicator.Success) && apkOutFile.exists();
        File apkTargetFile = isConverted ? apkOutFile : apkFile;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (IsProcessRunning) {
            builder.setTitle("");
            builder.setMessage("Can't do this action while processing");

            builder.setPositiveButton("Close", (dialog, which) -> {
                dialog.dismiss();
            });
        } else {
            //?? Install
            if (item.getTitle() == "Install") {

                builder.setTitle("Do you want to install?");
                builder.setMessage(apkTargetFile.getPath());

                builder.setPositiveButton("YES", (dialog, which) -> {
                    try {
                        PermissionHelper.AskInstallPermission();

                        // Create Uri
                        Uri apkUri = getUriForFile(_this, getPackageName(), apkTargetFile);

                        // Intent to open apk
                        Intent intent = new Intent(Intent.ACTION_VIEW, apkUri);
                        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    } catch (Settings.SettingNotFoundException e) {
                        ChangeStateText("## ERROR\n\n" + e);
                    }

                    dialog.dismiss();
                }).setNegativeButton("NO", (dialog, which) -> dialog.dismiss());
            }

            //?? Remove
            if (item.getTitle() == "Remove") {
                builder.setTitle("Do you want to remove?");
                builder.setMessage(apkTargetFile.getPath());

                builder.setPositiveButton("YES", (dialog, which) -> {
                    FileviewHelper.RemoveByIndex(info.position);

                    ChangeButtonState();
                    dialog.dismiss();
                }).setNegativeButton("NO", (dialog, which) -> dialog.dismiss());
            }

            //?? Delete
            if (item.getTitle() == "Delete") {
                builder.setTitle("Do you want to delete?");
                builder.setMessage(apkTargetFile.getPath());

                builder.setPositiveButton("YES", (dialog, which) -> {
                    if (isConverted)
                        FileviewHelper.ClearTag(info.position);
                    else
                        FileviewHelper.RemoveByIndex(info.position);

                    ChangeButtonState();
                    apkTargetFile.delete();

                    if (isConverted) {
                        File dirPico = new File(apkTargetFile.getPath().replace(apkTargetFile.getName(), ""));
                        if (dirPico.listFiles().length == 0)
                            dirPico.delete();
                    }

                    dialog.dismiss();
                }).setNegativeButton("NO", (dialog, which) -> dialog.dismiss());
            }
        }

        AlertDialog alert = builder.create();
        alert.show();

        return super.onContextItemSelected(item);
    }

    public void ButtonHelpOpen(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Help");
        builder.setMessage("⬤ Hold point any element to see its tooltip including files in the box.");

        builder.setPositiveButton("Close", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog alert = builder.create();
        alert.show();
    }
}
