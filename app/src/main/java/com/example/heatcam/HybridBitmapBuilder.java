package com.example.heatcam;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.view.View;

import androidx.camera.core.ImageProxy;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HybridBitmapBuilder{
    private CameraBitmap cameraBitmap;

    private Bitmap heatMap;
    private Bitmap liveMap;
    private HybridFaceDetector fTool;
    private Face face;
    private Rect faceBounds = new Rect();
    private HuippuLukema huiput = new HuippuLukema();
    private HybridImageListener listener;

    public HybridBitmapBuilder(LifecycleOwner owner, View view){
        cameraBitmap = new CameraBitmap(owner, this, view);
        fTool = new HybridFaceDetector(this);
        this.listener = (HybridImageListener)owner;
    }

    long aika = System.currentTimeMillis();
    public void onNewBitmap(Bitmap image, ImageProxy proxy){
        liveMap = image;
        InputImage inputImage = InputImage.fromBitmap(image, 0);
        if(System.currentTimeMillis() > aika){
            aika = System.currentTimeMillis()+100;
            fTool.processImage(inputImage, proxy); // face detection
        }
        updateLiveImage(image);
    }

    public void setHeatmap(Bitmap image) {
        heatMap = image;
    }

    private void drawFaceBounds(Bitmap image){
        huiput = laskeAlue();
        Canvas canvas = new Canvas(image);
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);

        canvas.drawRect(faceBounds, paint);
    }

    private void drawTemperature(Bitmap image){
        huiput = laskeAlue();
        Canvas canvas = new Canvas(image);

        Paint paint2 = new Paint();
        paint2.setColor(Color.CYAN);
        paint2.setStyle(Paint.Style.FILL);
        //paint2.setStrokeWidth(1);

        Paint paint3 = new Paint();
        paint3.setColor(Color.GREEN);
        paint3.setStyle(Paint.Style.FILL);
        //paint3.set

        canvas.drawText(huiput.max+"", huiput.x, huiput.y, paint2);
        canvas.drawCircle(huiput.x, huiput.y, 2, paint3);
    }

    private void updateLiveImage(Bitmap image){

        if(heatMap != null && liveMap != null){
            Bitmap uusi = image;
            if(HybridImageOptions.opacity)
                uusi = ModifyHeatmap.overlay(liveMap,heatMap,true);
            if(HybridImageOptions.heatmap && !HybridImageOptions.opacity)
                uusi = heatMap;
            else if(!HybridImageOptions.heatmap && !HybridImageOptions.opacity)
                uusi = liveMap;
            if(HybridImageOptions.facebounds)
                drawFaceBounds(uusi);
            if(HybridImageOptions.temperature)
                drawTemperature(uusi);
            listener.onNewHybridImage(uusi);
        }
        if(heatMap == null && liveMap != null){
            if(HybridImageOptions.facebounds)
                drawFaceBounds(image);
            listener.onNewHybridImage(image);
        }
    }

    protected void updateDetectedFace(Face face){
        if(face != null){
            this.face = face;
            faceBounds = face.getBoundingBox();
        }
        else{
            faceBounds = new Rect();
            faceBounds.set(0,0,0,0);
        }
    }

    private HuippuLukema laskeAlue(){
        int[][] tempFrame = LeptonCamera.getTempFrame();
        huiput = new HuippuLukema();
        if(heatMap == null) tempFrame = Interpolate.testikuva();
        int[][] scaledTempFrame = Interpolate.scale(tempFrame, LeptonCamera.getHeight(), LeptonCamera.getWidth(), (liveMap.getHeight()/LeptonCamera.getHeight()), (liveMap.getWidth()/LeptonCamera.getWidth()));

        int temp;
        for (int y = 0; y < scaledTempFrame.length; y++) {
            for (int x = 0; x < scaledTempFrame[y].length/2; x++) {
                temp = scaledTempFrame[y][x];
                scaledTempFrame[y][x] = scaledTempFrame[y][scaledTempFrame[y].length - 1 - x];
                scaledTempFrame[y][scaledTempFrame[y].length - 1 - x] = temp;
            }
        }
        for (int y = 0; y < scaledTempFrame.length / 2; y++) {
            for (int x = 0; x < scaledTempFrame[y].length; x++) {
                temp = scaledTempFrame[y][x];
                scaledTempFrame[y][x] = scaledTempFrame[scaledTempFrame.length - 1 - y][x];
                scaledTempFrame[scaledTempFrame.length - 1 -y][x] = temp;
            }
        }

        int maxleveys = scaledTempFrame[scaledTempFrame.length-1].length;
        int maxkorkeus = scaledTempFrame.length;
        int vasen = (int)(faceBounds.left); if(vasen < 0) vasen = 0; if(vasen > maxleveys) vasen = maxleveys;
        int oikea = (int)(faceBounds.right); if(oikea < 0) oikea = 0; if(oikea > maxleveys) oikea = maxleveys;
        int yla = (int)(faceBounds.top); if(yla < 0) yla = 0; if(yla > maxkorkeus) yla = maxkorkeus;
        int ala = (int)(faceBounds.bottom); if(ala < 0) ala = 0; if(ala > maxkorkeus) ala = maxkorkeus;

        try{
            if(scaledTempFrame != null /*&& tempFrame.length > maxkorkeus && tempFrame[tempFrame.length-1].length > maxleveys*/){
                for(int y = yla; y < ala; y++){
                    for(int x = vasen; x < oikea; x++){
                        double lampo = (scaledTempFrame[y][x]- 27315)/100.0;
                        if(lampo > huiput.max){
                            huiput.max = lampo;
                            huiput.y = y;
                            huiput.x = x;
                        }
                    }
                }

            }
        }catch (Exception e){
            //getActivity().runOnUiThread(() -> resoTeksti.setText(e.getMessage()));
        }

        return  huiput;
    }

    public double getHighestFaceTemperature(){
        return huiput.max;
    }

    class HuippuLukema{
        int x = 0;
        int y = 0;
        double max = 0;
    }


}
class HybridImageOptions{
    static boolean opacity = true;
    static boolean heatmap = true;
    static boolean livemap = true;
    static boolean facebounds = true;
    static boolean temperature = true;
}

