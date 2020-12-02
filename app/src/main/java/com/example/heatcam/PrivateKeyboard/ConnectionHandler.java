package com.example.heatcam.PrivateKeyboard;

import android.util.Log;
import com.example.heatcam.PrivateKeyboard.Data.ConfirmQRScan;
import com.example.heatcam.PrivateKeyboard.Data.NewMessage;
import com.example.heatcam.PrivateKeyboard.Data.TakingPicture;
import com.example.heatcam.PrivateKeyboard.Data.TiltAngle;
import com.example.heatcam.PrivateKeyboard.Helpers.QRUtils;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;

import static com.example.heatcam.PrivateKeyboard.Data.EmailConfig.saveInstance;

public class ConnectionHandler {
    private String functionUrl;
    private HubConnection hubConnection;

    private ConnectionListener listener;

    private void initConnection() {
        hubConnection = HubConnectionBuilder
                .create(functionUrl)
                .build();

        hubConnection.on("sendInputField", (message) -> {
            Log.d("NewMessage", message.text);
            if (!message.sender.equals(QRUtils.connectedUuid)) return;
            listener.onSendInputField(message);
        }, NewMessage.class);

        hubConnection.on("updateTiltAngle", (message) -> {
            if (!message.sender.equals(QRUtils.connectedUuid)) return;
            Log.d("TiltAngle", String.valueOf(message.value));
            saveInstance.put("TextViewField-Tilt", String.valueOf(message.value));
            listener.onUpdateTiltAngle(message);
        }, TiltAngle.class);

        hubConnection.on("pressButton", (message) -> {
            if (!message.sender.equals(QRUtils.connectedUuid)) return;
            Log.d("pressButton", String.valueOf(message.value));
            listener.onPressButton(message);
        }, TakingPicture.class);

        hubConnection.on("confirmQRScan", (message) -> {
            Log.d("ConfirmQRScan", message.uuid);
            if (!message.uuid.equals(QRUtils.newUuid)) return;
            listener.onConfirmQRScan(message);
        }, ConfirmQRScan.class);

        //Start the connection
        hubConnection.start().blockingAwait();

    }

    public void stop() {
        hubConnection.stop();
    }

    public void setListener(ConnectionListener listener) {
        this.listener = listener;
    }

    private ConnectionHandler(String functionUrl) {
        this.functionUrl = functionUrl;
    }

    private ConnectionHandler() {
        this("https://privatekeyboard.azurewebsites.net/api");
    }

    public ConnectionHandler(ConnectionListener listener) {
        this();
        this.listener = listener;
        initConnection();
    }

    public ConnectionHandler(ConnectionListener listener, String functionUrl) {
        this(functionUrl);
        this.listener = listener;
        initConnection();
    }

}
