package com.example.isla_beta.utilities;

import android.app.Notification;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Locale;
import java.util.Set;

public class NotificationListener extends NotificationListenerService {

    private static final String TAG = "NotificationListener";
    private static final String WA_PACKAGE = "com.whatsapp";
    private TextToSpeech textToSpeech;
    private PreferenceManager preferenceManager;

    @Override
    public void onListenerConnected() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        System.out.println(preferenceManager.getBoolean(Constants.KEY_BLUETOOTH_PERMISSION));
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    Locale bahasaIndonesia = new Locale("id", "ID");
                    int result = textToSpeech.setLanguage(bahasaIndonesia);

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    }
                } else {
                }
            }
        });
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!sbn.getPackageName().equals(WA_PACKAGE)) return;
        Notification notification = sbn.getNotification();
        Bundle bundle = notification.extras;
        String from = bundle.getString(NotificationCompat.EXTRA_TITLE);
        String message = bundle.getString(NotificationCompat.EXTRA_TEXT);

        checkBluetoothConnection();
        if(preferenceManager.getBoolean(Constants.KEY_BLUETOOTH_CONNECTION)){
            if (!from.equals("Whatsapp") || !message.contains("pesan dari") || !message.contains("pesan baru")) {
                String textToSpeak = "Terdapat pesan baru dari " + from;

                speakText(textToSpeak);
            }
        }

    }

    private void checkBluetoothConnection() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            preferenceManager.putBoolean(Constants.KEY_BLUETOOTH_CONNECTION, true);
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            preferenceManager.putBoolean(Constants.KEY_BLUETOOTH_CONNECTION, false);
            return;
        } else {
            preferenceManager.putBoolean(Constants.KEY_BLUETOOTH_CONNECTION, true);
        }
    }

    private void speakText(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}
