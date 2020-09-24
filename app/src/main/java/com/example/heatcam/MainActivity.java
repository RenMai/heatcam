package com.example.heatcam;

import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.renderscript.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    View decorView;
    Button btn;
    boolean active = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        btn = findViewById(R.id.devBtn);
        btn.setOnClickListener(v -> changeLayout());

        Fragment cameraActivity = new CameraActivity();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentCamera, cameraActivity, "default").commit();

        initLogger();

        /*
        FragmentManager fManager = getSupportFragmentManager();
        FragmentTransaction fTransaction = fManager.beginTransaction();

        CameraActivity fragment = new CameraActivity();
        fTransaction.add(R.id.fragmentCamera, fragment);
        fTransaction.commit();
        */

        //Status & Navigation bars hiding using decorView 1/2
        decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
            if (visibility == 0)
                decorView.setSystemUiVisibility(hideSystemBars());
        });
    }

    //Status & Navigation bars hiding 2/3
    //this method gets called whenever the the window focus is changed
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            decorView.setSystemUiVisibility(hideSystemBars());

        }

    }
    //Status & Navigation bars hiding 3/3
    private int hideSystemBars() {
        return View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
    }

    private void initLogger() {
        String filePath = getFilesDir() + "/logcat.txt";
        try {
            Runtime.getRuntime().exec("logcat -c");
            Runtime.getRuntime().exec(new String[]{"logcat", "-v time", "-f", filePath,
                    "heatcam:V", "AndroidRuntime:E", "System.out:I",  "SerialInputOutputManager:I", // filters
                    "*:S"});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void changeLayout() {
        if (!active) {
            FragmentManager fManager = getSupportFragmentManager();
            FragmentTransaction fTransaction = fManager.beginTransaction();

            Fragment fragment = new SetupFragment();
            fTransaction.add(R.id.fragmentCamera, fragment, "dev");
            fTransaction.addToBackStack(null);
            fTransaction.commit();
            active = true;
        } else {
            Fragment f = getSupportFragmentManager().findFragmentByTag("dev");
            getSupportFragmentManager().beginTransaction().remove(f).commit();
            active = false;
        }
    }


    private void createFileAndSave(String data) {
        try {
            // Creates a file in the primary external storage space of the
            // current application.
            // If the file does not exists, it is created.
            File testFile = new File(this.getExternalFilesDir(null), "data.txt");
            if (!testFile.exists())
                testFile.createNewFile();

            // Adds a line to the file
            BufferedWriter writer = new BufferedWriter(new FileWriter(testFile, true /*append*/));
            writer.write(data + "\n");
            writer.close();
            // Refresh the data so it can seen when the device is plugged in a
            // computer. You may have to unplug and replug the device to see the
            // latest changes. This is not necessary if the user should not modify
            // the files.
            MediaScannerConnection.scanFile(this,
                    new String[]{testFile.toString()},
                    null,
                    null);
        } catch (IOException e) {
            Log.e("ReadWriteFile", "Unable to write to the TestFile.txt file.");
        }
    }


}