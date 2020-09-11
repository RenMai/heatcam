package com.example.heatcam;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.util.Objects;

public class CameraActivity extends Fragment implements CameraListener {

    private LeptonCamera camera;
    SerialPortModel sModel;

    private TextView txtView;
    private Button scanBtn;
    private Button analysisBtn;
    private Button testBtn;
    private ImageView imgView;

    private TestFileReader testFileReader;



    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.activity_camera_fragment, container, false);
        txtView = (TextView) view.findViewById(R.id.textView);
        scanBtn = (Button) view.findViewById(R.id.scanBtn);
        analysisBtn = (Button) view.findViewById(R.id.analysisBtn);
        testBtn = (Button) view.findViewById(R.id.testBtn);
        imgView = (ImageView) view.findViewById(R.id.imageView);

        camera = new LeptonCamera(this);
        sModel = new SerialPortModel(this, camera);

        // camera.setListener(this);
        testFileReader = new TestFileReader(view.getContext(), camera);

        scanBtn.setOnClickListener(v -> sModel.scanDevices(Objects.requireNonNull(getContext())));
        analysisBtn.setOnClickListener(v -> sModel.toggleAnalysisMode());
        testBtn.setOnClickListener(v -> testFileReader.readTestFile("data2.txt"));

        imgView.setOnTouchListener((v, event) -> {
            camera.clickedHeatMapCoordinate(event.getX(), event.getY(), imgView.getWidth(), imgView.getHeight());
            return false;
        });

        return view;
    }

    private void sendTestData(){
        testFileReader.readTestFile("data.txt");
    }

    @Override
    public void setConnectingImage() {
        getActivity().runOnUiThread(() -> {
            imgView.setImageResource(R.drawable.connecting);
        });
    }

    @Override
    public void setNoFeedImage() {
        getActivity().runOnUiThread(() -> {
            imgView.setImageResource(R.drawable.noimage);
        });
    }

    @Override
    public void updateImage(Bitmap image) {
        getActivity().runOnUiThread(() -> { imgView.setImageBitmap(image);});
    }

    @Override
    public void updateText(String text) {
        getActivity().runOnUiThread(() -> {txtView.setText(text);});
    }

    @Override
    public void disconnect() {
        try {
            sModel.disconnect();
            setNoFeedImage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void maxCelsiusValue(double max) {

    }

    @Override
    public void minCelsiusValue(double min) {

    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            imgView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            imgView.getLayoutParams().width = 0;
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            imgView.getLayoutParams().height = 0;
            imgView.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
        }
    }
}
