package com.example.heatcam;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;

import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.List;

public class FaceDetectionTool {


    FaceDetectorOptions options =
            new FaceDetectorOptions.Builder()
                    .setClassificationMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .setMinFaceSize(0.15f)
                    .enableTracking()
                    .build();

    FaceDetector faceDetector;
    LiveCameraActivity a;

    public FaceDetectionTool(LiveCameraActivity a) {
        faceDetector = FaceDetection.getClient(options);
        this.a = a;
    }

    public void processImage(InputImage image, ImageProxy imageProxy) {

        Task<List<Face>> result =
                faceDetector.process(image)
                        .addOnSuccessListener(
                                faces -> {

                                    // Task completed successfully
                                    // [START_EXCLUDE]
                                    // [START get_face_info]
                                    if (faces.size() > 0) {

                                        for (Face face : faces) {
                                            System.out.println(face.getAllContours() + " CONTOUR");
                                            System.out.println(face.getAllLandmarks() + " LANDMARK");
                                            Rect bounds = face.getBoundingBox();

                                            Bitmap b = image.getBitmapInternal();
                                            Canvas canvas = new Canvas(b);
                                            Paint paint = new Paint();
                                            paint.setColor(Color.GREEN);
                                            paint.setStyle(Paint.Style.STROKE);
                                            paint.setStrokeWidth(30);
                                            canvas.drawRect(faces.get(0).getBoundingBox(), paint);
                                            a.drawImage(b);

                                            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
                                            float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees

                                            // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                                            // nose available):
                                            FaceLandmark leftEar = face.getLandmark(FaceLandmark.LEFT_EAR);
                                            if (leftEar != null) {
                                                PointF leftEarPos = leftEar.getPosition();
                                            }

                                            // If classification was enabled:
                                            if (face.getSmilingProbability() != null) {
                                                float smileProb = face.getSmilingProbability();
                                            }
                                            if (face.getRightEyeOpenProbability() != null) {
                                                float rightEyeOpenProb = face.getRightEyeOpenProbability();
                                            }

                                            // If face tracking was enabled:
                                            if (face.getTrackingId() != null) {
                                                int id = face.getTrackingId();
                                            }
                                        }
                                    } else {
                                        a.drawImage(image.getBitmapInternal());
                                    }
                                    // [END get_face_info]
                                    // [END_EXCLUDE]
                                    System.out.println("SUCCESS");
                                    imageProxy.close();
                                })
                        .addOnFailureListener(
                                e -> {

                                    // Task failed with an exception
                                    // ...
                                    System.out.println("FAILURE");
                                    System.out.println(e.getMessage());
                                    e.printStackTrace();
                                    imageProxy.close();
                                });
    }
}
