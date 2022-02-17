package com.dormmom.flutter_twilio_voice.Utils;

import static android.content.Context.AUDIO_SERVICE;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import com.dormmom.flutter_twilio_voice.R;

public class SoundUtils {
    private boolean playing = false;
    private boolean loaded = false;
    private boolean playingCalled = false;
    private final float volume;
    private final SoundPool soundPool;
    private final int ringingSoundId;
    private int ringingStreamId;
    private final int disconnectSoundId;
    private static SoundUtils instance;
    private final Vibrator vibrator;
    private final AudioManager audioManager;

    private SoundUtils(Context context) {
        // AudioManager audio settings for adjusting the volume
        audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
        float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        volume = actualVolume / maxVolume;

        // Load the sounds
        int maxStreams = 1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(maxStreams)
                    .build();
        } else {
            soundPool = new SoundPool(maxStreams, AudioManager.STREAM_MUSIC, 0);
        }

        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                loaded = true;
                if (playingCalled && sampleId == ringingSoundId) {
                    playRinging();
                    playingCalled = false;
                }
            }

        });
        ringingSoundId = soundPool.load(context, R.raw.incoming, 1);
        disconnectSoundId = soundPool.load(context, R.raw.disconnect, 1);
        // Get instance of Vibrator from current Context
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public static SoundUtils getInstance(Context context) {
        if (instance == null) {
            instance = new SoundUtils(context);
        }
        return instance;
    }

    public void playRinging() {
        if (loaded && !playing) {

            switch (audioManager.getRingerMode()) {
                case AudioManager.RINGER_MODE_NORMAL:
                    ringingStreamId = soundPool.play(ringingSoundId, volume, volume, 1, -1, 1f);
                    vibrate();
                    break;
                case AudioManager.RINGER_MODE_VIBRATE:
                    vibrate();
                    break;
                default:
                    break;
            }
            playing = true;
        } else {
            playingCalled = true;
        }
    }

    private void vibrate() {
        long[] mVibratePattern = new long[]{0, 400, 400, 400, 400, 400, 400, 400};
        final int[] mAmplitudes = new int[]{0, 128, 0, 128, 0, 128, 0, 128};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(mVibratePattern, mAmplitudes, 0));
        } else {
            //deprecated in API 26
            vibrator.vibrate(mVibratePattern, 0);
        }
    }

    public void stopRinging() {
        if (playing) {
            soundPool.stop(ringingStreamId);
            vibrator.cancel();
            playing = false;
        }
    }

    public void playDisconnect() {
        if (loaded && !playing) {
            soundPool.play(disconnectSoundId, volume, volume, 1, 0, 1f);
            playing = false;
        }
    }

}
