package com.example.heatcam;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.Executors;


// TODO: function implementations
// TODO: save port name
// TODO: tests
public class LeptonCamera implements SerialInputOutputManager.Listener {

    private Vector<Integer> colorTable = ImageUtils.createColorTable();

    // max width and height of image
    private int width;
    private int height;

    // raw data arrays
    private int[][] rawFrame;
    private int[] rawTelemetry;

    private Activity activity;
    private CameraListener listener;

    private enum UsbPermission { Unknown, Requested, Granted, Denied };
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
    private UsbPermission usbPermission = UsbPermission.Unknown;
    private UsbSerialPort usbSerialPort;
    private SerialInputOutputManager usbIoManager;
    final int ID_VENDOR_TEENSY = 5824;
    final int ID_PRODUC_TEENSY = 1155;
    final Vector<Integer> ALLOWED_VENDORS = new Vector<>(Arrays.asList(ID_VENDOR_TEENSY));
    final Vector<Integer> ALLOWED_PRODUCTS = new Vector<>(Arrays.asList(ID_PRODUC_TEENSY));

    private boolean analysisMode;

    public LeptonCamera(Activity a) {
        this.activity = a;
        this.width = 160;
        this.height = 120;
        this.rawFrame = new int[120][160];
        this.rawTelemetry = new int [50];
        this.analysisMode = false;
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
        if(parseData(data)) {
            int maxRaw, minRaw;
            maxRaw = rawTelemetry[18] + rawTelemetry[19]*256;
            minRaw = rawTelemetry[21] + rawTelemetry[22]*256;

            // TODO: convert rawFrame[][] to Bitmap
            // update image with listener
            Bitmap camImage = ImageUtils.bitmapFromArray(rawFrame);
            listener.updateImage(camImage);
        }
    }

    // parse byte data into rawFrame 2d array
    private boolean parseData(byte[] data) {
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
        // TODO: exception handling here
        try {
            disconnect();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void connect() {
        // TODO: implementation
        // Find all available drivers from attached devices.
        if(usbIoManager != null) {
            listener.updateText("already exists");
            return;
        }
        UsbManager manager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(Objects.requireNonNull(manager));
        if (availableDrivers.isEmpty()) {
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        int prodID = driver.getDevice().getProductId();
        int vendID = driver.getDevice().getVendorId();
        String deviceInfo = driver.getDevice().getProductName() + " - " + driver.getDevice().getManufacturerName() +
                " - " + prodID + " - " + vendID;
        if(ALLOWED_PRODUCTS.contains(prodID) && ALLOWED_VENDORS.contains(vendID)){
            listener.updateText(deviceInfo);
            UsbDeviceConnection connection = manager.openDevice(driver.getDevice());

            if (connection == null && usbPermission == UsbPermission.Unknown && !manager.hasPermission(driver.getDevice())) {
                usbPermission = UsbPermission.Requested;
                PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(activity.getBaseContext(), 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
                manager.requestPermission(driver.getDevice(), usbPermissionIntent);
                return;
            }

            if(connection == null) {
                //failed
                return;
            }

            usbSerialPort = driver.getPorts().get(0); // Most devices have just one port (port 0)
            try {
                usbSerialPort.open(connection);
                usbSerialPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                calibrate();
            } catch (Exception e) {
                e.printStackTrace();
            }

            usbIoManager = new SerialInputOutputManager(usbSerialPort, this);
            Executors.newSingleThreadExecutor().submit(usbIoManager);
        } else {
            listener.updateText("Unauthorized Device");
        }

    }

    public void disconnect() throws IOException {
        if(usbIoManager != null) {
            usbIoManager.stop();
        }
        usbIoManager = null;
        usbSerialPort.close();
        usbPermission = UsbPermission.Unknown;
        listener.updateText("Disconnected");
    }

    // Calibration
    // 0x43 is character 'C'
    public void calibrate() throws IOException {
        usbSerialPort.write("C".getBytes(), 1);
    }

    // Analysis mode
    // Send bytes 0x42 and 0x08 to activate it
    // Send bytes 0x42 and 0x02 to disable it
    // Note 0x42 is character ‘B’
    public void toggleAnalysisMode(){
        // TODO: don't think this works
        if (!analysisMode) {

            try {
               usbSerialPort.write("B".getBytes(), 1);
               usbSerialPort.write(new byte[]{0x08}, 1);
                analysisMode = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                usbSerialPort.write("B".getBytes(), 1);
               usbSerialPort.write(new byte[]{0x02}, 1);
                analysisMode = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setListener(CameraListener listener) {
        this.listener = listener;
    }
}
