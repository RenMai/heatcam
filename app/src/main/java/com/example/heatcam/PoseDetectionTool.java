package com.example.heatcam;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.Image;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseDetectorOptions;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.List;

public class PoseDetectionTool {

    PoseDetectorOptions options =
            new PoseDetectorOptions.Builder()
                    .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                    .setPerformanceMode(PoseDetectorOptions.PERFORMANCE_MODE_FAST)
                    .build();
    PoseDetector poseDetector;

    LiveCameraActivity a;

    public PoseDetectionTool (LiveCameraActivity a) {
        poseDetector = PoseDetection.getClient(options);
        this.a = a;
    }

    public void processImage(InputImage image) {

        Task<Pose> result =
                poseDetector.process(image)
                        .addOnSuccessListener(
                                pose -> {
                                    PoseLandmark rightIndex = pose.getPoseLandmark(PoseLandmark.Type.LEFT_WRIST);
                                    PoseLandmark rightElbow = pose.getPoseLandmark(PoseLandmark.Type.LEFT_ELBOW);
                                    PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.Type.LEFT_SHOULDER);
                                    if (rightIndex != null && rightElbow != null && rightShoulder != null) {
                                        //System.out.println("pose = rightIndex : "+ rightIndex.getPosition() + " | Likelihood: " + rightIndex.getInFrameLikelihood());
                                        //System.out.println("pose = rightElbow : "+ rightElbow .getPosition() + " | Likelihood: " + rightElbow.getInFrameLikelihood());
                                       // System.out.println("pose = rightShoul : "+ rightShoulder.getPosition() + " | Likelihood: " + rightShoulder.getInFrameLikelihood());
                                        Bitmap b = image.getBitmapInternal();
                                        /*
                                        float points[] = {
                                                rightShoulder.getPosition().x,
                                                rightShoulder.getPosition().y,
                                                rightElbow.getPosition().x,
                                                rightElbow.getPosition().y,
                                                rightIndex.getPosition().x,
                                                rightIndex.getPosition().y
                                        };
*/


                                        Canvas canvas = new Canvas(b);
                                        Paint paint = new Paint();
                                        paint.setColor(Color.GREEN);
                                        paint.setStrokeWidth(30);
                                        //canvas.drawLines(points, paint);
                                        canvas.drawLine(rightShoulder.getPosition().x, rightShoulder.getPosition().y,
                                                rightElbow.getPosition().x, rightElbow.getPosition().y, paint);
                                        canvas.drawLine(rightElbow.getPosition().x, rightElbow.getPosition().y,
                                                rightIndex.getPosition().x, rightIndex.getPosition().y, paint);
                                        a.drawImage(image.getBitmapInternal());

                                    } else {
                                        a.drawImage(image.getBitmapInternal());
                                    }
                                })
                        .addOnFailureListener(
                                e -> {
                                    // Task failed with an exception
                                    // ...
                                });


    }


}
