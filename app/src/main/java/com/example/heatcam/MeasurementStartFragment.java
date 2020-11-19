package com.example.heatcam;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.Log;
import android.util.SizeF;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MeasurementStartFragment extends Fragment implements CameraListener, HybridImageListener {

    private final String TAG = "MeasurementStartFragment";

    private int facePositionCheckCounter = 0;
    private final int checkLimit = 10;

    private final int AVERAGE_EYE_DISTANCE = 63; // in mm

    private float focalLength;
    private float sensorX;
    private float sensorY;
    private SizeF sensor;

    private float preferred_measure_distance;

    private Animation scanAnimation;
    private Button animBtn;
    private View scanBar;

    private boolean ready;
    private Rect naamarajat;

    private double userTemp = 0;
    private double avgUserTemp = 0;
    private List<Double> userTempList;

    private int laskuri = 0;
    private boolean hasMeasured = false;

    private TextView txtDebug;

    SerialPortModel serialPortModel;

    private MeasurementAccessObject measurementAccessObject;
    private Timer tiltTimer = new Timer();
    private int currentTiltAngle;
    private int setTiltAngle = 0;
    private boolean isAtSetAngle = true;
    private boolean tiltTimerRunning = false;
    private int timerDelay = 200;

    private ScheduledThreadPoolExecutor idleExecutor;

    private AnimatedOval animatedOval;
    private ImageView heatkuva;
    private HybridBitmapBuilder hbb;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferred_measure_distance = Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(getContext())
                .getString(getString(R.string.preference_measure_distance), "300"));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.measurement_start_layout, container, false);

        SharedPreferences sharedPrefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        timerDelay = Integer.parseInt(sharedPrefs.getString("PREFERENCE_TILT_CORRECTION_DELAY", "800"));
        // prevent app from dimming
        view.setKeepScreenOn(true);
        ConstraintLayout cl = (ConstraintLayout)view.findViewById(R.id.ConstraintLayout);
        cl.setBackgroundColor(Color.BLACK);
        hbb = new HybridBitmapBuilder(this, view);
        animBtn = view.findViewById(R.id.animBtn);
        scanBar = view.findViewById(R.id.scanBar);
        createFaceOval(view);

        heatkuva = view.findViewById(R.id.heatkuva);
        animatedOval = view.findViewById(R.id.animatedOval);


        measurementAccessObject = new MeasurementAccessObject();

        txtDebug = view.findViewById(R.id.txtDebugit);

        serialPortModel = SerialPortModel.getInstance();
        serialPortModel.setCamListener(this);

        scanAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.scan_animation);

        scanAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
    public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                scanBar.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        animBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // can add here some functionality for debugging
                changeToResultLayout();
                /*
                scanBar.setVisibility(View.VISIBLE);
                scanBar.startAnimation(scanAnimation);

                changeToResultLayout();

                animatedOval.init();
                animatedOval.setVisibility(View.VISIBLE);
                 */
            }
        });
        getCameraProperties();
        ProgressBar bar = view.findViewById(R.id.face_check_prog);
        bar.setMax(checkLimit);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        tiltTimer.cancel();
        stopIdleExecutor();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void getCameraProperties() {
        CameraManager manager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics c = manager.getCameraCharacteristics(getFrontFacingCameraId(manager));
            focalLength = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)[0];
            sensor = c.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
            float angleX = (float) Math.atan(sensor.getWidth() / (2 * focalLength));
            float angleY = (float) Math.atan(sensor.getHeight() / (2 * focalLength));
            System.out.println("fov" + angleX + angleY);
            sensorX = (float) (Math.tan(Math.toRadians(angleX / 2)) * 2 * focalLength);
            sensorY = (float) (Math.tan(Math.toRadians(angleY / 2)) * 2 * focalLength);
            System.out.println("leng" + focalLength);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createFaceOval(View view) {
        Bitmap overlay = Bitmap.createBitmap(1200, 1920, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(overlay);
        Paint paint = new Paint();
        //paint.setColor(Color.parseColor("#871019FF"));
        paint.setShader(new LinearGradient(0, 0, 600, 960, Color.parseColor("#871019FF"), Color.parseColor("#85FB3A1F"), Shader.TileMode.MIRROR));
        paint.setShader(new LinearGradient(600, 960, 1200, 1920, Color.parseColor("#85FB3A1F"), Color.parseColor("#871019FF"), Shader.TileMode.MIRROR));
        paint.setAlpha(230);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        Rect rt = new Rect(0, 0, overlay.getWidth(), overlay.getHeight());
        canvas.drawRect(rt, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        float x = overlay.getWidth() / 2.0f;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(view.getContext());
        float width = Float.parseFloat(sp.getString(getString(R.string.preference_oval_width), "750")) / 2;
        float height = Float.parseFloat(sp.getString(getString(R.string.preference_oval_height), "1300"));
        float fromTop = Float.parseFloat(sp.getString(getString(R.string.preference_oval_pos_from_top), "200"));
        canvas.drawOval(x - width, fromTop, x + width, height, paint);
        ((ImageView) view.findViewById(R.id.start_layout_oval_overlay)).setImageBitmap(overlay);
    }

    String getFrontFacingCameraId(CameraManager cManager) throws CameraAccessException {
        for (final String cameraId : cManager.getCameraIdList()) {
            CameraCharacteristics characteristics = cManager.getCameraCharacteristics(cameraId);
            int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (cOrientation == CameraCharacteristics.LENS_FACING_FRONT) return cameraId;
        }
        return null;
    }

    public float facePositionCheck(Face face, int imgWidth, int imgHeight) {

        float middleX = imgWidth / 2f;
        float middleY =  imgHeight / 2.2f; //2.146f; //imgHeight / 2.05f; // joutuu sit säätää tabletille tää ja deviation
        float maxDeviationX = 25f; // eli max +- pixel heitto sijaintiin
        float maxDeviationY = 15f;
        PointF noseP = face.getLandmark(FaceLandmark.NOSE_BASE).getPosition();
        PointF leftEyeP = face.getLandmark(FaceLandmark.LEFT_EYE).getPosition();
        PointF rightEyeP = face.getLandmark(FaceLandmark.RIGHT_EYE).getPosition();

        float deltaX = Math.abs(leftEyeP.x - rightEyeP.x);
        float deltaY = Math.abs(leftEyeP.y - rightEyeP.y);

        float dist = 0f;
        if (deltaX >= deltaY) {
            dist = focalLength * (AVERAGE_EYE_DISTANCE / sensorX) * (imgWidth / deltaX) / 100;
        } else {
            dist = focalLength * (AVERAGE_EYE_DISTANCE / sensorY) * (imgHeight / deltaY) / 100;
        }

        //System.out.println(imgWidth+" "+imgWidth + " dist");

        boolean xOK = noseP.x > (middleX - maxDeviationX) && noseP.x < (middleX + maxDeviationX);
        boolean yOK = noseP.y > (middleY - maxDeviationY) && noseP.y < (middleY + maxDeviationY);

        int offset = 50;
        float et = dist;
        if(getActivity() != null)
            getActivity().runOnUiThread(() -> txtDebug.setText(et+""));
        boolean distanceOK = dist < preferred_measure_distance + offset && dist > preferred_measure_distance - offset;
        if (xOK && yOK && distanceOK) {
            facePositionCheckCounter++;
            startScanAnimation();
            ready = true;
        } else {
            animatedOval.stopAnimation();
            animatedOval.setVisibility(View.INVISIBLE);
            scanBar.clearAnimation();
            scanBar.setVisibility(View.INVISIBLE);
            ready = false;
            laskuri = 0;
            userTempList = null;

            userTemp = 0;
            facePositionCheckCounter--;
            if (facePositionCheckCounter < 0) facePositionCheckCounter = 0;
        }

        // angle correction if target is in specified distance and y position is not OK.
        if(dist < 500 && !yOK) {
            synchronized (this) {

                /*
                if(currentTiltAngle == setTiltAngle && tiltTimerRunning && !isAtSetAngle) {
                    isAtSetAngle = true;
                    tiltTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            tiltTimerRunning = false;
                        }
                    }, timerDelay);
                }*/

                if(!tiltTimerRunning /*&&isAtSetAngle*/ ) {
                    tiltTimerRunning = true;
                    isAtSetAngle = false;
                    // single angle correction. e.g. value 200 == 2 degrees
                    int correctionAngle = 0;
                    float objHeightPix = middleY - noseP.y;
                    float objHeightSensor = (sensor.getHeight() * objHeightPix) / imgHeight;
                    float realObjHeight = (dist * objHeightSensor) / focalLength;
                    System.out.println("realObjHeight : " + realObjHeight);
                    //double c = Math.sqrt(Math.pow(dist, 2) + Math.pow(objHeight, 2));
                    int a = (int) Math.round(Math.sin(realObjHeight*0.7 / dist)*100) *100;
                    if(currentTiltAngle >= 2200 && currentTiltAngle <= 9500) {
                        int angle = currentTiltAngle - a;
                        if (setTiltAngle < 2200) {
                            setTiltAngle = 2200;
                        } else if (setTiltAngle > 9500) {
                            setTiltAngle = 9500;
                        } else {
                            setTiltAngle = angle;
                        }
                        serialPortModel.changeTiltAngle((setTiltAngle) /100);
                        tiltTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                tiltTimerRunning = false;
                            }
                        }, timerDelay);
                        System.out.println("angle correction " + a);
                        getActivity().runOnUiThread(() -> txtDebug.setText(String.valueOf(a)));

                    }
                }
            }

        }
        updateProgress();
        return et;
    }

    private void updateProgress() {
        try {
            ProgressBar bar = getActivity().findViewById(R.id.face_check_prog);
            bar.setProgress(facePositionCheckCounter);
        } catch (Exception ignored) {

        }
    }


    public void faceDetected(Face face) {
        int width = 144;
        int height = 144;
        Bitmap live = hbb.getLiveMap();
        if(live != null){
            width = live.getWidth(); height = live.getHeight();
        }
        float dist = facePositionCheck(face, width, height);

        // getActivity().runOnUiThread(() -> txtDebug.setText(String.valueOf(laskuri)));
        if (ready) {
            if (userTempList == null) {
                userTempList = new ArrayList<>();
            }
            if (laskuri < 35) {
                float distDiff = (dist - 250)/100;
                float correction = distDiff * 0.404f *(-1);
                double correctedTemp = hbb.getHighestFaceTemperature() + correction - 0.8;
                userTempList.add(correctedTemp);

                if (hbb.getHighestFaceTemperature() > userTemp) {
                    userTemp = hbb.getHighestFaceTemperature();
                }
                laskuri++;
            } else {
                ready = false;
                laskuri = 0;
                //hasMeasured = true;
                saveMeasurementToJson();
                changeToResultLayout();


            }
        }
        stopIdleExecutor();
    }

    public void faceNotDetected() {
        scanBar.clearAnimation();
        animatedOval.stopAnimation();
        animatedOval.setVisibility(View.INVISIBLE);
        startIdleExecutor();
    }


    private void startIdleExecutor() {
        if (idleExecutor == null) {
            idleExecutor = new ScheduledThreadPoolExecutor(1);
        }
        // schedule the layout change if there isn't already a task going for it
        if (idleExecutor.getTaskCount() == 0) {
            idleExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    changeLayout();
                }
            }, 60, TimeUnit.SECONDS);
        }
    }

    private void stopIdleExecutor() {
        if (idleExecutor != null) {
            idleExecutor.shutdownNow();
            idleExecutor = null;
        }
    }

    private void changeLayout() {
        hbb.setMsfNull();
        Fragment f = new IntroFragment();
        getActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.slide_in_left, R.animator.slide_in_right, 0, 0)
                .replace(R.id.fragmentCamera, f, "menu").commit();
    }

    private void changeToResultLayout() {
        hbb.setMsfNull();

        Fragment f = new QR_code_fragment();
        Bundle args = new Bundle();
        args.putDouble("user_temp", userTemp);
        args.putDouble("avg_user_temp", avgUserTemp);
        f.setArguments(args);

        getActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.slide_in_left, R.animator.slide_in_right, 0, 0)
                .replace(R.id.fragmentCamera, f, "default").commit();
        System.out.println("commited");
    }


    private void startScanAnimation() {

        if (!ready) {
            animatedOval.init();
            animatedOval.setVisibility(View.VISIBLE);

            scanBar.setVisibility(View.VISIBLE);
            scanBar.bringToFront();
            scanBar.invalidate();
            scanBar.startAnimation(scanAnimation);
        }
    }

    private void saveMeasurementToJson() {
        if (userTempList != null) {
            avgUserTemp = userTempList.stream()
                    .mapToDouble(v -> v)
                    .average()
                    .getAsDouble();
        }

        try {
            JSONObject obj = measurementAccessObject.newEntry(avgUserTemp, new Date());
            measurementAccessObject.write(getContext(), obj, true);
        } catch (JSONException | IOException e) {
            Log.e(TAG, "Something went wrong while saving measurement to JSON " + e);
            e.printStackTrace();
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
        //System.out.println("MeasurementStartFragment heatmap update "+hbb.getHighestFaceTemperature());
        sendHeatmap(image);
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
    public void detectFace(Bitmap image) {
    }

    @Override
    public void writeToFile(byte[] data) {
    }

    public void updateData(LowResolution16BitCamera.TelemetryData data) {
        currentTiltAngle = data.tiltAngle;
        if(setTiltAngle == 0) {
            setTiltAngle = currentTiltAngle;
        }
    }


    @Override
    public void onNewHybridImage(Bitmap image) {
        if(getActivity() != null)
            getActivity().runOnUiThread(() -> heatkuva.setImageBitmap(image));
    }

    @Override
    public void sendHeatmap(Bitmap image) {
        hbb.setHeatmap(image);
    }
}
