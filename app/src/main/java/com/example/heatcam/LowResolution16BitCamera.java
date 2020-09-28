package com.example.heatcam;

import android.graphics.Bitmap;
import android.graphics.Matrix;

public class LowResolution16BitCamera extends LeptonCamera {

    public LowResolution16BitCamera() {
        super(24, 32,24, 16);
    }

    @Override
    public void onNewData(byte[] data) {
        if(getHeight() == data[3]) {
            extractRow(data);
            parse16bitData();
            setRawDataIndex(0);
            int maxRaw = (rawTelemetry[0]&0xFF) + (rawTelemetry[1]&0xFF)*256;
            int minRaw = (rawTelemetry[3]&0xFF) + (rawTelemetry[4]&0xFF)*256;
            //Bitmap bMap = convertTo8bit(29915, 30515);
            Bitmap bMap = convertTo8bit(minRaw, maxRaw);
            Matrix m = new Matrix();
            m.postRotate(180);
            bMap = Bitmap.createBitmap(bMap, 0,0, bMap.getWidth(), bMap.getHeight(), m, true);
            getCameraListener().updateImage(bMap);
            getCameraListener().updateText(""+ kelvinToCelsius(maxRaw));
            getCameraListener().maxCelsiusValue(kelvinToCelsius(maxRaw));
        } else {
            extractRow(data);
            setRawDataIndex(getRawDataIndex()+data.length);
        }

    }

    private Bitmap convertTo8bit(int min, int max) {
        int pix;
        int ind = 0;
        int[] colors = new int[getWidth() * getHeight()];
        for(int i = 0; i < getHeight(); i++) {
            for(int j = 0; j < getWidth(); j++) {
                pix = ((getRawFramePixel(j, i) - min) * 255 )/ (max-min);
                if(pix > 255) pix = 255;
                else if(pix < 0) pix = 0;
                colors[ind++] = getColorTable().elementAt(pix);
            }
        }
        return Bitmap.createBitmap(colors, getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
    }

}
