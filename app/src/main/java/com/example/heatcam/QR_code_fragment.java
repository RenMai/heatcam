package com.example.heatcam;

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


public class QR_code_fragment extends Fragment {

    private TextView text, text1, text2;
    private ImageView imgView;
    private long timer;
    private boolean t = true;

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

        // text1.setText(R.string.FeedBack);
        if (!getArguments().isEmpty()){
            System.out.println(getArguments() + " argumentit");
            double temp = (double)getArguments().get("user_temp");
            text1.setText("Your temp was: " + temp);
        }
        text2.setText(R.string.title);
        imgView = view.findViewById(R.id.qr_code);
        imgView.setImageResource(R.drawable.frame);


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
}