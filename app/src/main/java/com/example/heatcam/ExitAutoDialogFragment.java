package com.example.heatcam;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

public class ExitAutoDialogFragment extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage("Exit auto mode?");
        builder.setPositiveButton("OK", (dialog, id) -> {
            Fragment f = new MenuFragment();
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentCamera, f, "menu")
                    .commit();
            MainActivity.setAutoMode(false);
        });
        builder.setNegativeButton("Cancel", (dialog, id) -> {
            // User cancelled the dialog
        });
        return builder.create();
    }
}
