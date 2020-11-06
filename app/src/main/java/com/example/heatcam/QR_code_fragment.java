package com.example.heatcam;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;


public class QR_code_fragment extends Fragment {

    private TextView text, text1, text2;
    private ImageView imgView;
    private long timer;
    private boolean t = true;

    private Timer task_timer;

    private LineChart measuresChart;
    private MeasurementAccessObject measurementAccessObject;
    private JSONArray measurementsJSON;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.qr_code_fragment, container, false);



        //moving background
        ConstraintLayout constraintLayout = (ConstraintLayout) view.findViewById(R.id.ConstraintLayout);
        AnimationDrawable animationDrawable = (AnimationDrawable) constraintLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();

        text = view.findViewById(R.id.textView);
        text1 = view.findViewById(R.id.textView1);
        text2 = view.findViewById(R.id.textView2);
        text.setText(R.string.qr_instruction);

        measuresChart = view.findViewById(R.id.measuresChart);

        measurementAccessObject = new MeasurementAccessObject();

        getPreviousMeasurements();
        initChart();

        // text1.setText(R.string.FeedBack);
        if (getArguments() != null && !getArguments().isEmpty()){
            System.out.println(getArguments() + " argumentit");
            double temp = (double)getArguments().get("user_temp");
            double avgTemp = (double) getArguments().get("avg_user_temp");
            //text1.setText("Your temp was: " + temp);
            //text1.append("\nYour avg temp was: " + avgTemp);
            if (37.5 >= temp && temp >= 35.5) {
                text1.setText(R.string.msgNormTmprt);

            } else if (temp > 37.5) {
                text1.setText(R.string.msgHightTmprt);
            } else {
                text1.setText(R.string.msgLowTmprt);
            }

        }
        text2.setText(R.string.title);

        // commented off to test graph from measurement data
        // imgView = view.findViewById(R.id.qr_code);
        // imgView.setImageResource(R.drawable.frame);




        task_timer = new Timer();
        task_timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Fragment fragment = new IntroFragment();
                getActivity().getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.animator.slide_in_left, R.animator.slide_in_right, 0, 0)
                        .replace(R.id.fragmentCamera, fragment, "measure_start")
                        .commit();
            }
        }, 50000);

        // freezes sometimes when exiting fragment
        /*
        timer = System.currentTimeMillis();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                while(timer + 90000 > System.currentTimeMillis()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Fragment fragment = new IntroFragment();
                getActivity().getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.animator.slide_in_left, R.animator.slide_in_right, 0, 0)
                        .replace(R.id.fragmentCamera, fragment, "measure_start")
                        .commit();
            }
        }, 5000);
        */
        // Inflate the layout for this fragment
        return view;
    }

    private void getPreviousMeasurements() {
        try {
            measurementsJSON = measurementAccessObject.read(getContext());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private void initChart() {
        XAxis xAxis = measuresChart.getXAxis();
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(6f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisLineWidth(2f);
        xAxis.setGridLineWidth(3f);
        xAxis.setGridColor(Color.WHITE);

        YAxis yAxis = measuresChart.getAxisLeft();
        yAxis.setAxisMinimum(20f);
        yAxis.setAxisMaximum(33f);
        yAxis.setAxisLineWidth(2f);
        yAxis.setGridLineWidth(3f);
        yAxis.setGridColor(Color.WHITE);


        measuresChart.getDescription().setEnabled(false);
        measuresChart.getAxisRight().setEnabled(false);
        measuresChart.getLegend().setTextSize(20f);

        initChartData();

    }

    private void initChartData() {
        ArrayList<Entry> measurements = new ArrayList<>();
        ArrayList<Entry> userMeasurement = new ArrayList<>();
        int y = -1;
        int z = 0;
        // we want to get only the last 6 measurements, the last will be the most recent measurement
        // meaning the last is what the user just measured
        if (measurementsJSON.length() > 6) { // if there are more than 6 measurements
            for (int x = 0; x < measurementsJSON.length(); x++) {
                if (x + 6 == measurementsJSON.length()) { // to check if there are only 6 left in JSON
                    y = x;
                }
                if (y != -1 && y < measurementsJSON.length() - 1) { // we loop through the last indexes
                    try {
                        measurements.add(new Entry(z, (float) measurementsJSON.getJSONObject(y).getDouble("Measured")));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    y++;
                    z++;
                }

            }
        } else { // there are less than 6 measurements so we can just loop the json without tricks
            for (int x = 0; x < measurementsJSON.length() - 1; x++) {
                try {
                    measurements.add(new Entry(z, (float) measurementsJSON.getJSONObject(x).getDouble("Measured")));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                z++;
            }
        }

        // need to add the last measurement to its own arraylist
        // so we can color the last dot in the graph with different color
        try {
            userMeasurement.add(new Entry(z, (float) measurementsJSON.getJSONObject(measurementsJSON.length()-1).getDouble("Measured")));
        } catch (JSONException e) {
            e.printStackTrace();
        }


        LineDataSet lineDataSet;
        LineDataSet userDataSet;
        if (measuresChart.getData() != null && measuresChart.getData().getDataSetCount() > 0) {
            lineDataSet = (LineDataSet) measuresChart.getData().getDataSetByIndex(0);
            lineDataSet.setValues(measurements);
            userDataSet = (LineDataSet) measuresChart.getData().getDataSetByIndex(0);
            userDataSet.setValues(userMeasurement);
            measuresChart.getData().notifyDataChanged();
            measuresChart.notifyDataSetChanged();
        } else {
            // change style for measurements before the user
            lineDataSet = new LineDataSet(measurements, "Mittaukset");
            lineDataSet.setCircleRadius(5f);
            lineDataSet.setColor(Color.GREEN);
            lineDataSet.setCircleColor(Color.GREEN);
            lineDataSet.setValueTextSize(18f);
            lineDataSet.setDrawCircleHole(false);
            lineDataSet.setDrawFilled(false);
            lineDataSet.setFormLineWidth(4f);
            lineDataSet.setFormSize(20f);
            lineDataSet.enableDashedLine(0, 1, 0);

            //change style for the measurement the user got
            userDataSet = new LineDataSet(userMeasurement, "Sinä");
            userDataSet.setDrawCircleHole(false);
            userDataSet.setValueTextSize(18f);
            userDataSet.setColor(Color.RED);
            userDataSet.setCircleRadius(5f);
            userDataSet.setValueTextSize(18f);
            userDataSet.setDrawFilled(false);
            userDataSet.setCircleColor(Color.RED);
            userDataSet.setFormLineWidth(4f);
            userDataSet.setFormSize(20f);
        }
        ArrayList<ILineDataSet> dataSet = new ArrayList<>();
        dataSet.add(lineDataSet);
        dataSet.add(userDataSet);
        LineData data = new LineData(dataSet);
        measuresChart.setData(data);
    }

    @Override
    public void onPause() {
        if(task_timer != null) {
            task_timer.cancel();
        }
        super.onPause();
    }
}