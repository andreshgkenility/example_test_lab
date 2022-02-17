package com.dormmom.flutter_twilio_voice;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dormmom.flutter_twilio_voice.Utils.PreferencesUtils;
import com.dormmom.flutter_twilio_voice.Utils.TwilioConstants;
import com.dormmom.flutter_twilio_voice.Utils.TwilioUtils;
import com.squareup.picasso.Picasso;
import com.twilio.voice.Call;
import com.twilio.voice.CallException;
import com.twilio.voice.CallInvite;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class BackgroundCallJavaActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "BackgroundCallActivity";

    private PowerManager.WakeLock wakeLock;
    private ImageView image;
    private TextView textDisplayName;
    private TextView textPhoneNumber;
    private TextView textCallStatus;
    private TextView textTimer;
    private ImageView btnMute;
    private ImageView btnSpeaker;
    private View containerIncomingCall;
    private View containerActiveCall;
    private View containerLock;
    private CustomBroadCastReceiver customBroadCastReceiver;
    private boolean broadcastReceiverRegistered = false;
    private CallInvite callInvite;
    private boolean exited = false;
    private SensorManager sensorManager;
    private Sensor sensor;
    private boolean previouslySpeaker = false;
    private Timer timer;
    private int seconds = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_background_call);

        this.image = findViewById(R.id.image);
        this.textDisplayName = findViewById(R.id.textDisplayName);
        this.textPhoneNumber = findViewById(R.id.textPhoneNumber);
        this.textCallStatus = findViewById(R.id.textStatus);
        this.textCallStatus.setVisibility(View.GONE);
        this.textTimer = findViewById(R.id.textTimer);
        this.textTimer.setVisibility(View.GONE);

        this.btnMute = findViewById(R.id.btnMute);
        this.btnMute.setOnClickListener((v) -> this.toggleMute());

        this.btnSpeaker = findViewById(R.id.btnSpeaker);
        this.btnSpeaker.setOnClickListener((v) -> this.toggleSpeaker());

        ImageView btnHangUp = findViewById(R.id.btnHangUp);
        btnHangUp.setOnClickListener(v -> this.hangUp());

        ImageView btnAccept = findViewById(R.id.btnAccept);
        btnAccept.setOnClickListener(v -> this.acceptCall());

        ImageView btnReject = findViewById(R.id.btnReject);
        btnReject.setOnClickListener(v -> this.rejectCall());

        this.containerActiveCall = findViewById(R.id.containerActiveCall);
        this.containerActiveCall.setVisibility(View.GONE);
        this.containerLock = findViewById(R.id.containerLock);

        this.containerIncomingCall = findViewById(R.id.containerIncomingCall);
        this.containerIncomingCall.setVisibility(View.GONE);

        TwilioUtils.getInstance(this).setSpeaker(false);
        applyColorToButton(this.btnSpeaker, false);
        applyColorToButton(this.btnMute, false);

        this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        this.sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        this.turnScreenOnAndKeyguardOff();

        handleIntent(getIntent());
        registerReceiver();
    }

    private boolean isSpeaker() {
        AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        return audioManager.isSpeakerphoneOn();
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wakeLock != null) {
            wakeLock.release();
        }

        this.unregisterReceiver();
    }


    @Override
    protected void onResume() {
        super.onResume();
        this.sensorManager.registerListener(this, this.sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        float distance = event.values[0];
        if (distance == 0.0) {
            this.lockScreen();
        } else {
            this.unlockScreen();
        }
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void turnScreenOnAndKeyguardOff() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true);
            setShowWhenLocked(true);
        } else {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            this.wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "lock:" + TAG);
            this.wakeLock.acquire(60 * 24 * 60 * 1000L /*24hs*/);

            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN |
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            );
        }

        KeyguardManager kgm = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        Log.d(TAG, "isKeyguardUp $isKeyguardUp");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            kgm.requestDismissKeyguard(this, null);
        }
    }

    private void lockScreen() {
        this.previouslySpeaker = this.isSpeaker();
        if (this.previouslySpeaker) {
            this.toggleSpeaker();
        }
        this.containerLock.setVisibility(View.VISIBLE);
    }

    private void unlockScreen() {
        if (this.previouslySpeaker) {
            this.toggleSpeaker();
        }
        this.containerLock.setVisibility(View.GONE);
    }

    private void registerReceiver() {
        if (broadcastReceiverRegistered) return;

        this.broadcastReceiverRegistered = true;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TwilioConstants.ACTION_CANCEL_CALL);
        intentFilter.addAction(TwilioConstants.ACTION_REJECT);
        this.customBroadCastReceiver = new CustomBroadCastReceiver(this);
        LocalBroadcastManager.getInstance(this).registerReceiver(customBroadCastReceiver, intentFilter);
    }

    private void unregisterReceiver() {
        if (!broadcastReceiverRegistered) return;
        this.broadcastReceiverRegistered = false;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(customBroadCastReceiver);
    }

    private void handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            close();
            return;
        }

        switch (intent.getAction()) {
            case TwilioConstants.ACTION_INCOMING_CALL: {
                this.callInvite = intent.getParcelableExtra(TwilioConstants.EXTRA_INCOMING_CALL_INVITE);
                containerIncomingCall.setVisibility(View.VISIBLE);
                containerActiveCall.setVisibility(View.GONE);
                updateCallDetails(this.callInvite.getFrom());
            }
            break;

            case TwilioConstants.ACTION_ACCEPT: {
                this.callInvite = intent.getParcelableExtra(TwilioConstants.EXTRA_INCOMING_CALL_INVITE);
                containerIncomingCall.setVisibility(View.GONE);
                containerActiveCall.setVisibility(View.VISIBLE);
                updateCallDetails(this.callInvite.getFrom());
                this.acceptCall();
            }
            break;

            case TwilioConstants.ACTION_CANCEL_CALL: {
                onCallCanceled();
            }
            break;

            case TwilioConstants.ACTION_RETURN_CALL: {
                String to = intent.getStringExtra(TwilioConstants.EXTRA_CALL_TO);
                containerIncomingCall.setVisibility(View.GONE);
                containerActiveCall.setVisibility(View.VISIBLE);
                updateCallDetails(to);
                this.returnCall(to);
            }
            break;
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent == null || intent.getAction() == null) return;

        if (TwilioConstants.ACTION_CANCEL_CALL.equals(intent.getAction())) {
            onCallCanceled();
        }
    }

    private void stopServiceIncomingCall() {
        Intent intent = new Intent(this, IncomingCallNotificationService.class);
        intent.setAction(TwilioConstants.ACTION_STOP_SERVICE);
        startService(intent);
    }

    private void acceptCall() {
        stopServiceIncomingCall();

        if (this.callInvite == null) {
            Log.i(TAG, "No call invite");
            this.close();
            return;
        }

        this.containerActiveCall.setVisibility(View.VISIBLE);
        this.containerIncomingCall.setVisibility(View.GONE);

        try {
            TwilioUtils.getInstance(this).acceptInvite(this.callInvite, getListener());
        } catch (Exception exception) {
            Log.i(TAG, "Error accepting call. " + exception.getMessage());
            this.close();
        }
    }

    private void rejectCall() {
        stopServiceIncomingCall();

        if (this.callInvite == null) {
            Log.i(TAG, "No call invite");
            this.close();
            return;
        }

        this.callInvite.reject(this);
        this.close();
    }


    private void returnCall(String to) {
        Log.i(TAG, "Returning call to: " + to);
        to = to.replace("client:", "");

        try {
            TwilioUtils.getInstance(this).makeCall(to, getListener());
        } catch (Exception exception) {
            Log.i(TAG, "Error returning call. " + exception.getMessage());
            this.close();
        }
    }


    private void hangUp() {
        TwilioUtils.getInstance(this).disconnect();
        this.close();
    }

    private void onCallCanceled() {
        TwilioUtils.getInstance(this).disconnect();
        this.close();
    }

    private void toggleMute() {
        boolean muted = TwilioUtils.getInstance(this).toggleMute();
        applyColorToButton(this.btnMute, muted);
        this.btnMute.setImageResource(muted ? R.drawable.ic_mic_off : R.drawable.ic_mic);
    }

    private void toggleSpeaker() {
        boolean speaker = TwilioUtils.getInstance(this).toggleSpeaker();
        applyColorToButton(this.btnSpeaker, speaker);
    }

    private void applyColorToButton(ImageView view, boolean value) {
        int backgroundColor;
        int iconColor;
        if (value) {
            backgroundColor = getResources().getColor(R.color.call_btn_fill);
            iconColor = getResources().getColor(R.color.on_call_btn_fill);
        } else {
            backgroundColor = getResources().getColor(R.color.call_btn);
            iconColor = getResources().getColor(R.color.on_call_btn);
        }

        Drawable background = view.getBackground();
        DrawableCompat.setTint(background, backgroundColor);
        ImageViewCompat.setImageTintList(view, ColorStateList.valueOf(iconColor));
    }

    private void updateCallDetails(String from) {
        HashMap<String, Object> call = TwilioUtils.getInstance(this).getCallDetails();

        String status = (String) call.get("status");
        if (status != null && !status.trim().equals("")) {
            switch (status) {
                case "callRinging": {
                    this.textCallStatus.setVisibility(View.VISIBLE);
                    textCallStatus.setText(R.string.call_status_ringing);
                }
                break;

                case "callReconnecting": {
                    this.textCallStatus.setVisibility(View.VISIBLE);
                    textCallStatus.setText(R.string.call_status_reconnecting);
                }
                break;

                default: {
                    this.textCallStatus.setVisibility(View.GONE);
                }
                break;
            }
        } else {
            this.textCallStatus.setVisibility(View.VISIBLE);
            textCallStatus.setText(R.string.call_status_connecting);
        }

        // Display name
        String displayName;
        if (from != null && !from.trim().equals("")) {
            displayName = PreferencesUtils.getInstance(this).findContactName(from);
        } else {
            displayName = (String) call.get("toDisplayName");
        }

        if (displayName == null || displayName.trim().equals("")) {
            displayName = PreferencesUtils.getInstance(this).getDefaultDisplayName();
        }
        this.textDisplayName.setText(displayName);

        // Phone number
        String phoneNumber;
        if (from != null && !from.trim().equals("")) {
            phoneNumber = from;
        } else {
            phoneNumber = (String) call.get("to");
        }

        if (phoneNumber != null && !phoneNumber.trim().equals("")) {
            this.textPhoneNumber.setText(phoneNumber);
        } else {
            this.textPhoneNumber.setText("");
        }

        // Image
        String imageURL;
        if (from != null && !from.trim().equals("")) {
            imageURL = PreferencesUtils.getInstance(this).findPhotoURL(from);
        } else {
            imageURL = (String) call.get("toPhotoURL");
        }

        if (imageURL != null && !imageURL.trim().equals("")) {
            Picasso.get().load(imageURL).into(this.image);
        } else {
            Picasso.get().load("https://stonegatesl.com/wp-content/uploads/2021/01/avatar-300x300.jpg").into(this.image);
        }

        // Timer
        if (status != null && status.equals("callConnected")) {
            this.startTimer();
        } else {
            this.stopTimer();
        }
    }

    private void close() {
        if (this.exited) return;

        if (this.wakeLock != null && this.wakeLock.isHeld()) {
            this.wakeLock.release();
        }

        this.stopTimer();
        this.exited = true;
        this.finish();
    }

    @Override
    public void finish() {
        this.stopTimer();
        super.finish();
    }


    private void startTimer() {
        this.textTimer.setVisibility(View.VISIBLE);
        this.textTimer.setText(DateUtils.formatElapsedTime(0));

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                seconds += 1;
                runOnUiThread(new TimerTask() {
                    @Override
                    public void run() {
                        textTimer.setText(DateUtils.formatElapsedTime(seconds));
                    }
                });
            }
        }, 0, 1000);
    }

    private void stopTimer() {
        if (this.timer != null) {
            this.timer.cancel();
            this.timer = null;
        }
        this.textTimer.setVisibility(View.GONE);
    }


    Call.Listener getListener() {
        return new Call.Listener() {
            @Override
            public void onConnectFailure(@NonNull Call call, @NonNull CallException callException) {
                updateCallDetails(null);
                close();
            }

            @Override
            public void onRinging(@NonNull Call call) {
                updateCallDetails(null);
            }

            @Override
            public void onConnected(@NonNull Call call) {
                updateCallDetails(null);
            }

            @Override
            public void onReconnecting(@NonNull Call call, @NonNull CallException callException) {
                updateCallDetails(null);
            }

            @Override
            public void onReconnected(@NonNull Call call) {
                updateCallDetails(null);
            }

            @Override
            public void onDisconnected(@NonNull Call call, @Nullable CallException callException) {
                updateCallDetails(null);
                close();
            }
        };
    }


    private static class CustomBroadCastReceiver extends BroadcastReceiver {

        private final BackgroundCallJavaActivity activity;

        private CustomBroadCastReceiver(BackgroundCallJavaActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Received broadcast for action " + action);

            if (action == null) return;
            if (TwilioConstants.ACTION_CANCEL_CALL.equals(action)) {
                activity.onCallCanceled();
            }

            if (TwilioConstants.ACTION_REJECT.equals(action)) {
                activity.onCallCanceled();
            }
        }
    }
}