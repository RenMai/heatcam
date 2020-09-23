package com.example.heatcam;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.text.DecimalFormat;

public class LowResolutionCamera extends LeptonCamera implements SerialInputOutputManager.Listener {

    public LowResolutionCamera() {
        super(24, 32, 24);
    }

    @Override
    public void onNewData(byte[] data) {

        if(getHeight() == data[3]) {
            extractRow(data);
            parseData();
            setRawDataIndex(0);
            int maxRaw = rawTelemetry[0] + rawTelemetry[1]*0xFF;
            int minRaw = rawTelemetry[3] + rawTelemetry[5]*0xFF;
            Log.d("heatcam", "on new Frame");
            Matrix m = new Matrix();
            m.postRotate(180);
            Bitmap bMap = getBitmapInternal();
            bMap = Bitmap.createBitmap(bMap, 0,0, bMap.getWidth(), bMap.getHeight(), m, true);
            getCameraListener().updateImage(bMap);
            getCameraListener().updateText(""+ kelvinToCelsius(maxRaw));
            if(getFrameListener() != null) {
                getFrameListener().onNewFrame(getRawData());
            }
        } else {
            extractRow(data);
            setRawDataIndex(getRawDataIndex()+data.length);
        }
    }
}
