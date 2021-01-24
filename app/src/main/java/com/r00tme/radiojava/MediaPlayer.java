package com.r00tme.radiojava;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

class PlayerAction  {

    private static MediaPlayer mediaPlayer = new MediaPlayer();

    private final Context context;
    private final Radio radio;
    private static final String LOG_TAG = "AudioRecordTest";

    public PlayerAction(Context context, Radio radio) {
        this.context  = context;
        this.radio = radio;
        initPlayer();
    }

    private void initPlayer(){
        //Set CPU lock, draw battery all time until get released
        mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);

        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(String.valueOf(Uri.parse(radio.getRadioUrl())));
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }

    public void recordRadio(){

        PackageManager androidManager = context.getPackageManager();
        if (androidManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {

            String fileName = Environment.getExternalStorageDirectory().getAbsolutePath();
            fileName += "/".concat(radio.getRadioName()).concat(getDate())+".mp3";

            MediaRecorder mediaRecorder = new MediaRecorder();

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(fileName);
            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
            } catch (IOException e) {
                Log.e(LOG_TAG, "prepare() failed");
            }
        } else { // no mic on device
            Toast.makeText(context, "This device doesn't have a mic!", Toast.LENGTH_LONG).show();
        }
    }

    /** Method takes format date string as an argument and returns the formatted date
     *
     */

    private String getDate(){
        @SuppressLint("SimpleDateFormat") SimpleDateFormat  formatter =
                new SimpleDateFormat("dd-MM-yyyy HH-mm-ss");
        Date date = new Date();
        return formatter.format(date);
    }

    public void stopMedia(){
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void playMedia(){
        mediaPlayer.prepareAsync();
        mediaPlayer.setOnPreparedListener(android.media.MediaPlayer::start);
        mediaPlayer.setLooping(true);
    }

}
