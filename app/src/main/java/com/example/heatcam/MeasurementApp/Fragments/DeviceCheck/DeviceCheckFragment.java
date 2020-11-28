package com.example.heatcam.MeasurementApp.Fragments.DeviceCheck;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.heatcam.MeasurementApp.Fragments.CameraListener;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.LowResolution16BitCamera;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialPort.SerialPortModel;
import com.example.heatcam.R;


public class DeviceCheckFragment extends Fragment implements CameraListener {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.device_check_layout, container, false);

        TextView mTextField = view.findViewById(R.id.mTextField);

        checkCamera(view.getContext());
        new CountDownTimer(30000, 1000) {

            public void onTick(long millisUntilFinished) {
                mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
            }

            public void onFinish() {
                mTextField.setText("done!");
            }
        }.start();


        return view;
    }

    private void checkCamera(Context context) {
        SerialPortModel serialPortModel = SerialPortModel.getInstance();
        if(!serialPortModel.hasCamera()) {
            SharedPreferences sharedPrefs = getActivity().getPreferences(Context.MODE_PRIVATE);
            LowResolution16BitCamera cam = new LowResolution16BitCamera();
            cam.setMaxFilter(sharedPrefs.getFloat(getString(R.string.preference_max_filter), -1));
            cam.setMinFilter(sharedPrefs.getFloat(getString(R.string.preference_min_filter), -1));
            serialPortModel.setSioListener(cam);
            serialPortModel.scanDevices(context);
            serialPortModel.changeTiltSpeed(7);
        } else {
            serialPortModel.changeTiltAngle(75);
        }

    }

    @Override
    public void setConnectingImage() {

    }

    @Override
    public void setNoFeedImage() {

    }

    @Override
    public void updateImage(Bitmap image) {

    }

    @Override
    public void updateText(String text) {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public void maxCelsiusValue(double max) {

    }

    @Override
    public void minCelsiusValue(double min) {

    }

    @Override
    public void updateData(LowResolution16BitCamera.TelemetryData data) {

    }

    @Override
    public void detectFace(Bitmap image) {

    }

    @Override
    public void writeToFile(byte[] data) {

    }
}
