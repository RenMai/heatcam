package com.example.heatcam;

import android.app.Activity;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;


// TODO: function implementations
// TODO: save port name
// TODO: tests
public class LeptonCamera implements SerialInputOutputManager.Listener {

    // max width and height of image
    private int width;
    private int height;

    // raw data arrays
    private int rawFrame[][];
    private int rawTelemetry[];

    private UsbSerialPort port;
    private SerialInputOutputManager usbIoManager;
    private Activity activity;

    public LeptonCamera(Activity a) {
        this.activity = a;
        this.width = 160;
        this.height = 120;
        this.rawFrame = new int[120][160];
        this.rawTelemetry = new int [50];
    }

    @Override
    public void onNewData(byte[] data) {
        // TODO: implementation
        // data = 1 row of image (164 bytes)
        // basically should do the same as ::onReadyRead() in LeptonCamDemo thermalcamera.cpp
    }

    @Override
    public void onRunError(Exception e) {
        // TODO: exception handling here
        try {
            disconnect();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void connect() {
        // TODO: implementation
        // eli tähä se connectaus osio mitä toho mainactivity tehty
    }

    public void disconnect() throws IOException {
        if(usbIoManager != null) {
            usbIoManager.stop();
        }
        usbIoManager = null;
        port.close();
    }

    // Calibration
    // 0x43 is character 'C'
    public void calibrate() throws IOException {
        port.write("C".getBytes(), 1);
    }

    private void onNewFrame() {
        int maxRaw, minRaw;
        maxRaw = rawTelemetry[18] + rawTelemetry[19]*256;
        minRaw = rawTelemetry[21] + rawTelemetry[22]*256;

        // TODO: convert rawFrame[][] to Bitmap
        // update image with listener
    }
}
