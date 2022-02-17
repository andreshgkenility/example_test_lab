package com.dormmom.flutter_twilio_voice;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dormmom.flutter_twilio_voice.Utils.NotificationUtils;
import com.dormmom.flutter_twilio_voice.Utils.SoundUtils;
import com.dormmom.flutter_twilio_voice.Utils.TwilioConstants;
import com.dormmom.flutter_twilio_voice.Utils.TwilioUtils;
import com.twilio.voice.CallInvite;
import com.twilio.voice.CancelledCallInvite;

public class IncomingCallNotificationService extends Service {

    private static final String TAG = IncomingCallNotificationService.class.getSimpleName();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        Log.i(TAG, "onStartCommand " + action);
        if (action != null) {
            switch (action) {
                case TwilioConstants.ACTION_INCOMING_CALL: {
                    CallInvite callInvite = intent.getParcelableExtra(TwilioConstants.EXTRA_INCOMING_CALL_INVITE);
                    handleIncomingCall(callInvite);
                }
                break;

                case TwilioConstants.ACTION_ACCEPT: {
                    CallInvite callInvite = intent.getParcelableExtra(TwilioConstants.EXTRA_INCOMING_CALL_INVITE);
                    accept(callInvite);
                }
                break;
                case TwilioConstants.ACTION_REJECT: {
                    CallInvite callInvite = intent.getParcelableExtra(TwilioConstants.EXTRA_INCOMING_CALL_INVITE);
                    reject(callInvite);
                }
                break;
                case TwilioConstants.ACTION_CANCEL_CALL: {
                    CancelledCallInvite cancelledCallInvite = intent.getParcelableExtra(TwilioConstants.EXTRA_CANCELLED_CALL_INVITE);
                    handleCancelledCall(cancelledCallInvite);
                }
                break;
                case TwilioConstants.ACTION_RETURN_CALL: {
                    String to = intent.getStringExtra(TwilioConstants.EXTRA_CALL_TO);
                    returnCall(to);
                }
                break;
                case TwilioConstants.ACTION_STOP_SERVICE: {
                    stopServiceIncomingCall();
                }
                break;
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleIncomingCall(CallInvite callInvite) {
        if (callInvite == null) {
            Log.i(TAG, "Incoming call. No call invite");
            return;
        }

        Log.i(TAG, "Incoming call. App visible: " + isAppVisible() + ". Locked: " + isLocked());
        this.startServiceIncomingCall(callInvite);
    }

    private void accept(CallInvite callInvite) {
        Log.i(TAG, "Accept call invite. App visible: " + isAppVisible() + ". Locked: " + isLocked());
        this.stopServiceIncomingCall();

        if (!isLocked() && isAppVisible()) {
            // Inform call accepted
            Log.i(TAG, "Answering from APP");
            this.informAppAcceptCall(callInvite);
        } else {
            Log.i(TAG, "Answering from custom UI");
            this.openBackgroundCallActivityForAcceptCall(callInvite);
        }
    }

    private void reject(CallInvite callInvite) {
        Log.i(TAG, "Reject call invite. App visible: " + isAppVisible() + ". Locked: " + isLocked());
        this.stopServiceIncomingCall();

        // Reject call
        TwilioUtils.getInstance(this).rejectInvite(callInvite);
    }

    private void handleCancelledCall(CancelledCallInvite cancelledCallInvite) {
        Log.i(TAG, "Call canceled. App visible: " + isAppVisible() + ". Locked: " + isLocked());
        this.stopServiceIncomingCall();

        if (cancelledCallInvite == null) return;
        if (cancelledCallInvite.getFrom() == null) return;

        Log.i(TAG, "From: " + cancelledCallInvite.getFrom() + ". To: " + cancelledCallInvite.getTo());

        this.informAppCancelCall();

        // Create missed call notification
        Notification notification = NotificationUtils.createMissingCallNotification(
                getApplicationContext(),
                cancelledCallInvite.getFrom()
        );
        NotificationUtils.notify(getApplicationContext(), TwilioConstants.NOTIFICATION_MISSED_CALL, notification);
    }

    private void returnCall(String to) {
        Log.i(TAG, "Return call. App visible: " + isAppVisible() + ". Locked: " + isLocked());
        this.stopServiceIncomingCall();

        if (!isLocked() && isAppVisible()) {
            // Inform to the APP
            Log.i(TAG, "Returning call from APP");
            this.informAppReturnCall(to);
        } else {
            // Open custom UI
            Log.i(TAG, "Returning call from custom UI");
            this.openBackgroundCallActivityForReturnCall(to);
        }
    }

    private void startServiceIncomingCall(CallInvite callInvite) {
        Log.i(TAG, "Start service incoming call");
        SoundUtils.getInstance(this).playRinging();
        Notification notification = NotificationUtils.createIncomingCallNotification(getApplicationContext(), callInvite, true);
        startForeground(TwilioConstants.NOTIFICATION_INCOMING_CALL, notification);
    }

    private void stopServiceIncomingCall() {
        Log.i(TAG, "Stop service incoming call");
        stopForeground(true);
        NotificationUtils.cancel(this, TwilioConstants.NOTIFICATION_INCOMING_CALL);
        SoundUtils.getInstance(this).stopRinging();
    }

    private boolean isLocked() {
        KeyguardManager myKM = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        return myKM.inKeyguardRestrictedInputMode();
    }

    private boolean isAppVisible() {
        return App.visible;
    }


    // UTILS

    private void informAppAcceptCall(CallInvite callInvite) {
        Intent intent = new Intent();
        intent.putExtra(TwilioConstants.EXTRA_INCOMING_CALL_INVITE, callInvite);
        intent.setAction(TwilioConstants.ACTION_ACCEPT);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void informAppReturnCall(String to) {
        Intent intent = new Intent();
        intent.putExtra(TwilioConstants.EXTRA_CALL_TO, to);
        intent.setAction(TwilioConstants.ACTION_RETURN_CALL);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void informAppCancelCall() {
        Intent intent = new Intent();
        intent.setAction(TwilioConstants.ACTION_CANCEL_CALL);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void openBackgroundCallActivityForAcceptCall(CallInvite callInvite) {
        Intent intent = new Intent(this, BackgroundCallJavaActivity.class);
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK |
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        );
        intent.putExtra(TwilioConstants.EXTRA_INCOMING_CALL_INVITE, callInvite);
        intent.setAction(TwilioConstants.ACTION_ACCEPT);
        startActivity(intent);
    }

    private void openBackgroundCallActivityForReturnCall(String to) {
        Intent intent = new Intent(this, BackgroundCallJavaActivity.class);
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK |
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        );
        intent.putExtra(TwilioConstants.EXTRA_CALL_TO, to);
        intent.setAction(TwilioConstants.ACTION_RETURN_CALL);
        startActivity(intent);
    }
}
