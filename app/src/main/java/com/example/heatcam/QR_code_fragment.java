package com.example.heatcam;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


public class QR_code_fragment extends Fragment {

    private TextView text, text1, text2;
    private ImageView imgView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.qr_code_fragment, container, false);

        text = view.findViewById(R.id.textView);
        text1 = view.findViewById(R.id.textView1);
        text2 = view.findViewById(R.id.textView2);
        text.setText(R.string.qr_instruction);
        text1.setText(R.string.FeedBack);
        text2.setText(R.string.otsikko);
        imgView = view.findViewById(R.id.qr_code);
        imgView.setImageResource(R.drawable.frame);

        // Inflate the layout for this fragment
        return view;
    }
}