package com.whispertflite;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.whispertflite.asr.Whisper;
import com.whispertflite.asr.WhisperResult;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

import java.io.File;

import tw.nekomimi.nekogram.NekoConfig;

public class WhisperRecognitionService {
    private static final String TAG = "WhisperRecognitionSvc";
    public static final WhisperRecognitionService instance = new WhisperRecognitionService();

    // whisper-small.tflite works well for multi-lingual
    public static final String MULTI_LINGUAL_EU_MODEL_FAST = "whisper-base.EUROPEAN_UNION.tflite";
    public static final String MULTI_LINGUAL_TOP_WORLD_FAST = "whisper-base.TOP_WORLD.tflite";
    public static final String MULTI_LINGUAL_TOP_WORLD_SLOW = "whisper-small.TOP_WORLD.tflite";
    public static final String MULTI_LINGUAL_MODEL_FAST = "whisper-base.tflite";
    public static final String MULTI_LINGUAL_MODEL_SLOW = "whisper-small.tflite";
    public static final String ENGLISH_ONLY_MODEL = "whisper-tiny.en.tflite";
    // English only model ends with extension ".en.tflite"
    public static final String ENGLISH_ONLY_MODEL_EXTENSION = ".en.tflite";
    public static final String ENGLISH_ONLY_VOCAB_FILE = "filters_vocab_en.bin";
    public static final String MULTILINGUAL_VOCAB_FILE = "filters_vocab_multilingual.bin";
    private Whisper mWhisper = null;
    private File sdcardDataFolder = null;
    private File selectedTfliteFile = null;
    private boolean recognitionCancelled = false;


    protected void onCancel(Callback callback) {
        Log.d(TAG,"cancel");
        deinitModel();
        recognitionCancelled = true;
    }

    // Model initialization
    private void initModel(File modelFile, Callback callback) {
        boolean isMultilingualModel = !(modelFile.getName().endsWith(ENGLISH_ONLY_MODEL_EXTENSION));
        String vocabFileName = isMultilingualModel ? MULTILINGUAL_VOCAB_FILE : ENGLISH_ONLY_VOCAB_FILE;
        File vocabFile = new File(sdcardDataFolder, vocabFileName);

        if (mWhisper == null) {
            mWhisper = new Whisper();
            mWhisper.loadModel(modelFile, vocabFile, isMultilingualModel);
            Log.d(TAG, "Initialized: " + modelFile.getName());
            mWhisper.setLanguage(-1); // auto
        }
        mWhisper.setListener(new Whisper.WhisperListener() {
            @Override
            public void onUpdateReceived(String message) {
                Log.d("030-Whisper", "msg: " + message);
                if (message.startsWith("ERR:"))
                    callback.apply(null, message.replace("ERR:", ""));
            }

            @Override
            public void onResultReceived(WhisperResult whisperResult) {
                if (!whisperResult.getResult().trim().isEmpty()) {
                    Log.d(TAG, whisperResult.getResult().trim());
                    deinitModel();
                    String result = whisperResult.getResult();
                    Log.d(TAG, "result = " + result);
                    callback.apply(result, null);
                } else {
                    callback.apply(null, "ERR_NO_SPEECH");
                }
            }
        });
    }

    public void startTranscription(float[] samples, Callback callback) {
        sdcardDataFolder = ApplicationLoader.applicationContext.getExternalFilesDir(null);
        selectedTfliteFile = new File(sdcardDataFolder, NekoConfig.useSlowWhisperModel.Bool() ?
                MULTI_LINGUAL_TOP_WORLD_SLOW : MULTI_LINGUAL_TOP_WORLD_FAST);

        if (!selectedTfliteFile.exists()) {
            callback.apply(null, "ERR_NO_MODEL");
            return;
        }

        initModel(selectedTfliteFile, callback);

        if (!recognitionCancelled) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(()-> {
                Toast toast = new Toast(ApplicationLoader.applicationContext);
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setText(LocaleController.getString(R.string.WhisperLocalProcessing));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    toast.addCallback(new Toast.Callback() {
                        @Override
                        public void onToastHidden() {
                            super.onToastHidden();
                            if (mWhisper != null) toast.show();
                        }
                    });
                }
                toast.show();
            });
            mWhisper.setAction(Whisper.ACTION_TRANSCRIBE);
            mWhisper.processSamples(samples);
            mWhisper.start();
            Log.d(TAG,"Start Transcription");
        }
    }

    public void onDestroy() {
        deinitModel();
    }
    private void deinitModel() {
        if (mWhisper != null) {
            mWhisper.unloadModel();
            mWhisper = null;
        }
    }


    public interface Callback {
        void apply(String result, String error);
    }
}
