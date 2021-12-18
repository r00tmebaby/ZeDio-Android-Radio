package com.r00tme.radiojava;
import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.PowerManager;

import com.vincan.medialoader.DefaultConfigFactory;
import com.vincan.medialoader.DownloadManager;
import com.vincan.medialoader.MediaLoader;
import com.vincan.medialoader.MediaLoaderConfig;
import com.vincan.medialoader.data.file.naming.HashCodeFileNameCreator;

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
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiManager.WifiLock wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "MyWifiLock");
        wifiLock.acquire();
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

    public void recordMedia() throws IOException {
    }

    public void stopMedia(){

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

