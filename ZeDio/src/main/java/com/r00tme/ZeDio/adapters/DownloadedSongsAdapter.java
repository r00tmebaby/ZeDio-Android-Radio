package com.r00tme.ZeDio.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.r00tme.ZeDio.R;

import java.io.File;
import java.util.List;

public class DownloadedSongsAdapter extends RecyclerView.Adapter<DownloadedSongsAdapter.SongViewHolder> {

    private final List<File> songs;
    private final Context context;
    private final OnSongClickListener onSongClickListener;

    public interface OnSongClickListener {
        void onSongClick(File song);
    }

    public DownloadedSongsAdapter(List<File> songs, Context context, OnSongClickListener listener) {
        this.songs = songs;
        this.context = context;
        this.onSongClickListener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.songs_vew, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        File song = songs.get(position);
        holder.songName.setText(song.getName());

        holder.itemView.setOnClickListener(v -> onSongClickListener.onSongClick(song));
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {

        TextView songName;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            songName = itemView.findViewById(R.id.song_name);
        }
    }
}
