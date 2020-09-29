package com.example.heatcam;

public class HighResolutionCamera extends LeptonCamera {

    public HighResolutionCamera() {
        super(160, 120,114);
    }
    @Override
    public void onNewData(byte[] data) {

        if(getHeight() == data[3]) {
            extractRow(data);
            parseData();
            setRawDataIndex(0);
            int maxRaw = (rawTelemetry[18]&0xFF) + (rawTelemetry[19]&0xFF)*256;
            int minRaw = (rawTelemetry[21]&0xFF) + (rawTelemetry[22]&0xFF)*256;

            //getCameraListener().detectFace(getBitmapInternal());
            getCameraListener().maxCelsiusValue(kelvinToCelsius(maxRaw));
            getCameraListener().minCelsiusValue(kelvinToCelsius(minRaw));
            getCameraListener().updateImage(getBitmapInternal());
            if(getFrameListener() != null) {
                getFrameListener().onNewFrame(getRawData());
            }
        } else {
            extractRow(data);
            setRawDataIndex(getRawDataIndex()+data.length);
        }
    }
}
