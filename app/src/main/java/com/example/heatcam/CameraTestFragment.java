package com.example.heatcam;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Locale;


public class CameraTestFragment extends Fragment implements CameraListener, HybridImageListener {

    private SerialPortModel sModel;
    private TestFileReader testFileReader;

    private ImageView camFeed;

    private String testDataFileName = "maskilasit.txt";
    private final String testDataPath = "test_data/";

    private CameraListener listener = this;
    private LowResolution16BitCamera activeCam = null;

    private SensorManager sManager;

    private TextView textAzimuth;
    private TextView textPitch;
    private TextView textRoll;
    private TextView kerroinTeksti, resoTeksti;
    private HybridBitmapBuilder hybridBitmap;
    // Gravity rotational data
    private float gravity[];
    // Magnetic rotational data
    private float magnetic[];
    private float accels[] = new float[3];
    private float mags[] = new float[3];
    private float[] values = new float[3];

    // azimuth z axis
    private float azimuth;
    // pitch x axis
    private float pitch;
    // roll y axis
    private float roll;

    private SeekBar sliderAngle;
    private int sliderMin = 22;
    private TextView angleText;
    private TextView telemetryText;
    private int telemetryCount = 0;

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.camera_test_layout, container, false);

        camFeed = view.findViewById(R.id.camera_test_view);

        sModel = SerialPortModel.getInstance();
        sModel.setCamListener(this);

        textAzimuth = view.findViewById(R.id.textAzimuth);
        textPitch = view.findViewById(R.id.textPitch);
        textRoll = view.findViewById(R.id.textRoll);
        sliderAngle = view.findViewById(R.id.seekBar);
        angleText = view.findViewById(R.id.textView6);
        telemetryText = view.findViewById(R.id.textTelemetry);
        //liveFeed = view.findViewById(R.id.livefeed);
        kerroinTeksti = view.findViewById(R.id.kerroinText);
        resoTeksti = view.findViewById(R.id.resot);
        hybridBitmap = new HybridBitmapBuilder(this, view);

        sManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        sManager.registerListener(myDeviceOrientationListener, sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        sManager.registerListener(myDeviceOrientationListener, sManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);

        IntentFilter filter = new IntentFilter("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        getContext().registerReceiver(sModel, filter);

        LeptonCamera testCam = new HighResolutionCamera();
        testCam.setCameraListener(this);
        testFileReader = new TestFileReader(view.getContext(), testCam);

        view.findViewById(R.id.camera_test_data_button).setOnClickListener(v -> {
            testFileReader.readTestFile(testDataPath + testDataFileName);
        });

        view.findViewById(R.id.ylos).setOnTouchListener((v, e)-> {
            if(e.getAction() == MotionEvent.ACTION_MOVE || e.getAction() == MotionEvent.ACTION_DOWN){
                ModifyHeatmap.setyOffset(ModifyHeatmap.getyOffset()-1);
                getActivity().runOnUiThread(() -> kerroinTeksti.setText(ModifyHeatmap.teksti()));
            }
            return false;
        });
        view.findViewById(R.id.alas).setOnTouchListener((v, e)-> {
            if(e.getAction() == MotionEvent.ACTION_MOVE || e.getAction() == MotionEvent.ACTION_DOWN){
                ModifyHeatmap.setyOffset(ModifyHeatmap.getyOffset()+1);
                getActivity().runOnUiThread(() -> kerroinTeksti.setText(ModifyHeatmap.teksti()));
            }
            return false;
        });
        view.findViewById(R.id.oikea).setOnTouchListener((v, e)-> {
            if(e.getAction() == MotionEvent.ACTION_MOVE || e.getAction() == MotionEvent.ACTION_DOWN){
                ModifyHeatmap.setxOffset(ModifyHeatmap.getxOffset()+1);
                getActivity().runOnUiThread(() -> kerroinTeksti.setText(ModifyHeatmap.teksti()));
            }
            return false;
        });
        view.findViewById(R.id.vasen).setOnTouchListener((v, e)-> {
            if(e.getAction() == MotionEvent.ACTION_MOVE || e.getAction() == MotionEvent.ACTION_DOWN){
                ModifyHeatmap.setxOffset(ModifyHeatmap.getxOffset()-1);
                getActivity().runOnUiThread(() -> kerroinTeksti.setText(ModifyHeatmap.teksti()));
            }
            return false;
        });
        view.findViewById(R.id.plus).setOnClickListener(v -> {
            ModifyHeatmap.setScale(Math.round((ModifyHeatmap.getScale() + 0.2f)*10.0f)/10.0f);
            getActivity().runOnUiThread(() -> kerroinTeksti.setText(ModifyHeatmap.teksti()));
        });
        view.findViewById(R.id.miinus).setOnClickListener(v -> {
            ModifyHeatmap.setScale(Math.round((ModifyHeatmap.getScale() - 0.2f)*10.0f)/10.0f);
            getActivity().runOnUiThread(() -> kerroinTeksti.setText(ModifyHeatmap.teksti()));
        });
        view.findViewById(R.id.heatmap).setOnClickListener(v -> {
            HybridImageOptions.heatmap = ((CheckBox) v).isChecked();
        });
        view.findViewById(R.id.opacity).setOnClickListener(v -> {
            HybridImageOptions.opacity = ((CheckBox) v).isChecked();
        });
        view.findViewById(R.id.temperature).setOnClickListener(v -> {
            HybridImageOptions.temperature = ((CheckBox) v).isChecked();
        });
        view.findViewById(R.id.facebounds).setOnClickListener(v -> {
            HybridImageOptions.facebounds = ((CheckBox) v).isChecked();
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
                SharedPreferences sp = getActivity().getPreferences(Context.MODE_PRIVATE);
                switch (position) {
                    case 0 :
                        cam = new LowResolution16BitCamera();
                        activeCam = (LowResolution16BitCamera) cam;
                        activeCam.setMaxFilter(sp.getFloat(getString(R.string.preference_max_filter), -1));
                        activeCam.setMinFilter(sp.getFloat(getString(R.string.preference_min_filter), -1));
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
        SharedPreferences sp = getActivity().getPreferences(Context.MODE_PRIVATE);
        float maxFilterVal = sp.getFloat(getString(R.string.preference_max_filter), 0);
        if(maxFilterVal > 0 ) {
            maxFilter.setText(Float.toString(maxFilterVal));
        }
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
                    getActivity().getPreferences(Context.MODE_PRIVATE)
                            .edit()
                            .putFloat(getString(R.string.preference_max_filter), value)
                            .apply();
                    activeCam.setMaxFilter(value);

                }
            }
        });
        EditText minFilter = view.findViewById(R.id.edit_min_filter);
        float minFilterVal = sp.getFloat(getString(R.string.preference_min_filter), 0);
        if(minFilterVal > 0 ) {
            minFilter.setText(Float.toString(minFilterVal));
        }
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
                    getActivity().getPreferences(Context.MODE_PRIVATE)
                            .edit()
                            .putFloat(getString(R.string.preference_min_filter), value)
                            .apply();
                    activeCam.setMinFilter(value);
                }
            }
        });

        sliderAngle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int valueSlider = sliderMin;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueSlider = sliderMin + seekBar.getProgress();
                angleText.setText("Angle: " + valueSlider);
                sModel.changeTiltAngle(valueSlider);
            }
        });

        return view;
    }

    @Override
    public void onPause() {
        //disconnect();
        super.onPause();
        sManager.unregisterListener(myDeviceOrientationListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        sManager.registerListener(myDeviceOrientationListener, sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        sManager.registerListener(myDeviceOrientationListener, sManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
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
        sendHeatmap(image);
        //getActivity().runOnUiThread(() -> camFeed.setImageBitmap(image));
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

    public void updateTelemetryText(String telemetry) {
        if (telemetryCount == 5) {
            getActivity().runOnUiThread(() -> telemetryText.setText(telemetry + "\n"));
            telemetryCount = 0;
        } else {
            getActivity().runOnUiThread(() -> telemetryText.append(telemetry + "\n"));
        }
        telemetryCount++;
    }

    public void updateOrientationText() {
        getActivity().runOnUiThread(() -> {
            textAzimuth.setText(String.format("Azimuth: %.02f", azimuth));
            textPitch.setText(String.format("Pitch: %.02f", pitch));
            textRoll.setText(String.format("Roll: %.02f", roll));
        });
    }

    private SensorEventListener myDeviceOrientationListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mags = event.values.clone();
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    accels = event.values.clone();
                    break;
            }

            if (mags != null && accels != null) {
                gravity = new float[9];
                magnetic = new float[9];
                SensorManager.getRotationMatrix(gravity, magnetic, accels, mags);
                float[] outGravity = new float[9];
                SensorManager.remapCoordinateSystem(gravity, SensorManager.AXIS_X,SensorManager.AXIS_Z, outGravity);
                SensorManager.getOrientation(outGravity, values);


                azimuth = values[0] * 57.2957795f;
                pitch = values[1] * 57.2957795f;
                roll = values[2] * 57.2957795f;
                mags = null;
                accels = null;
                updateTelemetryText(String.valueOf(azimuth));
                updateOrientationText();
            }
        }
    };

    @Override
    public void onNewHybridImage(Bitmap image) {
        getActivity().runOnUiThread(() -> camFeed.setImageBitmap(image));
    }

    @Override
    public void sendHeatmap(Bitmap image) {
        hybridBitmap.setHeatmap(image);
    }

    public void updateData(LowResolution16BitCamera.TelemetryData data) {
        getActivity().runOnUiThread(() -> telemetryText.setText(data.toString()));
    }
}
