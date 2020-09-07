package com.example.heatcam;

import android.graphics.Bitmap;

public interface CameraListener {

    void updateImage(Bitmap image);
    void updateText();

}
