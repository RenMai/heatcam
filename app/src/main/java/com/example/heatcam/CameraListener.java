package com.example.heatcam;

import android.graphics.Bitmap;

public interface CameraListener {

    void setConnectingImage();
    void setNoFeedImage();
    void updateImage(Bitmap image);
    void updateText(String text);
    void disconnect();
    void maxCelsiusValue(double max);
    void minCelsiusValue(double min);

}
