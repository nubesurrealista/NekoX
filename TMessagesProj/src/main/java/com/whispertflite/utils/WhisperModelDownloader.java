package com.whispertflite.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.SharedConfig;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import moe.hx030.momogram.MomoConfig;
import moe.hx030.momogram.helpers.WhisperHelper;


@SuppressWarnings("ResultOfMethodCallIgnored")
public class WhisperModelDownloader {
    static final String modelMultiLingualBase = "whisper-base.TOP_WORLD.tflite";
    static final String modelMultiLingualSmall = "whisper-small.TOP_WORLD.tflite";
    static final String modelMultiLingualBaseURL = "https://huggingface.co/DocWolle/whisper_tflite_models/resolve/main/whisper-base.TOP_WORLD.tflite";
    static final String modelMultiLingualSmallURL = "https://huggingface.co/DocWolle/whisper_tflite_models/resolve/main/whisper-small.TOP_WORLD.tflite";
    static final String modelMultiLingualBaseMD5 = "9e43f385a916ac4b2e48760ce1fa70fc";
    static final String modelMultiLingualSmallMD5 = "d3badbb86c9bcc7312c19167acac7133";
    static final long modelMultiLingualBaseSize = 107564368;
    static final long modelMultiLingualSmallSize = 307408944;
    static long downloadModelMultiLingualBaseSize = 0L;
    static long downloadModelMultiLingualSmallSize = 0L;
    static boolean modelMultiLingualBaseFinished = false;
    static boolean modelMultiLingualSmallFinished = false;

    static final File extDir = ApplicationLoader.applicationContext.getExternalFilesDir(null);

    public static boolean modelExists() {
        File modelMultiLingualBaseFile = new File(extDir + "/" + modelMultiLingualBase);
        File modelMultiLingualSmallFile = new File(extDir + "/" + modelMultiLingualSmall);
        if (!modelMultiLingualBaseFile.exists() || !modelMultiLingualSmallFile.exists()) {
            return true;   //update available
        } else {
            return false;  //no update
        }
    }

    public static boolean deleteModels() {
        boolean ok = false, useSlow = MomoConfig.useSlowWhisperModel.Bool(), modelInUse = false;
        File base = new File(extDir + "/" + modelMultiLingualBase);
        File small = new File(extDir + "/" + modelMultiLingualSmall);
        for (int acc : SharedConfig.activeAccounts) {
            if (WhisperHelper.useLocalModel(acc)) {
                modelInUse = true;
                break;
            }
        }
        if (useSlow || !modelInUse) {
            if (base.exists()) {
                base.delete();
                ok = true;
            }
        }
        if (!useSlow || !modelInUse) {
            if (small.exists()) {
                small.delete();
                ok = true;
            }
        }
        return ok;
    }

