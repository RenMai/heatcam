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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import static com.example.heatcam.PrivateKeyboard.Data.EmailConfig.saveInstance;

public class MainActivity extends AppCompatActivity {
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, -90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    String fileImage = null;
    private LinearLayout linearLayout;
    private ImageView profileImageView;
    private File visitorCardImageFile;
    // Deployment function URL: https://privatekeyboard.azurewebsites.net/api
    // Development function URL (example): http://192.168.1.149:7071/api

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pkb_activity_main);
    }

    //Reactive stuffs after getting back from other activities
    @Override
    protected void onResume() {
        super.onResume();
        linearLayout = findViewById(R.id.input_layout);
        ImageView qrImage = findViewById(R.id.qrImage);
        profileImageView = findViewById(R.id.takenImage);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            try {
                String savedImagePath = bundle.getString("image_path");
                this.fileImage = savedImagePath;
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
                ((ImageView) findViewById(R.id.visitorImage)).setImageBitmap(bitmap);
                profileImageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Button sendEmailButton = findViewById(R.id.sendEmailButton);
        sendEmailButton.setOnClickListener(view -> {
            updateVisitorCardFields();
            saveVisitorCard();
            sendEmail();
        });

        Button openCustomCameraButton = findViewById(R.id.buttonCam);
        openCustomCameraButton.setOnClickListener(v -> {
            saveInstance();
            Intent intent = new Intent(MainActivity.this, CustomCameraActivity.class);
            startActivity(intent);
        });

        if (!saveInstance.isEmpty()) {
            getInstance(saveInstance);
        }
        String functionUrl = "https://privatekeyboard.azurewebsites.net/api";
        HubConnection hubConnection = HubConnectionBuilder.create(functionUrl).build();

        hubConnection.on("sendInputField", (message) -> {
            Log.d("NewMessage", message.text);
            if (!message.sender.equals(QRUtils.connectedUuid)) return;
            LinearLayout inputField = (LinearLayout) linearLayout.getChildAt(message.targetInput);
            runOnUiThread(() -> ((EditText) inputField.getChildAt(1)).setText(message.text));
        }, NewMessage.class);

//        hubConnection.on("selectRadioGroup", (message) -> {
//            Log.d("NewCheckRadio", String.valueOf(message.targetRadioButton));
//            if (!message.sender.equals(QRUtils.connectedUuid)) return;
//
//            LinearLayout fieldLinearLayout = (LinearLayout) linearLayout.getChildAt(message.targetRadioGroup);
//            Log.d("NewMessageRadio", message.targetRadioGroup.toString());
//            RadioGroup radioGroup = (RadioGroup) fieldLinearLayout.getChildAt(1);
//            runOnUiThread(() -> ((RadioButton) radioGroup.getChildAt(message.targetRadioButton)).setChecked(true));
//        }, NewCheckRadio.class);

        hubConnection.on("updateTiltAngle", (message) -> {
            if (!message.sender.equals(QRUtils.connectedUuid)) return;
            Log.d("TiltAngle", String.valueOf(message.value));
            saveInstance.put("TextViewField-Tilt", String.valueOf(message.value));
            TextView tiltTextView = findViewById(R.id.tiltValue);
            runOnUiThread(() -> tiltTextView.setText("Angle:" + message.value));
        }, TiltAngle.class);

        hubConnection.on("pressButton", (message) -> {
            if (!message.sender.equals(QRUtils.connectedUuid)) return;
            Log.d("pressButton", String.valueOf(message.value));
            if (message.value.equals("on")) {
                openCustomCameraButton.callOnClick();
                hubConnection.stop();
            }else if (message.value.equals("sendEmail")) {
                Log.d("call", "calllled");
                runOnUiThread(() -> sendEmailButton.callOnClick());
            }
        }, TakingPicture.class);

        hubConnection.on("confirmQRScan", (message) -> {
            Log.d("ConfirmQRScan", message.uuid);
            if (!message.uuid.equals(QRUtils.newUuid)) return;
            // Set new QR bitmap to avoid duplicate connection
            QRUtils.SetNewQRBitmap(findViewById(R.id.qrImage), linearLayout);
            // hide the QR view after connecting successfully
            qrImage.setVisibility(View.INVISIBLE);
            // Set connection ID
            QRUtils.connectedUuid = message.uuid;
        }, ConfirmQRScan.class);
        //Start the connection
        hubConnection.start().blockingAwait();

        //Check if is there already a connection when go back from other activity
        QRUtils.SetNewQRBitmap(findViewById(R.id.qrImage), linearLayout);
        if (QRUtils.connectedUuid != null) {
            qrImage.setVisibility(View.INVISIBLE);

        }
    }

    private void saveInstance() {
//        saveInstance.put("RadioField-Sex", "No Response");
        saveInstance.clear();

        for (int i = 0; i < linearLayout.getChildCount(); i++) {
            LinearLayout fieldLayout = (LinearLayout) linearLayout.getChildAt(i);
            String fieldTag = (String) linearLayout.getChildAt(i).getTag();
            if (!fieldTag.equals("hidden")) {
                if (fieldLayout.getChildAt(1) instanceof EditText) {
                    saveInstance.put("InputField-" + i + "-" + ((TextView) fieldLayout.getChildAt(0)).getText(), ((EditText) fieldLayout.getChildAt(1)).getText().toString().trim());
                    Log.d("InputField", "InputField-" + i + "-" + ((TextView) fieldLayout.getChildAt(0)).getText());
                } /*else if (fieldLayout.getChildAt(1) instanceof RadioGroup) {
//                    if (((RadioButton) ((RadioGroup) fieldLayout.getChildAt(1)).getChildAt(0)).isChecked())
//                        saveInstance.put("RadioField-Sex", "Male");
//                    else if (((RadioButton) ((RadioGroup) fieldLayout.getChildAt(1)).getChildAt(1)).isChecked())
//                        saveInstance.put("RadioField-Sex", "Female");
//                }*/

            }
        }
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

    private void getInstance(HashMap<String, String> hashMap) {
        Set<String> keySet = hashMap.keySet();
        for (String key : keySet) {
            String[] arrOfStr = key.split("-", 3);
            Log.d("Instance",hashMap.get(key));

            if (arrOfStr[0].equals("InputField")) {
                LinearLayout inputField = (LinearLayout) linearLayout.getChildAt(Integer.parseInt(arrOfStr[1]));
                ((EditText) inputField.getChildAt(1)).setText(hashMap.get(key));
            } /*else if ((arrOfStr[0].equals("RadioField"))) {
//                RadioGroup radio = findViewById(R.id.radioSex);
//                switch (hashMap.get(key)) {
//                    case "Male":
//                        radio.check(R.id.radioMale);
//                        break;
//                    case "Female":
//                        radio.check(R.id.radioFemale);
//                        break;
//                }
            }*/ else {
                TextView tiltTextView = findViewById(R.id.tiltValue);
                tiltTextView.setText("Angle:" + hashMap.get(key));
            }
        }
    }

    private void updateVisitorCardFields() {
        ((TextView) findViewById(R.id.visitorName)).setText(((EditText) findViewById(R.id.fullNameText)).getText().toString());
        ((TextView) findViewById(R.id.hostName)).setText(((EditText) findViewById(R.id.hostNameText)).getText().toString());
        ((TextView) findViewById(R.id.companyName)).setText(((EditText) findViewById(R.id.companyNameText)).getText().toString());
        Date validDate = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        ((TextView) findViewById(R.id.visitDate)).setText(formatter.format(validDate));
    }

    private void saveVisitorCard() {
        View v1 = findViewById(R.id.visitorCard);
        v1.setDrawingCacheEnabled(true);
        v1.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        v1.layout(0, 0, v1.getMeasuredWidth(), v1.getMeasuredHeight());
        Bitmap testBitmap = Bitmap.createBitmap(v1.getDrawingCache());
        v1.setDrawingCacheEnabled(false);
        createVisitorCardFile(testBitmap);
    }

    private void createVisitorCardFile(Bitmap imageBitmap) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] fileData = stream.toByteArray();
            File path = new File(getApplicationContext().getFilesDir(), "Images");
            if (!path.exists()) {
                path.mkdirs();
            }
            visitorCardImageFile = new File(path, UUID.randomUUID().toString() + ".jpg");
            Log.d("CREATED_FILE", "createFile: " + visitorCardImageFile.getPath());
            FileOutputStream out = new FileOutputStream(visitorCardImageFile);
            out.write(fileData);
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendEmail() {
        //Getting content for clientEmail
        SendMail sm = new SendMail(this, ((EditText) findViewById(R.id.emailText)).getText().toString(), "Personal Information", visitorCardImageFile.getPath());
        sm.execute();
    }
}