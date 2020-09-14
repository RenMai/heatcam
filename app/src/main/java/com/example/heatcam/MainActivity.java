package com.example.heatcam;

import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

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



         /*
        FragmentManager fManager = getSupportFragmentManager();
        FragmentTransaction fTransaction = fManager.beginTransaction();

        CameraActivity fragment = new CameraActivity();
        fTransaction.add(R.id.fragmentCamera, fragment);
        fTransaction.commit();
        */



    }

    private void changeLayout() {
        if (!active)  {
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
            writer.write(data +"\n");
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