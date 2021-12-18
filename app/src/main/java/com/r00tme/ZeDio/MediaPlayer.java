package com.r00tme.ZeDio;
import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.RecursiveTask;


class PlayerAction  {


    private static MediaPlayer mediaPlayer = new MediaPlayer();

    private final Context context;
    private final Radio currentRadio;
    private static Radio previousRadio = null;
    WifiManager.WifiLock wifiLock;

    public PlayerAction(Context context, Radio radio) {

        this.context  = context;
        this.currentRadio = radio;
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "ZedioLock");

        if(previousRadio == null){
            previousRadio = radio;
        }

        initPlayer();
    }

    private void initPlayer(){
        //Set CPU lock, draw battery all time until get released
        mediaPlayer.reset();
        mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
        wifiLock.acquire();

        try {
            mediaPlayer.setDataSource(currentRadio.getRadioUrl());
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

    public void recordMedia() throws IOException {
            MediaRecorder mr = new MediaRecorder();
            String filePath = Environment.getDownloadCacheDirectory() + File.separator+ getDate() + ".aac" + File.separator;
            mr.setAudioSource(MediaRecorder.AudioSource.MIC);
            mr.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
            mr.setOutputFile(filePath);
            //Toast.makeText(context, filePath, Toast.LENGTH_LONG).show();
            mr.prepare();
            mr.start();


    }

    public void stopMedia(){
        if (wifiLock != null) {
            if (wifiLock.isHeld()) {
                wifiLock.release();
            }
        }
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            mediaPlayer = new MediaPlayer();
        }
    }

    public void playMedia(){
        mediaPlayer.prepareAsync();
        mediaPlayer.setOnPreparedListener(android.media.MediaPlayer::start);
        mediaPlayer.setLooping(true);
        //Toast.makeText(context, "This radio is " + artist, Toast.LENGTH_SHORT).show();
        }
    }

