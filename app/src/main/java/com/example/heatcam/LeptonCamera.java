package com.example.heatcam;

import android.graphics.Bitmap;
import android.util.Log;

import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.nio.charset.StandardCharsets;
import java.util.Vector;

public abstract class LeptonCamera implements ThermalCamera, SerialInputOutputManager.Listener {
    private Vector<Integer> colorTable = ImageUtils.createColorTable();

    // max width and height of image
    private int width;
    private int height;
    private int telemetryWidth;

    // raw data arrays
    private int[][] rawFrame;
    int[] rawTelemetry; // default visibility for tests
    private byte[] rawData;

    public int getRawDataIndex() {
        return rawDataIndex;
    }

    private int rawDataIndex = 0;

    private CameraListener cameraListener;
    private FrameListener frameListener;

    public LeptonCamera(int width, int height, int telemetryWidth) {
        this.width = width;
        this.height = height;
        this.telemetryWidth = telemetryWidth;
        this.rawFrame = new int[height][width];
        this.rawData = new byte[height*(width+4) + telemetryWidth + 4];
        this.rawTelemetry = new int[telemetryWidth];
        System.out.println("created new leptoncamera");
    }

    public LeptonCamera(int width, int height, int telemetryWidth, int bits) {
        this.width = width;
        this.height = height;
        this.telemetryWidth = telemetryWidth;
        this.rawFrame = new int[height][width];
        this.rawData = new byte[height*((width*(bits/8))+4) + telemetryWidth + 4];
        this.rawTelemetry = new int[telemetryWidth];
        System.out.println("created new leptoncamera");
    }

    public void clickedHeatMapCoordinate(float xTouch, float yTouch, float xImg, float yImg){
        float xScale = (float)this.width/xImg;
        float yScale = (float)this.height/yImg;

        int xPiste = (int)(xTouch*xScale);
        int yPiste = (int)(yTouch*yScale);

        System.out.println(rawFrame[yPiste][xPiste]);
    }

   // public abstract void onNewData(byte[] data);

    @Override
    public void onRunError(Exception e) {
        Log.d("heatcam", e.getMessage());
        cameraListener.disconnect();
    }

    public double kelvinToCelsius(int luku){
        return Math.round(((double)luku/100 - 273.15)*100.0)/100.0;//kahden desimaalin pyöristys
    }

    boolean parseData() {
        return parseData(rawData);
    }

    // parse byte data into rawFrame 2d array
    boolean parseData(byte[] data) {
        int bytesRead = data.length;
        int byteindx = 0;
        int lineNumber;
        int i;
        byte[] startBytes = new byte[] {-1, -1, -1};
        String rowBytes = new String(data, StandardCharsets.UTF_8);
        String pattern = new String(startBytes, StandardCharsets.UTF_8);
        byteindx = rowBytes.indexOf(pattern);

        for (i = byteindx; i < bytesRead; i += (width+4)) {
            lineNumber = data[i + 3];

            if (lineNumber < height) { // picture
                for (int j = 0; j < width; j++) {
                    int dataInd = i + j + 4;
                    if (dataInd < bytesRead) {
                        rawFrame[lineNumber][j] = colorTable.elementAt(data[dataInd] & 0xff);
                    }
                }
            } else if (lineNumber == height) { // telemetry
                for (int j = 0; j < telemetryWidth; j++) {
                    rawTelemetry[j] = data[i + 4 + j];
                }
                return true;

            }
        }
        return false;
    }

    boolean parse16bitData() {
        return parse16bitData(rawData);
    }

    boolean parse16bitData(byte[] data) {
        int bytesRead = data.length;
        int byteindx = 0;
        int lineNumber;
        int i;
        byte[] startBytes = new byte[] {-1, -1, -1};
        String rowBytes = new String(data, StandardCharsets.UTF_8);
        String pattern = new String(startBytes, StandardCharsets.UTF_8);
        byteindx = rowBytes.indexOf(pattern);

        for(i = byteindx; i < bytesRead; i += (width*2+4)) { // row
            lineNumber = data[i + 3];
            if(lineNumber < height) {
                int colInd = 0;
                for (int j = 0; j < width*2; j+=2) {
                    int dataInd = i + j + 4;
                    if (dataInd < bytesRead) {
                        rawFrame[lineNumber][colInd++] = (data[dataInd] & 0xff) + (data[dataInd+1] & 0xff)*256;
                    }
                }
            } else if(lineNumber == height) {
                for (int j = 0; j < telemetryWidth; j++) {
                    rawTelemetry[j] = data[i + 4 + j];
                }
                return true;
            }
        }
        return false;
    }

    public Vector<Integer> getColorTable() {
        return colorTable;
    }

    protected void extractRow(byte[] data) {
        System.arraycopy(data, 0, rawData, rawDataIndex, data.length);
    }

    protected Bitmap getBitmapInternal() {
        return ImageUtils.bitmapFromArray(rawFrame);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setRawDataIndex(int rawDataIndex) {
        this.rawDataIndex = rawDataIndex;
    }

    public CameraListener getCameraListener() {
        return cameraListener;
    }

    public void setCameraListener(CameraListener cameraListener) {
        this.cameraListener = cameraListener;
    }

    public FrameListener getFrameListener() {
        return frameListener;
    }

    @Override
    public void setFrameListener(FrameListener frameListener) {
        this.frameListener = frameListener;
    }
    public byte[] getRawData() {
        return rawData;
    }

    int getRawFramePixel(int width, int height) {
        return rawFrame[height][width];
    }

}
