package com.example.heatcam;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;

public class ModifyHeatmap {
    private CameraTestFragment cameraTestFragment;

    public static int xOffset = -47;
    public static int yOffset = -54;
    public static float scale = 1.6f;

    public ModifyHeatmap(){
    }

    public static Bitmap overlay(Bitmap live, Bitmap heat, boolean opacity) {
        //getActivity().runOnUiThread(() -> resoTeksti.setText(live.getWidth()+", y: "+live.getHeight()+" / "+heat.getWidth()+", y: "+heat.getHeight()));

        if(opacity) heat = setOpacity(heat);

        //heat.setWidth(live.getWidth());
        //heat.setHeight(live.getHeight());
        Matrix m = new Matrix();
        m.postScale(scale,scale);
        m.postTranslate(xOffset, yOffset);

        heat = Bitmap.createScaledBitmap(heat, live.getWidth(), live.getHeight(), true);
        Bitmap bmOverlay = Bitmap.createBitmap(live.getWidth(), live.getHeight(), live.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(live, new Matrix(), null);
        canvas.drawBitmap(heat, m, null);

        return bmOverlay;
    }

    public static Bitmap setOpacity(Bitmap image){
        Bitmap O = Bitmap.createBitmap(image.getWidth(),image.getHeight(), image.getConfig());
        for(int i=0; i<image.getWidth(); i++){
            for(int j=0; j<image.getHeight(); j++){
                int pixel = image.getPixel(i, j);
                int r = Color.red(pixel), g = Color.green(pixel), b = Color.blue(pixel);
                if (b < 200)
                {
                    O.setPixel(i, j, Color.argb(20, i, j, pixel));
                }
            }
        }
        return O;
    }

    public static int getxOffset() {
        return xOffset;
    }

    public static void setxOffset(int xOffset) {
        ModifyHeatmap.xOffset = xOffset;
    }

    public static int getyOffset() {
        return yOffset;
    }

    public static void setyOffset(int yOffset) {
        ModifyHeatmap.yOffset = yOffset;
    }

    public static float getScale() {
        return scale;
    }

    public static void setScale(float scale) {
        ModifyHeatmap.scale = scale;
    }

    public static String teksti(){
        return "x: "+xOffset+" y: "+yOffset+" s: "+scale;
    }
}
