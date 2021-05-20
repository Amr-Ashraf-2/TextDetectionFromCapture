/**
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.etisalat.test.util;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera.CameraInfo;
import android.util.Log;

import androidx.annotation.Nullable;

import com.etisalat.test.camera.FrameMetadata;
import com.huawei.hms.mlsdk.common.MLFrame;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class BitmapUtils {
    private static final String TAG = "BitmapUtils";

    /**
     * Convert nv21 format byte buffer to bitmap
     *
     * @param data data
     * @param metadata metadata
     * @return Bitmap object
     */
    @Nullable
    public static Bitmap getBitmap(ByteBuffer data, FrameMetadata metadata) {
        data.rewind();
        byte[] imageBuffer = new byte[data.limit()];
        data.get(imageBuffer, 0, imageBuffer.length);
        try {
            YuvImage yuvImage = new YuvImage(imageBuffer, ImageFormat.NV21, metadata.getWidth(),
                    metadata.getHeight(), null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, metadata.getWidth(), metadata.getHeight()), 80, stream);
            Bitmap bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
            stream.close();
            return rotateBitmap(bitmap, metadata.getRotation(), metadata.getCameraFacing());
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }
        return null;
    }


    public static Bitmap rotateBitmap(Bitmap bitmap, int rotation, int facing) {
        Matrix matrix = new Matrix();
        int rotationDegree = 0;
        if (rotation == MLFrame.SCREEN_SECOND_QUADRANT) {
            rotationDegree = 90;
        } else if (rotation == MLFrame.SCREEN_THIRD_QUADRANT) {
            rotationDegree = 180;
        } else if (rotation == MLFrame.SCREEN_FOURTH_QUADRANT) {
            rotationDegree = 270;
        }
        matrix.postRotate(rotationDegree);
        if (facing != CameraInfo.CAMERA_FACING_BACK) {
            matrix.postScale(-1.0f, 1.0f);
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}

