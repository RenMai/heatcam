package com.example.heatcam.MeasurementApp.FaceDetector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.heatcam.MeasurementApp.Fragments.IntroFragment.IntroFragment;
import com.example.heatcam.MeasurementApp.FaceDetector.VisionProcessorBase;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.List;

public class IntroFaceDetectorProcessor extends VisionProcessorBase<List<Face>> {

    private static final String TAG = "IntroFaceDetectorProcessor";

    private final FaceDetector detector;

    private IntroFragment introFrag;

    public IntroFaceDetectorProcessor(Context context, FaceDetectorOptions options, IntroFragment introFrag) {
        super(context);
        Log.v(MANUAL_TESTING_LOG, "Intro Face detector options: " + options);
        detector = FaceDetection.getClient(options);
        this.introFrag = introFrag;
    }

    @Override
    public void stop() {
        super.stop();
        detector.close();
    }

    @Override
    protected Task<List<Face>> detectInImage(InputImage image) {
        return detector.process(image);
    }

    @Override
    protected void onSuccess(@NonNull List<Face> results, Bitmap originalCameraImage) {
        if (results.size() > 0) {
            Face face = results.get(0);
            PointF leftEyeP = face.getLandmark(FaceLandmark.LEFT_EYE).getPosition();
            PointF rightEyeP = face.getLandmark(FaceLandmark.RIGHT_EYE).getPosition();
            introFrag.checkFaceDistance(leftEyeP, rightEyeP, originalCameraImage.getWidth(), originalCameraImage.getHeight());
        }

    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Face detection failed " + e);
    }
}
