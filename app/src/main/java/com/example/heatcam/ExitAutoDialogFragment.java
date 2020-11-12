package com.example.heatcam;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

public class ExitAutoDialogFragment extends AppCompatDialogFragment {
    private EditText editTextPassword;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.enter_password_dialog, null);

        builder.setView(view);
            builder.setMessage("Enter password to exit auto mode.");



            builder.setPositiveButton("OK", (dialogInterface, i) -> {
                String password = editTextPassword.getText().toString();
                Fragment f = new MenuFragment();
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentCamera, f, "menu")
                        .commit();
                MainActivity.setAutoMode(false);


            });


        // User cancelled the dialog
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> {
        });

        editTextPassword = view.findViewById(R.id.input_password);

        return builder.create();



    }
}
