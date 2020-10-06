package com.example.heatcam;

import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


public class CameraTestFragment extends Fragment implements CameraListener {

    private SerialPortModel sModel;
    private TestFileReader testFileReader;

    private ImageView camFeed;

    private String testDataFileName = "maskilasit.txt";
    private final String testDataPath = "test_data/";

    private CameraListener listener = this;
    private LowResolution16BitCamera activeCam = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.camera_test_layout, container, false);

        camFeed = view.findViewById(R.id.camera_test_view);

        sModel = SerialPortModel.getInstance();
        sModel.setCamListener(this);

        IntentFilter filter = new IntentFilter("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        getContext().registerReceiver(sModel, filter);

        LeptonCamera testCam = new HighResolutionCamera();
        testCam.setCameraListener(this);
        testFileReader = new TestFileReader(view.getContext(), testCam);

        view.findViewById(R.id.camera_test_data_button).setOnClickListener(v -> {
            testFileReader.readTestFile(testDataPath + testDataFileName);
        });

        Spinner setting = view.findViewById(R.id.camera_setting_spinner);
        String[] list = new String[] {"24x32 16bit", "24x32 8bit", "160x120 8bit"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), R.layout.support_simple_spinner_dropdown_item, list);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        setting.setAdapter(adapter);

        setting.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                System.out.println(position);
                disconnect();
                LeptonCamera cam = null;
                activeCam = null;
                switch (position) {
                    case 0 :
                        cam = new LowResolution16BitCamera();
                        activeCam = (LowResolution16BitCamera) cam;
                        break;
                    case 1 :
                        cam = new LowResolutionCamera();
                        break;
                    case 2 :
                        cam = new HighResolutionCamera();
                        break;
                }
                if(cam != null) {
                    cam.setCameraListener(listener);
                    sModel.setSioListener(cam);
                    sModel.scanDevices(getContext());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        EditText maxFilter = view.findViewById(R.id.edit_max_filter);
        maxFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                if(activeCam != null) {
                    float value = -1;
                    try {
                        value = Float.parseFloat(s.toString());

                    } catch(Exception e) {
                    }

                    activeCam.setMaxFilter(value);

                }
            }
        });
        EditText minFilter = view.findViewById(R.id.edit_min_filter);
        minFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(activeCam != null) {
                    float value = -1;
                    try {
                        value = Float.parseFloat(s.toString());
                    } catch(Exception e) {

                    }
                    activeCam.setMinFilter(value);


                }
            }
        });

        return view;
    }

    @Override
    public void onPause() {
        disconnect();
        super.onPause();
    }

    @Override
    public void setConnectingImage() {
        getActivity().runOnUiThread(() -> {

            camFeed.setImageResource(R.drawable.connecting);
        });
    }

    @Override
    public void setNoFeedImage() {
        getActivity().runOnUiThread(() -> {
            camFeed.setImageResource(R.drawable.noimage);
        });
    }

    @Override
    public void updateImage(Bitmap image) {
        getActivity().runOnUiThread(() -> camFeed.setImageBitmap(image));
    }

    @Override
    public void updateText(String text) {
        getActivity().runOnUiThread(() -> ((TextView)getActivity().findViewById(R.id.camera_status_text)).setText(text));
    }

    @Override
    public void disconnect() {
        try {
            sModel.disconnect();
            setNoFeedImage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void maxCelsiusValue(double max) {
        TextView text = getActivity().findViewById(R.id.camera_max_raw_value);
        if(text == null) return;
        getActivity().runOnUiThread(() -> text.setText((String.valueOf(max))));
    }

    @Override
    public void minCelsiusValue(double min) {
        TextView text = getActivity().findViewById(R.id.camera_min_raw_value);
        if(text == null) return;
        getActivity().runOnUiThread(() -> text.setText((String.valueOf(min))));
    }

    @Override
    public void detectFace(Bitmap image) {

    }

    @Override
    public void writeToFile(byte[] data) {

    }
}
