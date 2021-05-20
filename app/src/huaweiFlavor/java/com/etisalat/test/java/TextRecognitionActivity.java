/**
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.etisalat.test.java;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;
import androidx.core.content.ContextCompat;

import com.etisalat.test.Constants;
import com.etisalat.test.R;
import com.etisalat.test.camera.CameraConfiguration;
import com.etisalat.test.camera.FrameMetadata;
import com.etisalat.test.camera.LensEngine;
import com.etisalat.test.camera.LensEnginePreview;
import com.etisalat.test.transactor.BaseTransactor;
import com.etisalat.test.views.overlay.GraphicOverlay;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.mlsdk.MLAnalyzerFactory;
import com.huawei.hms.mlsdk.common.MLFrame;
import com.huawei.hms.mlsdk.text.MLLocalTextSetting;
import com.huawei.hms.mlsdk.text.MLText;
import com.huawei.hms.mlsdk.text.MLTextAnalyzer;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public final class TextRecognitionActivity extends AppCompatActivity
        implements OnRequestPermissionsResultCallback {

    private static final String TAG = "TextRecognitionActivity";
    private LensEngine lensEngine = null;
    private LensEnginePreview preview;
    private GraphicOverlay graphicOverlay;

    CameraConfiguration cameraConfiguration = null;
    private int facing = CameraConfiguration.CAMERA_FACING_BACK;

    private LocalTextTransactor localTextTransactor;
    private Handler mHandler = new MsgHandler(this);

    private static final int CAMERA_PERMISSION_CODE = 1;

    private boolean isInitialization = false;

    private static class MsgHandler extends Handler {
        WeakReference<TextRecognitionActivity> mMainActivityWeakReference;

        public MsgHandler(TextRecognitionActivity mainActivity) {
            this.mMainActivityWeakReference = new WeakReference<>(mainActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            TextRecognitionActivity mainActivity = this.mMainActivityWeakReference.get();
            if (mainActivity == null) {
                return;
            }
            Log.d(TextRecognitionActivity.TAG, "msg what :" + msg.what);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setStatusBarColor(Color.BLACK);
        this.setContentView(R.layout.activity_text_recognition);
        if (savedInstanceState != null) {
            this.facing = savedInstanceState.getInt(Constants.CAMERA_FACING);
        }
        this.preview = this.findViewById(R.id.live_preview);
        this.graphicOverlay = this.findViewById(R.id.live_overlay);
        this.cameraConfiguration = new CameraConfiguration();
        this.cameraConfiguration.setCameraFacing(this.facing);

        this.createLensEngine();

        // Check whether the app has the camera permission.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
            // The app has the camera permission.
        //...
        } else {
            // Apply for the camera permission.
            requestCameraPermission();
        }
    }

    private void requestCameraPermission() {
        final String[] permissions = new String[]{Manifest.permission.CAMERA};
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, CAMERA_PERMISSION_CODE);
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,@NonNull int[] grantResults) {
        if (requestCode != CAMERA_PERMISSION_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }
        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // The camera permission is granted.
        }
    }

    @Override
    public void onBackPressed() {
            super.onBackPressed();
            releaseLensEngine();
    }

    private void createLensEngine() {
        if (this.lensEngine == null) {
            this.lensEngine = new LensEngine(this, this.cameraConfiguration, this.graphicOverlay);
        }
        try {
            this.localTextTransactor = new LocalTextTransactor(this.mHandler, this); //create

            this.lensEngine.setMachineLearningFrameTransactor(this.localTextTransactor);
            
            isInitialization = true;
        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Can not create image transactor: " + e.getMessage(),
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void startLensEngine() {
        if (this.lensEngine != null) {
            try {
                this.preview.start(this.lensEngine);
            } catch (IOException e) {
                Log.e(TextRecognitionActivity.TAG, "Unable to start lensEngine.", e);
                this.lensEngine.release();
                this.lensEngine = null;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isInitialization){
           createLensEngine();
        }
        this.startLensEngine();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.preview.stop();
    }

    private void releaseLensEngine() {
        if (this.lensEngine != null) {
            this.lensEngine.release();
            this.lensEngine = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseLensEngine();
    }

    private void customLayout(String text){
        // Set the toast and duration
        int toastDurationInMilliSeconds = 700;
        LayoutInflater layoutInflater = getLayoutInflater();
        View layout = layoutInflater.inflate(R.layout.custom_toast,(ViewGroup) findViewById(R.id.toast_root));
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

    public class LocalTextTransactor extends BaseTransactor<MLText> {
        private final MLTextAnalyzer detector;

        public LocalTextTransactor(Handler handler, Context context) {
            MLLocalTextSetting options = new MLLocalTextSetting.Factory()
                    .setOCRMode(MLLocalTextSetting.OCR_TRACKING_MODE)
                    .setLanguage("en")
                    .create();
            this.detector = MLAnalyzerFactory.getInstance().getLocalTextAnalyzer(options);
        }

        @Override
        public void stop() {
            try {
                this.detector.close();
            } catch (IOException e) {
               // Log.e(com.huawei.mlkit.sample.transactor.LocalTextTransactor.TAG,"Exception thrown while trying to close text transactor: " + e.getMessage());
            }
        }

        @Override
        protected Task<MLText> detectInImage(MLFrame image) {
            return this.detector.asyncAnalyseFrame(image);
        }

        @Override
        protected void onSuccess(
                @NonNull Bitmap originalCameraImage,
                @NonNull MLText results,
                @NonNull FrameMetadata frameMetadata,
                @NonNull GraphicOverlay graphicOverlay) {

            //this.latestImageMetaData = frameMetadata;
            graphicOverlay.clear();
            List<MLText.Block> blocks = results.getBlocks();
            for (int i = 0; i < blocks.size(); i++) {
                List<MLText.TextLine> lines = blocks.get(i).getContents();
                for (int j = 0; j < lines.size(); j++) {
                    // Display by line, without displaying empty lines.
                    if (lines.get(j).getStringValue() != null &&
                            lines.get(j).getStringValue().trim().length() != 0) {
                        String numbersOnly= lines.get(j).getStringValue().replaceAll("[^0-9]", "");
                        if (numbersOnly.length() == 16){


//                            //Get the Rectangle/BoundingBox of the word
//                            //RectF rect = new RectF(currentword.BoundingBox);
//                            RectF rectF = new RectF(lines.get(j).getBorder());
//                            Drawable drawable = ContextCompat.getDrawable(getApplicationContext(),R.drawable.text_recognition_rectangle);
//                            assert drawable != null;
//                            Rect yourRect = new Rect();
//                            yourRect.left = drawable.getBounds().left;
//                            yourRect.top = drawable.getBounds().top;
//                            yourRect.bottom = drawable.getBounds().bottom;
//                            yourRect.right = drawable.getBounds().right;
//                            RectF rectF2 = new RectF();
//                            rectF2.set(yourRect);
                            //rectF.intersect(rectF2);
                            // Check if the word boundingBox is inside the area required
//                            if (rectF.intersect(rectF2)){
//                                Toast.makeText(TextRecognitionActivity.this, "Inside", Toast.LENGTH_SHORT).show();
////                                Log.d("Inside","Inside Message");
//                            }else {
//                                //Log.d("Outside","Outside Message");
//                            }
                            // using: rect.intersect(yourRect);
                            //put the word in a filtered list...

                                // make space after 4 digits
                                String numbersToShow = numbersOnly.replaceAll("....(?!$)", "$0 ");
                                customLayout(numbersToShow);
                                final Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Do something after 700ms
                                        Intent intent = new Intent(TextRecognitionActivity.this, TextRecognitionHomeActivity.class);
                                        intent.putExtra("EXTRA_Scratch_ID", numbersOnly);
                                        startActivity(intent);
                                    }
                                }, 700);
                                stop();
                        }
                    }
                }
            }


//            //MLText.Block //TextBlock
//            List<MLText.Block> blocks_ = new ArrayList<>();
//            MLText.Block myItem = null;
//            for (int i = 0; i < blocks.size(); ++i)
//            {
//                myItem = (MLText.Block)blocks.get(i);
//
//                //Add All TextBlocks to the `blocks` List
//                blocks_.add(myItem);
//
//            }
//            List<MLText.TextLine> textLines = null;
//            //Loop through each `Block`
//            for (MLText.Block textBlock : blocks_)
//            {
//                //IList<IText> textLines = textBlock.Components;
//                textLines = textBlock.getContents();
//            }
//
//            //loop Through each `Line`
//            for (MLText.TextLine currentLine : textLines)
//            {
//                List<MLText.Word>  words = currentLine.getContents();
//                //Loop through each `Word`
//                for (MLText.Word currentword : words)
//                {
//                    //Get the Rectangle/BoundingBox of the word
//                    //RectF rect = new RectF(currentword.BoundingBox);
//                    RectF rectF = new RectF(currentword.getBorder());
//                    Drawable drawable = ContextCompat.getDrawable(getApplicationContext(),R.drawable.text_recognition_rectangle);
//                    assert drawable != null;
//                    Rect yourRect = drawable.getBounds();
////                    rect.left = drawable.getBounds().left;
////                    rect.top = drawable.ge;
////                    rect.bottom = drawable.getBottom();
////                    rect.right = drawable.getRight();
//                    RectF rectF2 = new RectF(yourRect);
//                    rectF.intersect(rectF2);
//                    // Check if the word boundingBox is inside the area required
//
//                    // using: rect.intersect(yourRect);
//                    //put the word in a filtered list...
//                }
//            }
//            //It continues doing other things here

        }

        @Override
        protected void onFailure(@NonNull Exception e) {
            //Log.e(com.huawei.mlkit.sample.transactor.LocalTextTransactor.TAG, "Text detection failed: " + e.getMessage());
        }
    }
}
