package com.r00tme.radiojava;
import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


class PlayerAction  {


    private static MediaPlayer mediaPlayer = new MediaPlayer();

    private final Context context;
    private final Radio currentRadio;
    private static Radio previousRadio = null;

    public PlayerAction(Context context, Radio radio) {

        this.context  = context;
        this.currentRadio = radio;

        if(previousRadio == null){
            previousRadio = radio;
        }

        initPlayer();
    }

    private void initPlayer(){
        //Set CPU lock, draw battery all time until get released
        mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);

        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(String.valueOf(Uri.parse(currentRadio.getRadioUrl())));
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }

    //TODO For record functionality
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
            mediaPlayer = new MediaPlayer();
        }
    }

    //TODO For record functionality
    public MediaPlayer getInstance(){
        return mediaPlayer;
    }

    public void playMedia(){
        mediaPlayer.prepareAsync();
        mediaPlayer.setOnPreparedListener(android.media.MediaPlayer::start);
        mediaPlayer.setLooping(true);
        //Toast.makeText(context, "This radio is " + artist, Toast.LENGTH_SHORT).show();
        }
    }

