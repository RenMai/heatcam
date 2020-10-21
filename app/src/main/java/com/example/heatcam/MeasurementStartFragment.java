package com.example.heatcam;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SizeF;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MeasurementStartFragment extends Fragment {

    private Executor executor = Executors.newSingleThreadExecutor();
    private final int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};

    private final String TAG = "MeasurementStartFragment";

    private int facePositionCheckCounter = 0;
    private final int checkLimit = 10;

    private final int AVERAGE_EYE_DISTANCE = 63; // in mm

    private MutableLiveData<Integer> detectedFrames = new MutableLiveData<>();

    private float focalLength;
    private float sensorX;
    private float sensorY;

    private PreviewView cameraFeed;
    private RenderScriptTools rs;
    private boolean found = false;

    private VisionImageProcessor imageProcessor;
    private ProcessCameraProvider cameraProvider;
    private CameraSelector cameraSelector;
    private ImageAnalysis analysisCase;
    private Preview previewCase;

    private float preferred_measure_distance;

    private FaceDetector faceDetector;
    private FaceDetectorOptions options =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .build();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferred_measure_distance = Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(getContext()).getString(getString(R.string.preference_measure_distance), "300"));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.measurement_start_layout, container, false);
        // prevent app from dimming
        view.setKeepScreenOn(true);
        createFaceOval(view);
        rs = new RenderScriptTools(view.getContext());
        cameraFeed = view.findViewById(R.id.measurement_position_video);
        faceDetector = FaceDetection.getClient(options);

        // takes approx. 2 minutes to go from 1000 to 10
        detectedFrames.setValue(1000);

        if (allPermissionsGranted()) {
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

            //startCamera();
        } else {
            getActivity().requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        ProgressBar bar = view.findViewById(R.id.face_check_prog);
        bar.setMax(checkLimit);

        detectedFrames.observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                if (integer < 10) {
                    Fragment f = new MenuFragment();
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.animator.slide_in_left, R.animator.slide_in_right, 0, 0)
                            .replace(R.id.fragmentCamera, f, "menu").commit();
                }
            }
        });

        return view;
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
            bindPreviewUseCase();
            bindFacePosAnalysisUseCase();
        }
    }

    private void bindPreviewUseCase() {
        if (cameraProvider == null) {
            return;
        }
        if (previewCase != null) {
            cameraProvider.unbind(previewCase);
        }

        previewCase = new Preview.Builder().build();
        previewCase.setSurfaceProvider(cameraFeed.createSurfaceProvider());
        cameraProvider.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector, previewCase);
    }

    private void bindFacePosAnalysisUseCase() {
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

            FaceDetectorOptions options =
                    new FaceDetectorOptions.Builder()
                            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                            .build();

            imageProcessor = new FacePositionProcessor(getContext(), options, this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        analysisCase = new ImageAnalysis.Builder()
                .setTargetResolution(new Size( 1, 1))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        analysisCase.setAnalyzer(
                // imageProcessor.processImageProxy will use another thread to run the detection underneath,
                // thus we can just runs the analyzer itself on main thread.
                ContextCompat.getMainExecutor(getContext()),
                imageProxy -> {
                    /*
                    if (needUpdateGraphicOverlayImageSourceInfo) {
                        boolean isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT;
                        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                        if (rotationDegrees == 0 || rotationDegrees == 180) {
                            graphicOverlay.setImageSourceInfo(
                                    imageProxy.getWidth(), imageProxy.getHeight(), isImageFlipped);
                        } else {
                            /*
                            graphicOverlay.setImageSourceInfo(
                                    imageProxy.getHeight(), imageProxy.getWidth(), isImageFlipped);
                        }
                    }
                     */
                    try {
                        imageProcessor.processImageProxy(imageProxy);
                    } catch (MlKitException e) {
                        Log.e(TAG, "Failed to process image. Error: " + e.getLocalizedMessage());

                    }
                });

        cameraProvider.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector, analysisCase);
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

    private void createFaceOval(View view) {
        Bitmap overlay = Bitmap.createBitmap(1200, 1920, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(overlay);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setAlpha(150);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        Rect rt = new Rect(0,0, overlay.getWidth(), overlay.getHeight());
        canvas.drawRect(rt, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        float x = overlay.getWidth() / 2.0f;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(view.getContext());
        float width = Float.parseFloat(sp.getString(getString(R.string.preference_oval_width), "750"))/ 2;
        float height = Float.parseFloat(sp.getString(getString(R.string.preference_oval_height), "1300"));
        float fromTop = Float.parseFloat(sp.getString(getString(R.string.preference_oval_pos_from_top), "200"));;
        canvas.drawOval(x-width,  fromTop, x+width, height, paint);
        ((ImageView)view.findViewById(R.id.start_layout_oval_overlay)).setImageBitmap(overlay);
    }

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {

            }
        }, ContextCompat.getMainExecutor(getContext()));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

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

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size( 1, 1))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        imageAnalysis.setAnalyzer(executor, new MyAnalyzer());


        preview.setSurfaceProvider(cameraFeed.createSurfaceProvider());
        Camera cam = cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);


    }

    String getFrontFacingCameraId(CameraManager cManager) throws CameraAccessException {
        for(final String cameraId : cManager.getCameraIdList()){
            CameraCharacteristics characteristics = cManager.getCameraCharacteristics(cameraId);
            int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
            if(cOrientation == CameraCharacteristics.LENS_FACING_FRONT) return cameraId;
        }
        return null;
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    public void processImage(InputImage image, ImageProxy imageProxy) {

        Task<List<Face>> result =
                faceDetector.process(image)
                        .addOnSuccessListener(
                                faces -> {

                                    if (faces.size() > 0) {
                                        Face face = faces.get(0);
                                        facePositionCheck(face, image.getWidth(), image.getHeight());

                                    } else {

                                    }
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

    public synchronized void facePositionCheck(Face face, int imgWidth, int imgHeight) {

        float middleX = imgWidth/2f;
        float middleY = imgHeight/2.05f; // joutuu sit säätää tabletille tää ja deviation
        float maxDeviation = 15f; // eli max +- pixel heitto sijaintiin
        PointF noseP = face.getLandmark(FaceLandmark.NOSE_BASE).getPosition();
        PointF leftEyeP = face.getLandmark(FaceLandmark.LEFT_EYE).getPosition();
        PointF rightEyeP = face.getLandmark(FaceLandmark.RIGHT_EYE).getPosition();

        float deltaX = Math.abs(leftEyeP.x - rightEyeP.x);
        float deltaY = Math.abs(leftEyeP.y - rightEyeP.y);

        float dist = 0f;
        if (deltaX >= deltaY) {
            dist = focalLength * (AVERAGE_EYE_DISTANCE / sensorX) * (imgWidth / deltaX) / 100;
        } else {
            dist = focalLength * (AVERAGE_EYE_DISTANCE / sensorY) * (imgHeight / deltaY) / 100;
        }

        System.out.println(dist + " dist");

        boolean xOK = noseP.x > (middleX - maxDeviation) && noseP.x < (middleX + maxDeviation);
        boolean yOK = noseP.y > (middleY - maxDeviation) && noseP.y < (middleY + maxDeviation);

        int offset = 50;

        boolean distanceOK = dist < preferred_measure_distance+offset && dist > preferred_measure_distance-offset;
        if (xOK && yOK && distanceOK) {
            facePositionCheckCounter++;
            updateProgress();
            if (facePositionCheckCounter > checkLimit && !found) {
                found = true;
                Fragment f = new User_result();
                getActivity().getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.animator.slide_in_left, R.animator.slide_in_right, 0, 0)
                            .replace(R.id.fragmentCamera, f, "default").commit();
            }

        } else {
            facePositionCheckCounter--;
            if(facePositionCheckCounter < 0) facePositionCheckCounter = 0;
            updateProgress();
        }

    }

    private void updateProgress() {
        try {
            ProgressBar bar = getActivity().findViewById(R.id.face_check_prog);
            bar.setProgress(facePositionCheckCounter);
        } catch (Exception ignored) {

        }
    }

    public void incrementDetectedFrames() {
        if (detectedFrames.getValue() > 1000) {
            detectedFrames.setValue(1000);
        } else {
            detectedFrames.setValue(detectedFrames.getValue() + 1);
        }
    }

    public void decrementDetectedFrames() {
        if (detectedFrames.getValue() < 0) {
            detectedFrames.setValue(0);
        } else {
            detectedFrames.setValue(detectedFrames.getValue() - 1);
        }
    }

    private class MyAnalyzer implements ImageAnalysis.Analyzer {

        @Override
        public void analyze(@NonNull ImageProxy image) {
            int rotationDegrees = image.getImageInfo().getRotationDegrees();
            @SuppressLint("UnsafeExperimentalUsageError") Image img = image.getImage();
            if (img != null) {

                Bitmap bMap = rs.YUV_420_888_toRGB(img, img.getWidth(), img.getHeight(), rotationDegrees);

                InputImage inputImage = InputImage.fromBitmap(bMap, 0);
                processImage(inputImage, image); // face detection
            }
        }
    }
}
