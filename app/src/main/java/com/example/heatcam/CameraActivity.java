package com.example.heatcam;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class CameraActivity extends Fragment implements CameraListener {

    private LeptonCamera camera;

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

       camera = new LeptonCamera((Activity) view.getContext());
       camera.setListener(this);
        testFileReader = new TestFileReader(view.getContext(), camera);

        scanBtn.setOnClickListener(v -> camera.connect());
        analysisBtn.setOnClickListener(v -> camera.toggleAnalysisMode());
        testBtn.setOnClickListener(v -> testFileReader.readTestFile("data.txt"));

        imgView.setOnTouchListener((v, event) -> {
            camera.clickedHeatMapCoordinate(event.getX(), event.getY(), imgView.getWidth(), imgView.getHeight());
            return false;
        });

        return view;
    }


    /*
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity a = null;
        if (context instanceof Activity) {
            a = (Activity) context;
        }

        Log.i("asd", a.toString());

        camera = new LeptonCamera(getActivity());
        testFileReader = new TestFileReader(context, camera);

    }

*/

    private void sendTestData(){
        testFileReader.readTestFile("data.txt");
    }

    @Override
    public void updateImage(Bitmap image) {
        getActivity().runOnUiThread(() -> { imgView.setImageBitmap(image);});
    }

    @Override
    public void updateText() {

    }
}
