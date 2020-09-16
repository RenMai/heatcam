package com.example.heatcam;

import android.graphics.Bitmap;

import com.google.android.gms.common.util.ArrayUtils;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.nio.charset.StandardCharsets;
import java.util.Vector;


// TODO: function implementations
// TODO: save port name
// TODO: tests
public class LeptonCamera implements ThermalCamera, SerialInputOutputManager.Listener {

    private Vector<Integer> colorTable = ImageUtils.createColorTable();

    // max width and height of image
    private int width;
    private int height;

    // raw data arrays
    private int[][] rawFrame;
    int[] rawTelemetry; // default visibility for tests
    private byte[] rawData;
    private int rawDataIndex = 0;

    private CameraListener listener;
    private FrameListener frameListener;

    public LeptonCamera(int width, int height) {
        this.width = width;
        this.height = height;
        this.rawFrame = new int[height][width];
        this.rawTelemetry = new int [50];
        this.rawData = new byte[height*(width+4) + 118];
    }

    public LeptonCamera(CameraListener listener) {
        this.listener = listener;
        this.width = 160;
        this.height = 120;
        this.rawFrame = new int[120][160];
        this.rawTelemetry = new int [50];
        this.rawData = new byte[height*(width+4) + 118];
    }

    public void clickedHeatMapCoordinate(float xTouch, float yTouch, float xImg, float yImg){
        float xScale = (float)this.width/xImg;
        float yScale = (float)this.height/yImg;

        int xPiste = (int)(xTouch*xScale);
        int yPiste = (int)(yTouch*yScale);

        System.out.println(rawFrame[yPiste][xPiste]);
    }

    @Override
    public void onNewData(byte[] data) {
        // check if data is last row
        if(height == data[3]) {
            System.arraycopy(data, 0, rawData, rawDataIndex, data.length);
            parseData(rawData);
            rawDataIndex = 0;

            int maxRaw, minRaw;
            maxRaw = rawTelemetry[18] + rawTelemetry[19]*256;
            minRaw = rawTelemetry[21] + rawTelemetry[22]*256;

            Bitmap camImage = ImageUtils.bitmapFromArray(rawFrame);

            // update image with listener
            listener.updateImage(camImage);
            listener.detectFace(camImage);
            if (frameListener != null) {
                frameListener.onNewFrame(rawData);
            }
            //listener.writeToFile(rawData);
            // listener.maxCelsiusValue(kelvinToCelsius(maxRaw));
            // listener.minCelsiusValue(kelvinToCelsius(minRaw));
        } else {
            System.arraycopy(data, 0, rawData, rawDataIndex, data.length);
            rawDataIndex += data.length;
        }
    }

    private double kelvinToCelsius(int luku){
        return Math.round((luku/100 - 273.15)*100.0)/100.0;//kahden desimaalin pyöristys
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
                for (int j = 0; j < 48; j++) {
                    rawTelemetry[j] = data[i + 4 + j];
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRunError(Exception e) {
        listener.disconnect();
    }

    // for testing purpose
    int getRawFramePixel(int width, int height) {
        return rawFrame[height][width];
    }

    @Override
    public void setFrameListener(FrameListener listener) {
        this.frameListener = listener;
    }
}
