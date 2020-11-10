package com.example.heatcam;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
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
    private MeasurementStartFragment msf;

    public HybridBitmapBuilder(LifecycleOwner owner, View view){
        cameraBitmap = new CameraBitmap(owner, this, view);
        fTool = new HybridFaceDetector(this);
        if(owner instanceof MeasurementStartFragment)
            this.msf = (MeasurementStartFragment)owner;

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

        Canvas canvas = new Canvas(image);
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);

        Rect rect = new Rect();
        rect.set((int)(vasen*ModifyHeatmap.scale + ModifyHeatmap.getxOffset()), (int)(yla*ModifyHeatmap.scale + ModifyHeatmap.getyOffset()), (int)(oikea*ModifyHeatmap.scale + ModifyHeatmap.getxOffset()), (int)(ala*ModifyHeatmap.scale + ModifyHeatmap.getyOffset()));
        canvas.drawRect(rect, paint);
    }

    private void drawTemperature(Bitmap image){

        Canvas canvas = new Canvas(image);

        Paint paint2 = new Paint();
        paint2.setColor(Color.CYAN);
        paint2.setStyle(Paint.Style.FILL);

        Paint paint3 = new Paint();
        paint3.setColor(Color.GREEN);
        paint3.setStyle(Paint.Style.FILL);

        canvas.drawText(huiput.max+"", huiput.x, huiput.y, paint2);
        canvas.drawCircle(huiput.x, huiput.y, 2, paint3);
    }

    private Bitmap overlay(Bitmap live, Bitmap heat, boolean opacity) {

        if(opacity)
            heat = ModifyHeatmap.setOpacity(heat);

        Matrix m = new Matrix();
        //m.postScale(ModifyHeatmap.scale, ModifyHeatmap.scale);
        m.postTranslate(ModifyHeatmap.xOffset, ModifyHeatmap.yOffset);
        heat = Bitmap.createScaledBitmap(heat, (int)(heat.getWidth()*ModifyHeatmap.scale), (int)(heat.getHeight()*ModifyHeatmap.scale), HybridImageOptions.smooth);
        Canvas canvas = new Canvas(live);
        canvas.drawBitmap(heat, m, null);

        return live;
    }

    private void updateLiveImage(Bitmap img){
        // copy image to make it mutable
        Bitmap image = img.copy(Bitmap.Config.ARGB_8888, true);
        if(heatMap != null && liveMap != null){
            huiput = laskeAlue();
            if(HybridImageOptions.opacity)
                image = overlay(liveMap,heatMap,true);
            else
                image = heatMap.copy(Bitmap.Config.ARGB_8888, true);

            if(HybridImageOptions.facebounds)
                drawFaceBounds(image);
            if(HybridImageOptions.temperature)
                drawTemperature(image);
            listener.onNewHybridImage(image);
        }
        else if(heatMap == null && liveMap != null){
            if(HybridImageOptions.facebounds)
                drawFaceBounds(image);
            listener.onNewHybridImage(image);
        }
    }

    public void setMsfNull(){
        msf = null;
    }
    protected void updateDetectedFace(Face face){
        if(face != null){
            if(msf != null)
                msf.faceDetected(face);
            this.face = face;
            faceBounds = face.getBoundingBox();
        }
        else{
            if(msf != null)
                msf.faceNotDetected();
            faceBounds = new Rect();
            faceBounds.set(0,0,0,0);
        }
    }

    int yla,vasen,oikea,ala = 0;
    private HuippuLukema laskeAlue(){
        int[][] scaledTempFrame = ScaledHeatmap.scaledTempFrame;
        huiput = new HuippuLukema();
        if(scaledTempFrame == null)
            scaledTempFrame = LeptonCamera.getTempFrame();

        int livekorkeus = liveMap.getHeight();
        int liveleveys = liveMap.getWidth();
        int heatleveys = scaledTempFrame[scaledTempFrame.length-1].length;
        int heatkorkeus = scaledTempFrame.length;

        int top = (int)(((double)faceBounds.top / (double)livekorkeus) * heatkorkeus);
        int left = (int)(((double)faceBounds.left / (double)liveleveys) * heatleveys);
        int right = (int)(((double)faceBounds.right / (double)liveleveys) * heatleveys);
        int bottom = (int)(((double)faceBounds.bottom / (double)livekorkeus) * heatkorkeus);

        vasen = left; if(vasen < 0) vasen = 0; if(vasen > heatleveys) vasen = heatleveys;
        oikea = right; if(oikea < 0) oikea = 0; if(oikea > heatleveys) oikea = heatleveys;
        yla = top; if(yla < 0) yla = 0; if(yla > heatkorkeus) yla = heatkorkeus;
        ala = bottom; if(ala < 0) ala = 0; if(ala > heatkorkeus) ala = heatkorkeus;

        try{
            if(scaledTempFrame != null /*&& tempFrame.length > maxkorkeus && tempFrame[tempFrame.length-1].length > maxleveys*/){
                for(int y = yla; y < ala; y++){
                    for(int x = vasen; x < oikea; x++){
                        double lampo = (scaledTempFrame[y][x]- 27315)/100.0;
                        if(lampo > huiput.max){
                            huiput.max = lampo;
                            huiput.y = (int)(y*ModifyHeatmap.scale) + ModifyHeatmap.getyOffset();
                            huiput.x = (int)(x*ModifyHeatmap.scale) + ModifyHeatmap.getxOffset();
                        }
                    }
                }
            }

        }catch (Exception e){
            //System.out.println(e.getMessage());
        }

        return  huiput;
    }

    public double getHighestFaceTemperature(){
        return huiput.max;
    }
    public Bitmap getLiveMap(){
        return liveMap;
    }

    class HuippuLukema{
        int x = 0;
        int y = 0;
        double max = 0;
    }

}
class HybridImageOptions{
    static boolean opacity = true;
    static boolean smooth = true;
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
                                    //faces.sort(((a, b) -> Integer.compare(a.getBoundingBox().width(), b.getBoundingBox().width())));
                                    if (faces.size() > 0)
                                        imagebuilder.updateDetectedFace(faces.get(0));
                                     else
                                        imagebuilder.updateDetectedFace(null);

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