package com.example.heatcam;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.renderscript.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final int REQUEST_CODE_PERMISSIONS = 1001;
    private final String CAMERA_PERMISSION = "android.permission.CAMERA";

    View decorView;
    Button btn;
    boolean active = false;

    private static boolean AUTO_MODE = false;
    private static boolean SETTINGS_OPEN = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkPermissions();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        setContentView(R.layout.activity_main);
        btn = findViewById(R.id.devBtn);
        btn.setOnClickListener(v -> changeLayout());

        // kommentoi tästä
/*
        Fragment cameraActivity = new CameraActivity();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentCamera, cameraActivity, "default").commit();
        initLogger();
        // tähän ja poista seuraava kommentti

 */
         btn.setVisibility(View.INVISIBLE);
        Fragment f = new MenuFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentCamera, f, "menu").commit();


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

        initLogger();
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
        String filePath = getFilesDir() + "/logcat.log";
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

    private void checkPermissions() {
            if (ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{CAMERA_PERMISSION}, REQUEST_CODE_PERMISSIONS);
            }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_CODE_PERMISSIONS && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this, "No permissions granted, app closing", Toast.LENGTH_LONG).show();
            finishAffinity();
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

    @Override
    public void onBackPressed() {
        if(AUTO_MODE) {
            // TODO: estäminen salasanalla tms, nyt vaa kyssäri ku koittaa poistuu
            DialogFragment fragment = new ExitAutoDialogFragment();
            fragment.show(getSupportFragmentManager(), "exit_auto");
        } else {
            FragmentManager manager = getSupportFragmentManager();
            List<Fragment> l = manager.getFragments();

            // first two are main activity and menu fragment
            // -> remove top fragment from list
            if(l.size() > 2) {
                manager.beginTransaction()
                        .setCustomAnimations(R.animator.slide_in_left, R.animator.slide_in_right, 0, 0)
                        .remove(l.get(l.size()-1))
                        .commit();

                // retardi mut ihs :D en jaksanu miettii parempaa tapaa ku halusin et settings buttonii painamal uudestaa pääsee takas ja myös jos painaa back button
                if (SETTINGS_OPEN) {
                    SETTINGS_OPEN = false;
                }
            } else {
                // if there are not other than root fragments exit app
                // TODO: vois tähki tehä jonku dialog joka kysyy poistutaanko
                super.onBackPressed();
            }
        }
    }

    public static synchronized void setAutoMode(boolean mode) {
        AUTO_MODE = mode;
    }

    public static synchronized void setSettingsStatus(boolean status) {
        SETTINGS_OPEN = status;
    }

    public static synchronized boolean getSettingsStatus() {
        return SETTINGS_OPEN;
    }
}