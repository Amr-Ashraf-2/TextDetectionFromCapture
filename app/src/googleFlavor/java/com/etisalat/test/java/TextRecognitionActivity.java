package com.etisalat.test.java;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;
import androidx.core.content.ContextCompat;

import com.etisalat.test.CameraSource;
import com.etisalat.test.CameraSourcePreview;
import com.etisalat.test.R;
import com.etisalat.test.GraphicOverlay;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class TextRecognitionActivity extends AppCompatActivity
        implements OnRequestPermissionsResultCallback {
    private static final String OBJECT_DETECTION = "Object Detection";

    private CameraSource cameraSource = null;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private final String selectedModel = OBJECT_DETECTION;
    private static final String TAG = "LivePreviewActivity";
    private static final int PERMISSION_REQUESTS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_text_recognition);

        preview = findViewById(R.id.preview_view);
        if (preview == null) {
            Log.d(TAG, "Preview is null");
        }
        graphicOverlay = findViewById(R.id.graphic_overlay);
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null");
        }

        if (allPermissionsGranted()) {
            createCameraSource(selectedModel);
        } else {
            getRuntimePermissions();
        }
    }

    private void createCameraSource(String model) {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = new CameraSource(this, graphicOverlay);
        }

        try {
            Log.i(TAG, "Using on-device Text recognition Processor");
            cameraSource.setMachineLearningFrameProcessor(new TextRecognitionProcessor(this));
        } catch (RuntimeException e) {
            Log.e(TAG, "Can not create image processor: " + model, e);
            Toast.makeText(
                    getApplicationContext(),
                    "Can not create image processor: " + e.getMessage(),
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    /**
     * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Log.d(TAG, "resume: Preview is null");
                }
                if (graphicOverlay == null) {
                    Log.d(TAG, "resume: graphOverlay is null");
                }
                preview.start(cameraSource, graphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        createCameraSource(selectedModel);
        startCameraSource();
    }

    /** Stops the camera. */
    @Override
    protected void onPause() {
        super.onPause();
        preview.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
        }
    }

    private String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                    this.getPackageManager()
                            .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }


    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "Permission granted!");
        if (allPermissionsGranted()) {
            createCameraSource(selectedModel);
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission granted: " + permission);
            return true;
        }
        Log.i(TAG, "Permission NOT granted: " + permission);
        return false;
    }

    private void customLayout(String text){
        // Set the toast and duration
        int toastDurationInMilliSeconds = 700;
        LayoutInflater layoutInflater = getLayoutInflater();
        View layout = layoutInflater.inflate(R.layout.custom_toast, findViewById(R.id.toast_root));
        Drawable background = layout.getBackground();
        background.setAlpha(235);
        final TextView textView = layout.findViewById(R.id.toast_text);
        textView.setText(text);
        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.CENTER,0,0);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        // Set the countdown to display the toast
        CountDownTimer toastCountDown;
        toastCountDown = new CountDownTimer(toastDurationInMilliSeconds, 1000 /*Tick duration*/) {
            public void onTick(long millisUntilFinished) {
                toast.show();
            }
            public void onFinish() {
                toast.cancel();
            }
        };
        // Show the toast and starts the countdown
        toast.show();
        toastCountDown.start();
    }

    public class TextRecognitionProcessor extends VisionProcessorBase<Text> {

        private static final String TAG = "TextRecProcessor";

        private final TextRecognizer textRecognizer;

        public TextRecognitionProcessor(Context context) {
            super(context);
            textRecognizer = TextRecognition.getClient();
        }

        @Override
        public void stop() {
            super.stop();
            textRecognizer.close();
        }

        @Override
        protected Task<Text> detectInImage(InputImage image) {
            return textRecognizer.process(image);
        }

        @Override
        protected void onSuccess(@NonNull Text text, @NonNull GraphicOverlay graphicOverlay) {

            Log.d(TAG, "On-device Text detection successful");

            logExtrasForTesting(text);

        }

        private void logExtrasForTesting(Text text) {
            if (text != null) {

                Log.v(MANUAL_TESTING_LOG, "Detected text has : " + text.getTextBlocks().size() + " blocks");

                for (int i = 0; i < text.getTextBlocks().size(); ++i) {
                    List<Text.Line> lines = text.getTextBlocks().get(i).getLines();

                    Log.v(
                            MANUAL_TESTING_LOG,
                            String.format("Detected text block %d has %d lines", i, lines.size()));

                    for (int j = 0; j < lines.size(); ++j) {
                        List<Text.Element> elements = lines.get(j).getElements();

                        lines.get(j).getText();
                        if (lines.get(j).getText().trim().length() != 0) {
                            String numbersOnly= lines.get(j).getText().replaceAll("[^0-9]", "");
                            if (numbersOnly.length() == 16){
                                // make space after 4 digits
                                String numbersToShow = numbersOnly.replaceAll("....(?!$)", "$0 ");
                                customLayout(numbersToShow);
                                final Handler handler = new Handler();
                                handler.postDelayed(() -> {
                                    // Do something after 700ms
                                    Intent intent = new Intent(TextRecognitionActivity.this, TextRecognitionHomeActivity.class);
                                    intent.putExtra("EXTRA_Scratch_ID", numbersOnly);
                                    startActivity(intent);
                                }, 700);
                                stop();
                            }
                        }

                        Log.v(
                                MANUAL_TESTING_LOG,
                                String.format("Detected text line %d has %d elements", j, elements.size()));

                    }
                }
            }
        }

        @Override
        protected void onFailure(@NonNull Exception e) {
            Log.w(TAG, "Text detection failed." + e);
        }
    }



}
