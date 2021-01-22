package com.r00tme.radiojava;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestOptions;

import java.io.IOException;
import java.util.ArrayList;

public class RadioAdapter extends  RecyclerView.Adapter<RadioAdapter.ViewHolder>{
    private static final MediaPlayer mediaPlayer = new MediaPlayer();
    private static final String TAG = "RadioViewAdapter";
    private final ArrayList<Radio> radioList;
    private final Context mContext;

    public RadioAdapter(Context context, ArrayList<Radio> radioList) {
        this.radioList = radioList;
        this.mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) throws ArrayIndexOutOfBoundsException{
        Radio currentRadio = this.radioList.get(position);
        RequestOptions options = new RequestOptions()
                .priority(Priority.HIGH);
        Glide.with(mContext).asBitmap().load(currentRadio.getRadioLogo()).apply(options).into(holder.radioLogo);
        holder.radioName.setText(currentRadio.getRadioName());
        holder.radioGenre.setText(currentRadio.getRadioGenre());
        holder.radioCountry.setText(currentRadio.getRadioCountry());
        holder.radioViewLayout.setOnClickListener(v -> {
            Toast.makeText(mContext, "Loading: " + currentRadio.getRadioName(), Toast.LENGTH_LONG).show();
            Log.d(TAG, String.valueOf(mediaPlayer.isPlaying()));
            try {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(String.valueOf(Uri.parse(currentRadio.getRadioUrl())));
            } catch (IOException e) {
                e.printStackTrace();
            }
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
        });
    }



    @Override
    public int getItemCount() {
        return this.radioList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        ImageView radioLogo;
        TextView radioName;
        TextView radioGenre;
        TextView radioCountry;
        CardView radioViewLayout;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            radioLogo = itemView.findViewById(R.id.radio_logo);
            radioName = itemView.findViewById(R.id.radio_name);
            radioGenre = itemView.findViewById(R.id.radio_genre);
            radioCountry = itemView.findViewById(R.id.radio_country);
            radioViewLayout = itemView.findViewById(R.id.radio_view_layout);
        }
    }
}
