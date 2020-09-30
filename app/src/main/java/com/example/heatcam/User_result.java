package com.example.heatcam;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class User_result extends AppCompatActivity implements CameraListener {

    // the value could be used of user temperature when userTemp is 100/real 39C etc.
    private double userTemp = 0, correcTemp = 0;
    double temp = 0;

    private Button buttonStart3;
    private TextView text, text2;
    private boolean ready = false;

    private int laskuri = 0;
    ProgressBar vProgressBar;
    SerialPortModel serialPortModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_result);

        ConstraintLayout constraintLayout = (ConstraintLayout) findViewById(R.id.linearLayout);
        AnimationDrawable animationDrawable = (AnimationDrawable) constraintLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();


        //new asyncTaskUpdateProgress().execute();
        serialPortModel = SerialPortModel.getInstance();
        serialPortModel.setCamListener(this);
        buttonStart3 = findViewById(R.id.start3);
        text = findViewById(R.id.textView);
        text2 = findViewById(R.id.textView2);
        text2.setText(R.string.otsikko);
        vProgressBar = findViewById(R.id.vprogressbar3);

        buttonStart3.setOnClickListener(v -> {
            ready = true;
            userTemp = 0;
        });
    }

    @Override
    public void setConnectingImage() { }

    @Override
    public void setNoFeedImage() { }

    @Override
    public void updateImage(Bitmap image) {
        runOnUiThread(() -> {
            ((ImageView) findViewById(R.id.imageView)).setImageBitmap(image);
        });

    }

    @Override
    public void updateText(String text) { }

    @Override
    public void disconnect() { }

    @Override
    public void maxCelsiusValue(double max) {
        if (ready == true) {
            if (laskuri < 100) {
                if (max > userTemp) {
                    userTemp = max;
            }
                laskuri++;
            } else {
                ready = false;
                laskuri = 0;
                correcTemp = userTemp + 2.5;
                new asyncTaskUpdateProgress().execute();

                if (37.5 >= correcTemp && correcTemp >= 35.5) {
                    text.setText(R.string.msgNormTmprt);

                } else if (correcTemp > 37.5) {
                    text.setText(R.string.msgHightTmprt);
                } else {
                    text.setText(R.string.msgLowTmprt);
                }
            }
        }
    }

    @Override
    public void minCelsiusValue(double min) { }

    @Override
    public void detectFace(Bitmap image) { }

    @Override
    public void writeToFile(byte[] data) { }


    public class asyncTaskUpdateProgress extends AsyncTask<Void, Integer, Void> {

        int progress;

        @Override
        protected void onPostExecute(Void result) {
            // TODO Auto-generated method stub
           // buttonStart3.setClickable(true);


        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            progress = 0;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            // TODO Auto-generated method stub
            vProgressBar.setProgress(values[0]);
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            // TODO Auto-generated method stub
            double tmin = 29, tmax = 39;

            text2.setText(Double.toString(correcTemp));
            temp = (userTemp - tmin)/(tmax - tmin)*100;
            while (progress < temp) {
                progress++;
                publishProgress(progress);
                SystemClock.sleep(1);
            }
            return null;

        }





    }

}