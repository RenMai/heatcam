package com.example.heatcam;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;

public class MeasurementStartFragment extends Fragment implements CameraListener {

    private final int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};

    private final int IMAGE_WIDTH = 480;
    private final int IMAGE_HEIGHT = 640;

    private final String TAG = "MeasurementStartFragment";

    private int facePositionCheckCounter = 0;
    private final int checkLimit = 10;

    private final int AVERAGE_EYE_DISTANCE = 63; // in mm

    private MutableLiveData<Integer> detectedFrames = new MutableLiveData<>();

    private float focalLength;
    private float sensorX;
    private float sensorY;

    private PreviewView cameraFeed;
    private boolean found = false;

    private VisionImageProcessor imageProcessor;
    private ProcessCameraProvider cameraProvider;
    private CameraSelector cameraSelector;
    private ImageAnalysis analysisCase;
    private Preview previewCase;

    private float preferred_measure_distance;

    private Animation scanAnimation;
    private Button animBtn;
    private View scanBar;

    private boolean ready;
    private HuippuLukema huiput = new HuippuLukema();
    private Rect naamarajat;
    float korkeussuhde = (float) LeptonCamera.getHeight() / (float) IMAGE_HEIGHT;//32/640
    float leveyssuhde = (float) LeptonCamera.getWidth() / (float) IMAGE_WIDTH;//24/480

    private double userTemp = 0;
    private int laskuri = 0;
    private boolean hasMeasured = false;

    private TextView txtDebug;

    SerialPortModel serialPortModel;

    private MeasurementAccessObject measurementAccessObject;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferred_measure_distance = Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(getContext())
                .getString(getString(R.string.preference_measure_distance), "300"));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.measurement_start_layout, container, false);
        // prevent app from dimming
        view.setKeepScreenOn(true);
        animBtn = view.findViewById(R.id.animBtn);
        scanBar = view.findViewById(R.id.scanBar);
        createFaceOval(view);
        cameraFeed = view.findViewById(R.id.measurement_position_video);

        measurementAccessObject = new MeasurementAccessObject();

        // takes approx. 2 minutes to go from 1000 to 10
        detectedFrames.setValue(1000);

        txtDebug = view.findViewById(R.id.txtDebugit);

        serialPortModel = SerialPortModel.getInstance();
        serialPortModel.setCamListener(this);

        scanAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.scan_animation);

        scanAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                scanBar.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        animBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                scanBar.setVisibility(View.VISIBLE);
                scanBar.startAnimation(scanAnimation);
            }
        });

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


        ProgressBar bar = view.findViewById(R.id.face_check_prog);
        bar.setMax(checkLimit);

        detectedFrames.observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                // getActivity().runOnUiThread(() -> txtDebug.setText(String.valueOf(integer)));
                //System.out.println(integer + " jees");
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
                .setTargetResolution(new Size(IMAGE_WIDTH, IMAGE_HEIGHT))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        analysisCase.setAnalyzer(
                // imageProcessor.processImageProxy will use another thread to run the detection underneath,
                // thus we can just runs the analyzer itself on main thread.
                ContextCompat.getMainExecutor(getContext()),
                imageProxy -> {
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
            float angleX = (float) Math.atan(sensor.getWidth() / (2 * focalLength));
            float angleY = (float) Math.atan(sensor.getHeight() / (2 * focalLength));
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
        //paint.setColor(Color.parseColor("#871019FF"));
        paint.setShader(new LinearGradient(0, 0, 600, 960, Color.parseColor("#871019FF"), Color.parseColor("#85FB3A1F"), Shader.TileMode.MIRROR));
        paint.setShader(new LinearGradient(600, 960, 1200, 1920, Color.parseColor("#85FB3A1F"), Color.parseColor("#871019FF"), Shader.TileMode.MIRROR));
        paint.setAlpha(230);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        Rect rt = new Rect(0, 0, overlay.getWidth(), overlay.getHeight());
        canvas.drawRect(rt, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        float x = overlay.getWidth() / 2.0f;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(view.getContext());
        float width = Float.parseFloat(sp.getString(getString(R.string.preference_oval_width), "750")) / 2;
        float height = Float.parseFloat(sp.getString(getString(R.string.preference_oval_height), "1300"));
        float fromTop = Float.parseFloat(sp.getString(getString(R.string.preference_oval_pos_from_top), "200"));
        canvas.drawOval(x - width, fromTop, x + width, height, paint);
        ((ImageView) view.findViewById(R.id.start_layout_oval_overlay)).setImageBitmap(overlay);
    }

    String getFrontFacingCameraId(CameraManager cManager) throws CameraAccessException {
        for (final String cameraId : cManager.getCameraIdList()) {
            CameraCharacteristics characteristics = cManager.getCameraCharacteristics(cameraId);
            int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (cOrientation == CameraCharacteristics.LENS_FACING_FRONT) return cameraId;
        }
        return null;
    }

    public void facePositionCheck(Face face, int imgWidth, int imgHeight) {

        float middleX = imgWidth / 2f;
        float middleY = imgHeight / 2.05f; // joutuu sit säätää tabletille tää ja deviation
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

        boolean distanceOK = dist < preferred_measure_distance + offset && dist > preferred_measure_distance - offset;
        if (xOK && yOK && distanceOK) {
            facePositionCheckCounter++;
            startScanAnimation();
            ready = true;
        } else {
            scanBar.clearAnimation();
            ready = false;
            laskuri = 0;
            userTemp = 0;
            huiput = new HuippuLukema();
            facePositionCheckCounter--;
            if (facePositionCheckCounter < 0) facePositionCheckCounter = 0;
        }
        updateProgress();

    }

    private void updateProgress() {
        try {
            ProgressBar bar = getActivity().findViewById(R.id.face_check_prog);
            bar.setProgress(facePositionCheckCounter);
        } catch (Exception ignored) {

        }
    }

    public void incrementDetectedFrames(Face face) {
        naamarajat = face.getBoundingBox();
        if (detectedFrames.getValue() > 1000) {
            detectedFrames.setValue(1000);
        } else {
            detectedFrames.setValue(detectedFrames.getValue() + 1);
        }
    }

    public void decrementDetectedFrames() {
        scanBar.clearAnimation();
        if (detectedFrames.getValue() < 0) {
            detectedFrames.setValue(0);
        } else {
            detectedFrames.setValue(detectedFrames.getValue() - 1);
        }
    }

    private void changeToResultLayout() {
        Fragment f = new QR_code_fragment();
        Bundle args = new Bundle();
        args.putDouble("user_temp", userTemp);
        f.setArguments(args);
        getActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.slide_in_left, R.animator.slide_in_right, 0, 0)
                .replace(R.id.fragmentCamera, f, "default").commit();
    }


    private void startScanAnimation() {

        if (!ready) {
            scanBar.setVisibility(View.VISIBLE);
            scanBar.bringToFront();
            scanBar.invalidate();
            scanBar.startAnimation(scanAnimation);
        }
    }

    private void saveMeasurementToJson() {
        System.out.println();
        try {
            JSONObject obj = measurementAccessObject.newEntry(userTemp, new Date());
            measurementAccessObject.write(getContext(), obj, true);
        } catch (JSONException | IOException e) {
            Log.e(TAG, "Something went wrong while saving measurement to JSON " + e);
            e.printStackTrace();
        }
    }

    @Override
    public void setConnectingImage() {
    }

    @Override
    public void setNoFeedImage() {
    }

    @Override
    public void updateImage(Bitmap image) {

    }

    @Override
    public void updateText(String text) {
    }

    @Override
    public void disconnect() {
    }

    @Override
    public void maxCelsiusValue(double max) {
        // getActivity().runOnUiThread(() -> txtDebug.setText(String.valueOf(laskuri)));
        if (ready) {
            if (laskuri < 100) {
                huiput = laskeAlue();
                if (huiput.max > userTemp) {
                    userTemp = huiput.max;
                }
                laskuri++;
            } else {
                ready = false;
                laskuri = 0;
                //hasMeasured = true;
                saveMeasurementToJson();
                changeToResultLayout();
            }
        }
    }

    @Override
    public void minCelsiusValue(double min) {
    }

    @Override
    public void detectFace(Bitmap image) {
    }

    @Override
    public void writeToFile(byte[] data) {
    }

    public HuippuLukema laskeAlue() {

        int maxleveys = LeptonCamera.getWidth() - 1;
        int maxkorkeus = LeptonCamera.getHeight() - 1;

        int vasen = (int) (naamarajat.left * leveyssuhde);
        if (vasen < 0) vasen = 0;
        if (vasen > maxleveys) vasen = maxleveys;
        int oikea = (int) (naamarajat.right * leveyssuhde);
        if (oikea < 0) oikea = 0;
        if (oikea > maxleveys) oikea = maxleveys;
        int yla = (int) (naamarajat.top * korkeussuhde);
        if (yla < 0) yla = 0;
        if (yla > maxkorkeus) yla = maxkorkeus;
        int ala = (int) (naamarajat.bottom * korkeussuhde);
        if (ala < 0) ala = 0;
        if (ala > maxkorkeus) ala = maxkorkeus;

        int[][] tempFrame = LeptonCamera.getTempFrame();

        try {
            if (tempFrame != null /*&& tempFrame.length > maxkorkeus && tempFrame[tempFrame.length-1].length > maxleveys*/) {
                for (int y = yla; y <= ala; y++) {
                    for (int x = vasen; x <= oikea; x++) {
                        double lampo = (tempFrame[y][x] - 27315) / 100.0;
                        if (lampo > huiput.max) {
                            huiput.max = lampo;
                            huiput.y = y;
                            huiput.x = x;
                        }
                    }
                }
            }
        } catch (Exception e) {

        }
        return huiput;
    }

    class HuippuLukema {
        int x = 0;
        int y = 0;
        double max = 0;
    }
}
