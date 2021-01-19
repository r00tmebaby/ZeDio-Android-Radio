package com.r00tme.radiojava;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;

public class ActionChains{
    private static AsyncPlayer player = new AsyncPlayer("");
    private static Radio radioData;
    private  Context context;

    public ActionChains(Context context, Radio radioData) throws NullPointerException{
        super();
        if(radioData == null){
           throw new NullPointerException("Radio object can not be null");
        }
        this.radioData = radioData;
        this.context = context;
    }

    public void playRadio(){
        this.stopRadio();
        this.player.play(this.context, Uri.parse(this.radioData.getRadioUrl()), false, AudioManager.STREAM_MUSIC);
    }
    public  void stopRadio(){
        this.player.stop();
    }
}
