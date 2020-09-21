package com.example.heatcam;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.util.Size;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.vision.CameraSource;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class LiveCameraActivity extends AppCompatActivity {

    // https://medium.com/@akhilbattula/android-camerax-java-example-aeee884f9102
    // https://developer.android.com/training/camerax/preview#java

    private Executor executor = Executors.newSingleThreadExecutor();
    private final int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};

    private Button cameraBtn;
    private PreviewView cameraFeed;
    private ImageView cameraView;

    private FaceDetectionTool fTool;

    private boolean startDetect = false;

    private PoseDetectionTool poseTool;
    private RenderScriptTools rs;
    private TextView posetext;
    private boolean poseStatus = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_camera);

        cameraBtn = (Button) findViewById(R.id.cameraBtn);
        cameraFeed = (PreviewView) findViewById(R.id.cameraFeed);
        cameraView = (ImageView) findViewById(R.id.imageCamera);
        posetext = findViewById(R.id.pose_text);
        rs = new RenderScriptTools(this);
        poseTool = new PoseDetectionTool(this);
        fTool = new FaceDetectionTool(this);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

    }

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {

                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(480, 640))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        imageAnalysis.setAnalyzer(executor, new MyAnalyzer());


        preview.setSurfaceProvider(cameraFeed.createSurfaceProvider());

        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, imageAnalysis, preview);

    }

    private class MyAnalyzer implements ImageAnalysis.Analyzer {

        @Override
        public void analyze(@NonNull ImageProxy image) {
            int rotationDegrees = image.getImageInfo().getRotationDegrees();
            @SuppressLint("UnsafeExperimentalUsageError") Image img = image.getImage();
            if (img != null) {
               //Bitmap bMap = previewToBitmap(img);
                //Bitmap bMap = cameraFeed.getBitmap();


                Bitmap bMap = rs.YUV_420_888_toRGB(img, img.getWidth(), img.getHeight(), rotationDegrees);

                InputImage inputImage = InputImage.fromBitmap(bMap, 0);
                fTool.processImage(inputImage, image); // face detection


                 /* Eeron pose detection

                Bitmap bMap = rs.YUV_420_888_toRGB(img, img.getWidth(), img.getHeight(), rotationDegrees);
                InputImage poseImg = InputImage.fromBitmap(bMap, 0);
                poseTool.processImage(poseImg, image); // <-- tää funktio kutsuu lopuks drawImage()


                  */

                if (rotationDegrees == 0){
                    cameraView.setRotation(-90);
                } else if (rotationDegrees == 270) {
                    cameraView.setRotation(0);
                }
              //  detectFace(bMap);
            }
            //image.close();
        }
    }


    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_CODE_PERMISSIONS){
            if(allPermissionsGranted()){
                startCamera();
            } else{
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }

    // https://stackoverflow.com/a/58568495
    private Bitmap previewToBitmap (Image img) {
        Image.Plane[] planes = img.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, img.getWidth(), img.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    public void drawImage(Bitmap image) {
        runOnUiThread(() -> cameraView.setImageBitmap(image));
    }

    public void updateText(boolean handUp) {
        if(handUp != poseStatus) { // ignore update if status hasn't changed
            poseStatus = handUp;
            String txt = "The hand is ";
            if(poseStatus) {
                txt += "up";
            } else {
                txt += "down";
            }
            final String status = txt;
            runOnUiThread(() -> posetext.setText(status));
        }
    }
}
