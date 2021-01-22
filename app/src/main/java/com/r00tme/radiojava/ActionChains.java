package com.r00tme.radiojava;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;

public class ActionChains{
    private static final AsyncPlayer player = new AsyncPlayer("");
    private static Radio radioData;
    private final Context context;

    public ActionChains(Context context, Radio radioData) throws NullPointerException{
        if(radioData == null){
           throw new NullPointerException("Radio object can not be null");
        }
        ActionChains.radioData = radioData;
        this.context = context;
    }

    public void playRadio(){
        this.stopRadio();
        player.play(this.context, Uri.parse(radioData.getRadioUrl()), false, AudioManager.STREAM_MUSIC);
    }
    public  void stopRadio(){
        player.stop();
    }
}
