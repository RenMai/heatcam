package com.example.heatcam;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class User_result extends AppCompatActivity {

    // the value could be used of user temperature when userTemp is 100/real 39C etc.
    private int userTemp = 0;

    private Button buttonStart, buttonStart2, buttonStart3;
    private TextView text, text2;
    ProgressBar vProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_result);

        buttonStart = findViewById(R.id.start);
        buttonStart2 = findViewById(R.id.start2);
        buttonStart3 = findViewById(R.id.start3);
        text2 = findViewById(R.id.textView2);
        text2.setText("THERMAL CAMERA");
        vProgressBar = findViewById(R.id.vprogressbar3);


        buttonStart.setOnClickListener(v -> {
            userTemp = 90;
            // TODO Auto-generated method stub
            buttonStart.setClickable(false);
            new asyncTaskUpdateProgress().execute();
            text = findViewById(R.id.textView);

            text.setText(R.string.msgHightTmprt);
        });
        buttonStart2.setOnClickListener(v -> {
            userTemp = 60;
            // TODO Auto-generated method stub
            buttonStart2.setClickable(false);
            new asyncTaskUpdateProgress().execute();
            text = findViewById(R.id.textView);
            text.setText(R.string.msgNormTmprt);
        });
        buttonStart3.setOnClickListener(v -> {
            userTemp = 30;
            // TODO Auto-generated method stub
            buttonStart3.setClickable(false);
            new asyncTaskUpdateProgress().execute();
            text = findViewById(R.id.textView);
            text.setText(R.string.msgLowTmprt);
        });
    }


    public class asyncTaskUpdateProgress extends AsyncTask<Void, Integer, Void> {

        int progress;

        @Override
        protected void onPostExecute(Void result) {
            // TODO Auto-generated method stub
            buttonStart.setClickable(true);
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
            while (progress < userTemp) {
                progress++;
                publishProgress(progress);
                SystemClock.sleep(1);
            }
            return null;
        }


    }


}