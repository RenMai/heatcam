package com.example.heatcam;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.Image;

public class ModifyHeatmap {

    public static int xOffset = -32;
    public static int yOffset = -71;
    public static int scaledWidth = LeptonCamera.getWidth() ;
    public static int scaledHeight = LeptonCamera.getHeight() ;
    public static float scale = 8.79f;

    public ModifyHeatmap(){ }

    public static Bitmap setOpacity(Bitmap image){
        Bitmap O = Bitmap.createBitmap(image.getWidth(),image.getHeight(), image.getConfig());
        for(int i=0; i<image.getWidth(); i++){
            for(int j=0; j<image.getHeight(); j++){
                int pixel = image.getPixel(i, j);
                int r = Color.red(pixel), g = Color.green(pixel), b = Color.blue(pixel);
                if (pixel > ImageUtils.LOWEST_COLOR) {
                    O.setPixel(i, j, Color.argb(230, r, g, b));
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

    public static void setScale(double newscale) {
        scale *= newscale;
        scale = Math.round(scale*100f)/100f;
        if(scale < 1)
            scale = 1f;
    }
    public static void setRes(double newres) {
        if(scaledWidth*newres < LeptonCamera.getWidth() || scaledHeight*newres < LeptonCamera.getHeight()){
            double oldscale = (double)scaledHeight / (double)(LeptonCamera.getHeight());
            setScale(oldscale);

            scaledWidth = LeptonCamera.getWidth();
            scaledHeight = LeptonCamera.getHeight();
            return;
        }

        double oldscale = (double)scaledHeight / (double)(scaledHeight*newres);
        setScale(oldscale);

        scaledHeight *= newres;
        scaledWidth *= newres;

    }

    public static String teksti(){
        return "x: "+xOffset+" y: "+yOffset+" w/h: "+ ModifyHeatmap.scaledWidth+"/"+ModifyHeatmap.scaledHeight+" s: "+ModifyHeatmap.scale;
    }
}
