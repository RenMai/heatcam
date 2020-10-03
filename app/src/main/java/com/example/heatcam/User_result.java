package com.example.heatcam;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.drawable.AnimationDrawable;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class User_result extends Fragment implements CameraListener {

    // the value could be used of user temperature when userTemp is 100/real 39C etc.
    private double userTemp = 0, correcTemp = 0;
    double temp = 0;

    private Button buttonStart3;
    private TextView text, text2, textDistance, textMeasuring;
    private ImageView imgView;
    private boolean ready = false;
    private boolean hasMeasured = false;

    private int laskuri = 0;
    private int progress = 0;
    ProgressBar vProgressBar;
    SerialPortModel serialPortModel;

    private Executor executor = Executors.newSingleThreadExecutor();

    private RenderScriptTools rs;
    private FaceDetectTool fTool;

    private final int AVERAGE_EYE_DISTANCE = 63; // in mm
    private final int IMAGE_WIDTH = 480;
    private final int IMAGE_HEIGHT = 640;

    private float focalLength;
    private float sensorX;
    private float sensorY;

    private AsyncTask tempMeasureTask;

    private MutableLiveData<Face> detectedFace = new MutableLiveData<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.activity_user_result, container, false);

        ConstraintLayout constraintLayout = (ConstraintLayout) view.findViewById(R.id.linearLayout);
        AnimationDrawable animationDrawable = (AnimationDrawable) constraintLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();


        //new asyncTaskUpdateProgress().execute();
        serialPortModel = SerialPortModel.getInstance();
        serialPortModel.setCamListener(this);
        buttonStart3 = view.findViewById(R.id.start3);
        text = view.findViewById(R.id.textView);
        text2 = view.findViewById(R.id.textView2);
        text2.setText(R.string.otsikko);
        textDistance = view.findViewById(R.id.textDistance);
        textMeasuring = view.findViewById(R.id.textMeasuring);
        imgView = view.findViewById(R.id.imageView);
        vProgressBar = view.findViewById(R.id.vprogressbar3);
        rs = new RenderScriptTools(view.getContext());
        fTool = new FaceDetectTool(this);

        android.hardware.Camera cam = getFrontCam();
        android.hardware.Camera.Parameters camP = cam.getParameters();
        focalLength = camP.getFocalLength();
        float angleX = camP.getHorizontalViewAngle();
        float angleY = camP.getVerticalViewAngle();
        sensorX = (float) (Math.tan(Math.toRadians(angleX / 2)) * 2 * focalLength);
        sensorY = (float) (Math.tan(Math.toRadians(angleY / 2)) * 2 * focalLength);
        cam.stopPreview();
        cam.release();

        startCamera();

        buttonStart3.setOnClickListener(v -> {
            ready = true;
            userTemp = 0;
        });

        detectedFace.observe(getViewLifecycleOwner(), new Observer<Face>() {
            @Override
            public void onChanged(Face face) {
                int fId = face.getTrackingId();
                PointF leftEyeP = face.getLandmark(FaceLandmark.LEFT_EYE).getPosition();
                PointF rightEyeP = face.getLandmark(FaceLandmark.RIGHT_EYE).getPosition();
                float faceDistance = calculateFaceDistance(leftEyeP, rightEyeP);

                if (!ready && !hasMeasured) {
                    userTemp = 0;
                }

                if (faceDistance < 300 && !hasMeasured) {
                    getActivity().runOnUiThread(() -> textDistance.setText("Distance: " + faceDistance));
                    ready = true;
                } else if (!hasMeasured) {
                    ready = false;
                    progress = 0;
                    laskuri = 0;
                    if (tempMeasureTask != null) {
                        tempMeasureTask.cancel(true);
                    }
                    getActivity().runOnUiThread(() -> textDistance.setText("Come closer.\nDistance: " + faceDistance));
                    getActivity().runOnUiThread(() -> textMeasuring.setText("Measuring temp: FALSE"));
                }
            }
        });


        return view;
    }

    @Override
    public void setConnectingImage() { }

    @Override
    public void setNoFeedImage() { }

    @Override
    public void updateImage(Bitmap image) {
        getActivity().runOnUiThread(() -> imgView.setImageBitmap(image));
    }

    @Override
    public void updateText(String text) { }

    @Override
    public void disconnect() { }

    @Override
    public void maxCelsiusValue(double max) {
        if (ready) {
            getActivity().runOnUiThread(() ->textMeasuring.setText("Measuring temp: TRUE " + laskuri + "/100"));
            if (laskuri < 100) {
                if (max > userTemp) {
                    userTemp = max;
            }
                laskuri++;
            } else {
                ready = false;
                hasMeasured = true;
                laskuri = 0;
                correcTemp = userTemp + 2.5;
                tempMeasureTask = new asyncTaskUpdateProgress().execute();

                if (37.5 >= correcTemp && correcTemp >= 35.5) {
                    text.setText(R.string.msgNormTmprt);

                } else if (correcTemp > 37.5) {
                    text.setText(R.string.msgHightTmprt);
                } else {
                    text.setText(R.string.msgLowTmprt);
                }
                getActivity().runOnUiThread(() -> textMeasuring.setText("Measuring temp: FALSE"));
            }
        }
    }

    @Override
    public void minCelsiusValue(double min) { }

    @Override
    public void detectFace(Bitmap image) { }

    @Override
    public void writeToFile(byte[] data) { }


    public class asyncTaskUpdateProgress extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPostExecute(Void result) {
            // TODO Auto-generated method stub
           // buttonStart3.setClickable(true);


        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            progress = 0;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            // TODO Auto-generated method stub
            vProgressBar.setProgress(values[0]);
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            // TODO Auto-generated method stub
            double tmin = 29, tmax = 39;

            text2.setText(Double.toString(correcTemp));
            temp = (userTemp - tmin)/(tmax - tmin)*100;
            while (progress < temp) {
                progress++;
                publishProgress(progress);
                SystemClock.sleep(1);
            }
            return null;
        }

    }

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {

                }
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
       // Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(IMAGE_WIDTH, IMAGE_HEIGHT))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        imageAnalysis.setAnalyzer(executor, new MyAnalyzer());


      // preview.setSurfaceProvider(cameraFeed.createSurfaceProvider());

        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, imageAnalysis);


    }

    private android.hardware.Camera getFrontCam() {
        android.hardware.Camera cam = null;
        android.hardware.Camera.CameraInfo camInfo = new android.hardware.Camera.CameraInfo();
        int camCount = android.hardware.Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < camCount; camIdx++) {
            android.hardware.Camera.getCameraInfo(camIdx, camInfo);
            if (camInfo.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    cam = android.hardware.Camera.open(camIdx);
                } catch (Exception e) {
                    System.out.println("Failed to open cam");
                    e.printStackTrace();
                }
            }
        }
        return cam;
    }

    public float calculateFaceDistance(PointF leftEye, PointF rightEye) {
        float deltaX = Math.abs(leftEye.x - rightEye.x);
        float deltaY = Math.abs(leftEye.y - rightEye.y);

        float dist = 0f;
        if (deltaX >= deltaY) {
            dist = focalLength * (AVERAGE_EYE_DISTANCE / sensorX) * (IMAGE_WIDTH / deltaX);
        } else {
            dist = focalLength * (AVERAGE_EYE_DISTANCE / sensorY) * (IMAGE_HEIGHT / deltaY);
        }


        return  dist;
    }

    public void updateDetectedFace(Face face) {
        detectedFace.setValue(face);
    }

    private class MyAnalyzer implements ImageAnalysis.Analyzer {

        @Override
        public void analyze(@NonNull ImageProxy image) {
            int rotationDegrees = image.getImageInfo().getRotationDegrees();
            @SuppressLint("UnsafeExperimentalUsageError") Image img = image.getImage();
            if (img != null) {

                Bitmap bMap = rs.YUV_420_888_toRGB(img, img.getWidth(), img.getHeight());

                InputImage inputImage = InputImage.fromBitmap(bMap, 0);
                fTool.processImage(inputImage, image); // face detection
            }
        }
    }

}