    public static boolean checkModels() {
        copyAssetsToSdcard();
        File modelMultiLingualBaseFile = new File(extDir + "/" + modelMultiLingualBase);
        File modelMultiLingualSmallFile = new File(extDir + "/" + modelMultiLingualSmall);
        String calcModelMultiLingualBaseMD5 = "";
        String calcModelMultiLingualSmallMD5 = "";
        if (modelMultiLingualBaseFile.exists()) {
            try {
                calcModelMultiLingualBaseMD5 = calculateMD5(modelMultiLingualBaseFile.getPath());
            } catch (IOException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        if (modelMultiLingualSmallFile.exists()) {
            try {
                calcModelMultiLingualSmallMD5 = calculateMD5(modelMultiLingualSmallFile.getPath());
            } catch (IOException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        if (modelMultiLingualBaseFile.exists() && !(calcModelMultiLingualBaseMD5.equals(modelMultiLingualBaseMD5))) { modelMultiLingualBaseFile.delete(); modelMultiLingualBaseFinished = false;}
        if (modelMultiLingualSmallFile.exists() && !(calcModelMultiLingualSmallMD5.equals(modelMultiLingualSmallMD5))) { modelMultiLingualSmallFile.delete(); modelMultiLingualSmallFinished = false;}

        boolean useSlowModel = MomoConfig.useSlowWhisperModel.Bool();
        return (!useSlowModel || calcModelMultiLingualSmallMD5.equals(modelMultiLingualSmallMD5)) && (useSlowModel || calcModelMultiLingualBaseMD5.equals(modelMultiLingualBaseMD5));
    }

    public static void downloadModels(BiConsumer<Boolean, Float> progressCallback) {
        if (checkModels()) {
            progressCallback.accept(true, 0f);
            return;
        }

        Thread baseThread, smallThread;
        File modelMultiLingualBaseFile = new File(ApplicationLoader.applicationContext.getExternalFilesDir(null)+ "/" + modelMultiLingualBase);
        if (!MomoConfig.useSlowWhisperModel.Bool()) {
            if (!modelMultiLingualBaseFile.exists()) {
                modelMultiLingualBaseFinished = false;
                Log.d("WhisperASR", "multi-lingual base model file does not exist");
                baseThread = new Thread(() -> {
                    modelMultiLingualBaseFinished = downloadModel(modelMultiLingualBaseURL, modelMultiLingualBaseFile, 1, progressCallback);
                });
                baseThread.start();
            } else {
                baseThread = null;
                downloadModelMultiLingualBaseSize = modelMultiLingualBaseSize;
                modelMultiLingualBaseFinished = true;
            }
        } else {
            baseThread = null;
        }

        File modelMultiLingualSmallFile = new File(ApplicationLoader.applicationContext.getExternalFilesDir(null)+ "/" + modelMultiLingualSmall);
        if (MomoConfig.useSlowWhisperModel.Bool()) {
            if (!modelMultiLingualSmallFile.exists()) {
                modelMultiLingualSmallFinished = false;
                Log.d("WhisperASR", "multi-lingual small model file does not exist");
                smallThread = new Thread(() -> {
                    modelMultiLingualSmallFinished = downloadModel(modelMultiLingualSmallURL, modelMultiLingualSmallFile, 2, progressCallback);
                });
                smallThread.start();
            } else {
                smallThread = null;
                downloadModelMultiLingualSmallSize = modelMultiLingualSmallSize;
                modelMultiLingualSmallFinished = true;
            }
        } else {
            smallThread = null;
        }

        if (!modelMultiLingualSmallFinished || !modelMultiLingualBaseFinished) {
            new Thread(() -> {
                try {
                    if (baseThread != null) baseThread.join();
                    if (smallThread != null) smallThread.join();
                } catch (InterruptedException ignore) {}

                progressCallback.accept(modelMultiLingualSmallFinished || modelMultiLingualBaseFinished, 1f);
            }).start();
        }
        // skip english only model
    }

    public static boolean downloadModel(String urlString, File modelFile, int type, BiConsumer<Boolean ,Float> progressCallback) {
        boolean ret = false;
        try {
            URL url = new URL(urlString);
            Log.d("WhisperASR", "Download model from " + urlString);

            URLConnection ucon = url.openConnection();
            ucon.setReadTimeout(5000);
            ucon.setConnectTimeout(10000);

            InputStream is = ucon.getInputStream();
            BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);

            modelFile.createNewFile();

            FileOutputStream outStream = new FileOutputStream(modelFile);
            byte[] buff = new byte[5 * 1024];

            int len;
            while ((len = inStream.read(buff)) != -1) {
                outStream.write(buff, 0, len);
                if (modelFile.exists()) {
                    if (type == 1)
                        downloadModelMultiLingualBaseSize = modelFile.length();
                    else
                        downloadModelMultiLingualSmallSize = modelFile.length();
                }
                if (type == 1) {
                    progressCallback.accept(false, (float) downloadModelMultiLingualBaseSize / (float) modelMultiLingualBaseSize);
                } else {
                    progressCallback.accept(false, (float) downloadModelMultiLingualSmallSize / (float) modelMultiLingualSmallSize);
                }
            }
            outStream.flush();
            outStream.close();
            inStream.close();
            String md5 = "";
            if (modelFile.exists()) {
                md5 = calculateMD5(modelFile.getPath());
            } else {
                throw new IOException();  //throw exception if there is no modelMultiLingualSmallFile at this point
            }

            if ((type == 1 && !(md5.equals(modelMultiLingualBaseMD5))) || (type == 2 && !md5.equals(modelMultiLingualSmallMD5))){
                modelFile.delete();
            } else {
                ret = true;
            }
        } catch (NoSuchAlgorithmException | IOException i) {
            modelFile.delete();
        }
        return ret;
    }

    public static String calculateMD5(String filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = new BufferedInputStream(new FileInputStream(filePath))) {
            byte[] buffer = new byte[8192]; // 8KB buffer
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }
        byte[] hash = md.digest();
        return new BigInteger(1, hash).toString(16);
    }

    // Copy assets to destination folder
    public static void copyAssetsToSdcard() {
        Context context = ApplicationLoader.applicationContext;
        String[] extensions = {"bin"};
        File sdcardDataFolder = context.getExternalFilesDir(null);
        AssetManager assetManager = context.getAssets();

        try {
            // List all files in the assets folder once
            String[] assetFiles = assetManager.list("");
            if (assetFiles == null) return;

            for (String assetFileName : assetFiles) {
                // Check if file matches any of the provided extensions
                for (String extension : extensions) {
                    if (assetFileName.endsWith("." + extension)) {
                        File outFile = new File(sdcardDataFolder, assetFileName);

                        // Skip if file already exists
                        if (outFile.exists()) break;

                        // Copy the file from assets to the destination folder
                        try (InputStream inputStream = assetManager.open(assetFileName);
                             OutputStream outputStream = new FileOutputStream(outFile)) {

                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                        }
                        break; // No need to check further extensions
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}