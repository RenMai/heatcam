package com.example.heatcam;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MenuFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.menu_layout, container, false);

        Fragment f = new CameraTestFragment();
        getActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.menu_dev_fragment, f, "default").commit();

        view.findViewById(R.id.menu_start_auto_button).setOnClickListener(v -> {
            Fragment fragment = new MeasurementStartFragment();
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.animator.slide_in_left, R.animator.slide_in_right, 0, 0)
                    .replace(R.id.fragmentCamera, fragment, "measure_start")
                    .commit();
            MainActivity.setAutoMode(true);
        });

        view.findViewById(R.id.menu_logs_button).setOnClickListener(v -> {
            startActivity(new Intent(getContext(), LogView.class));
        });

        return view;
    }
}
