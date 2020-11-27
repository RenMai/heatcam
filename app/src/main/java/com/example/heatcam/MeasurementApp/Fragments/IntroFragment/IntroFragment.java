package com.example.heatcam.MeasurementApp.Fragments.IntroFragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SizeF;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.heatcam.MeasurementApp.FaceDetector.CameraXViewModel;
import com.example.heatcam.MeasurementApp.FaceDetector.IntroFaceDetectorProcessor;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.LowResolution16BitCamera;
import com.example.heatcam.MeasurementApp.Main.MainActivity;
import com.example.heatcam.MeasurementApp.Fragments.Measurement.MeasurementStartFragment;
import com.example.heatcam.R;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialPort.SerialPortModel;
import com.example.heatcam.MeasurementApp.FaceDetector.VisionImageProcessor;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.face.FaceDetectorOptions;

public class IntroFragment extends Fragment {

    private final String TAG = "IntroFragment";

    private VisionImageProcessor imageProcessor;
    private ProcessCameraProvider cameraProvider;
    private CameraSelector cameraSelector;
    private ImageAnalysis analysisCase;

    private float focalLength;
    private float sensorX;
    private float sensorY;

    private final int AVERAGE_EYE_DISTANCE = 63; // in mm

    private int minDistanceToMeasure = 500;
    //private TextView txtV;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.heatcam_intro_fragment, container, false);
        SharedPreferences sharedPrefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        minDistanceToMeasure = Integer.parseInt(sharedPrefs.getString("PREFERENCE_MEASURE_START_MIN_DISTANCE", "500"));
        view.setKeepScreenOn(true);

       // txtV = view.findViewById(R.id.txtDist);

        //moving background
        ConstraintLayout constraintLayout = (ConstraintLayout) view.findViewById(R.id.ConstraintLayout);
        AnimationDrawable animationDrawable = (AnimationDrawable) constraintLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();

        getCameraProperties();
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getActivity().getApplication()))
                .get(CameraXViewModel.class)
                .getProcessCameraProvider()
                .observe(
                        getViewLifecycleOwner(),
                        provider -> {
                            cameraProvider = provider;
                            bindAllCameraUseCases();
                        }
                );
        try {
            checkCamera(view.getContext());
        } catch (Exception e) {
            e.printStackTrace();
        }


        return view;
    }

    private void checkCamera(Context context) {
        SerialPortModel serialPortModel = SerialPortModel.getInstance();
        if(!serialPortModel.hasCamera()) {
            SharedPreferences sharedPrefs = getActivity().getPreferences(Context.MODE_PRIVATE);
            LowResolution16BitCamera cam = new LowResolution16BitCamera();
            cam.setMaxFilter(sharedPrefs.getFloat(getString(R.string.preference_max_filter), -1));
            cam.setMinFilter(sharedPrefs.getFloat(getString(R.string.preference_min_filter), -1));
            serialPortModel.setSioListener(cam);
            serialPortModel.scanDevices(context);
            serialPortModel.changeTiltSpeed(7);
        } else {
            serialPortModel.changeTiltAngle(75);
        }

    }

    private void getCameraProperties() {
        CameraManager manager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics c = manager.getCameraCharacteristics(getFrontFacingCameraId(manager));
            focalLength = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)[0];
            SizeF sensor = c.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
            float angleX = (float) Math.atan(sensor.getWidth() / (2*focalLength));
            float angleY = (float) Math.atan(sensor.getHeight() / (2*focalLength));
            System.out.println("fov" + angleX + angleY);
            sensorX = (float) (Math.tan(Math.toRadians(angleX / 2)) * 2 * focalLength);
            sensorY = (float) (Math.tan(Math.toRadians(angleY / 2)) * 2 * focalLength);
            System.out.println("leng" + focalLength);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    String getFrontFacingCameraId(CameraManager cManager) throws CameraAccessException {
        for(final String cameraId : cManager.getCameraIdList()){
            CameraCharacteristics characteristics = cManager.getCameraCharacteristics(cameraId);
            int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
            if(cOrientation == CameraCharacteristics.LENS_FACING_FRONT) return cameraId;
        }
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        bindAllCameraUseCases();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
    }

    private void bindAllCameraUseCases() {
        if (cameraProvider != null) {
            // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
            cameraProvider.unbindAll();
            bindFaceAnalysisUseCase();
        }
    }

    private void bindFaceAnalysisUseCase() {
        if (cameraProvider == null) {
            return;
        }
        if (analysisCase != null) {
            cameraProvider.unbind(analysisCase);
        }
        if (imageProcessor != null) {
            imageProcessor.stop();
        }

        try {
            FaceDetectorOptions faceDetectOptions = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setMinFaceSize(0.40f)
                    .enableTracking()
                    .build();

            imageProcessor = new IntroFaceDetectorProcessor(getContext(), faceDetectOptions, this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        analysisCase = new ImageAnalysis.Builder()
                .setTargetResolution(new Size( 1, 1))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        analysisCase.setAnalyzer(
                ContextCompat.getMainExecutor(getContext()),
                imageProxy -> {
                    try {
                        imageProcessor.processImageProxy(imageProxy);
                    } catch (MlKitException e) {
                        Log.e(TAG, "Failed to process image. Error: " + e.getLocalizedMessage());
                    }
                }
        );

        cameraProvider.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector, analysisCase);

    }

    public void checkFaceDistance(PointF leftEye, PointF rightEye, int imgWidth, int imgHeight) {
        float deltaX = Math.abs(leftEye.x - rightEye.x);
        float deltaY = Math.abs(leftEye.y - rightEye.y);

        float dist = 0f;
        if (deltaX >= deltaY) {
            dist = focalLength * (AVERAGE_EYE_DISTANCE / sensorX) * (imgWidth / deltaX) / 100;
        } else {
            dist = focalLength * (AVERAGE_EYE_DISTANCE / sensorY) * (imgHeight / deltaY) / 100;
        }

//        float finalDist = dist;
//        getActivity().runOnUiThread(() -> txtV.setText("D: " + finalDist));

        if (dist > 0 && dist < minDistanceToMeasure) {
            switchToMeasurementStartFragment();
        }
    }

    private void switchToMeasurementStartFragment() {

        Fragment f = new MeasurementStartFragment();
        getActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.slide_in_left, R.animator.slide_in_right, 0, 0)
                .replace(R.id.fragmentCamera, f, "measure_start")
                .commit();
        MainActivity.setAutoMode(true);
    }
}