class HybridFaceDetector {

    FaceDetectorOptions options =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setMinFaceSize(0.35f)
                    .enableTracking()
                    .build();

    FaceDetector faceDetector;
    HybridBitmapBuilder imagebuilder;

    private volatile boolean isProcessing = false;

    public HybridFaceDetector(HybridBitmapBuilder imagebuilder){
        faceDetector = FaceDetection.getClient(options);
        this.imagebuilder = imagebuilder;
    }

    public void processImage(InputImage image, ImageProxy imageProxy) {

        isProcessing = true;

        Task<List<Face>> result =
                faceDetector.process(image)
                        .addOnSuccessListener(
                                faces -> {
                                    // Task completed successfully
                                    // [START_EXCLUDE]
                                    // [START get_face_info]
                                    //faces.sort(((a, b) -> Integer.compare(a.getBoundingBox().width(), b.getBoundingBox().width())));
                                    if (faces.size() > 0) {
                                        for (Face face : faces) {

                                            int id = face.getTrackingId();
                                            PointF leftEyeP = face.getLandmark(FaceLandmark.LEFT_EYE).getPosition();
                                            PointF rightEyeP = face.getLandmark(FaceLandmark.RIGHT_EYE).getPosition();

                                            //float faceDist = userResult.calculateFaceDistance(leftEyeP, rightEyeP);

                                            imagebuilder.updateDetectedFace(face);

                                        }
                                    } else {
                                        imagebuilder.updateDetectedFace(null);
                                    }
                                    // [END get_face_info]
                                    // [END_EXCLUDE]
                                })
                        .addOnCompleteListener(res -> {
                            imageProxy.close();
                            isProcessing = false;
                        })
                        .addOnFailureListener(
                                e -> {

                                    // Task failed with an exception
                                    // ...
                                    System.out.println("FAILURE");
                                    System.out.println(e.getMessage());
                                    e.printStackTrace();
                                    imageProxy.close();
                                    isProcessing = false;
                                });
    }

    public boolean isProcessing() {
        return isProcessing;
    }
}