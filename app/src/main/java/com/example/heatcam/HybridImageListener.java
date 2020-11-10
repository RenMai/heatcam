package com.example.heatcam;

import android.graphics.Bitmap;

public interface HybridImageListener {
    void onNewHybridImage(Bitmap image);
    void sendHeatmap(Bitmap image);
}
