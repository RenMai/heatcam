package com.example.heatcam.PrivateKeyboard;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.heatcam.PrivateKeyboard.Data.ConfirmQRScan;
import com.example.heatcam.PrivateKeyboard.Data.NewCheckRadio;
import com.example.heatcam.PrivateKeyboard.Data.NewMessage;
import com.example.heatcam.PrivateKeyboard.Data.TakingPicture;
import com.example.heatcam.PrivateKeyboard.Data.TiltAngle;
import com.example.heatcam.PrivateKeyboard.Helpers.QRUtils;
import com.example.heatcam.PrivateKeyboard.Helpers.SendMail;
import com.example.heatcam.R;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, -90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private final String functionUrl = "https://privatekeyboard.azurewebsites.net/api";
    private LinearLayout linearLayout;
    private ImageView profileImageView;
    // Deployment function URL: https://privatekeyboard.azurewebsites.net/api
    // Development function URL (example): http://192.168.1.149:7071/api
    // private final ActivityResultLauncher<Void> takePicturePreview = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), result -> ((ImageView) findViewById(R.id.qrImage)).setImageBitmap(result));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pkb_activity_main);

        linearLayout = findViewById(R.id.input_layout);
        ImageView qrImage = findViewById(R.id.qrImage);

        profileImageView = findViewById(R.id.takenImage);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            try {
                String savedImagePath = bundle.getString("image_path");

                File file = new File(savedImagePath);
                int size = (int) file.length();
                byte[] bytes = new byte[size];
                try {
                    BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                    buf.read(bytes, 0, bytes.length);
                    buf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
                profileImageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Button sendEmailButton = findViewById(R.id.sendEmailButton);
        Button openCustomCameraButton = findViewById(R.id.buttonCam);

        sendEmailButton.setOnClickListener(view -> {
            sendEmail();
        });

        openCustomCameraButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CustomCameraActivity.class);
            startActivity(intent);
        });

        linearLayout = findViewById(R.id.input_layout);
        HubConnection hubConnection = HubConnectionBuilder.create(functionUrl).build();

        hubConnection.on("sendInputField", (message) -> {
            Log.d("NewMessage", message.text);
            if (!message.sender.equals(QRUtils.connectedUuid)) return;
            LinearLayout inputField = (LinearLayout) linearLayout.getChildAt(message.targetInput);
            runOnUiThread(() -> ((EditText) inputField.getChildAt(1)).setText(message.text));
        }, NewMessage.class);

        hubConnection.on("selectRadioGroup", (message) -> {
            Log.d("NewCheckRadio", String.valueOf(message.targetRadioButton));
            if (!message.sender.equals(QRUtils.connectedUuid)) return;

            LinearLayout fieldLinearLayout = (LinearLayout) linearLayout.getChildAt(message.targetRadioGroup);
            RadioGroup radioGroup = (RadioGroup) fieldLinearLayout.getChildAt(1);
            runOnUiThread(() -> ((RadioButton) radioGroup.getChildAt(message.targetRadioButton)).setChecked(true));
        }, NewCheckRadio.class);

        hubConnection.on("updateTiltAngle", (message) -> {
            if (!message.sender.equals(QRUtils.connectedUuid)) return;

            Log.d("TiltAngle", String.valueOf(message.value));
            TextView tiltTextView = findViewById(R.id.tiltValue);
            tiltTextView.setText("Angle:" + message.value);
        }, TiltAngle.class);

        hubConnection.on("takePicture", (message) -> {
            if (!message.sender.equals(QRUtils.connectedUuid)) return;
            Log.d("isTakingPicture", String.valueOf(message.value));
            if(message.value == true) {
                Intent intent = new Intent(MainActivity.this, CustomCameraActivity.class);
                startActivity(intent);
            }

        }, TakingPicture.class);

        hubConnection.on("confirmQRScan", (message) -> {
            Log.d("ConfirmQRScan", message.uuid);
            if (!message.uuid.equals(QRUtils.newUuid)) return;
            // hide the QR view after connecting successfully
            qrImage.setVisibility(View.INVISIBLE);
            QRUtils.connectedUuid = message.uuid;
            QRUtils.SetNewQRBitmap(findViewById(R.id.qrImage), linearLayout);
        }, ConfirmQRScan.class);

        hubConnection.start().blockingAwait();

        QRUtils.SetNewQRBitmap(findViewById(R.id.qrImage), linearLayout);
    }

    private void sendEmail() {
        //Getting content for email
        String email = ((EditText) findViewById(R.id.editTextEmail)).getText().toString().trim();
        String subject = "Personal Information";
        String message = "Hiiiiiiiiiiiii";

        //Creating SendMail object
        SendMail sm = new SendMail(this, email, subject, message);

        //Executing sendmail to send email
        sm.execute();
    }

    private void rotateImageToUpright(Bitmap source) {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        float angle = ORIENTATIONS.get(rotation);
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(MainActivity.this, "Landscape Mode", Toast.LENGTH_LONG).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Toast.makeText(MainActivity.this, "Portrait Mode", Toast.LENGTH_LONG).show();
        }
        rotateImageToUpright(((BitmapDrawable) profileImageView.getDrawable()).getBitmap());
    }
}