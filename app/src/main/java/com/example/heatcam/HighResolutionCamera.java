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
            int maxRaw = rawTelemetry[18] + rawTelemetry[19]*256;
            int minRaw = rawTelemetry[21] + rawTelemetry[22]*256;

            getCameraListener().detectFace(getBitmapInternal());
            if(getFrameListener() != null) {
                getFrameListener().onNewFrame(getRawData());
            }
        } else {
            extractRow(data);
            setRawDataIndex(getRawDataIndex()+data.length);
        }
    }
}
