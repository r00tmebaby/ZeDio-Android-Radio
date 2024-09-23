package com.r00tme.ZeDio.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.r00tme.ZeDio.R;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> {

    private final List<File> files;
    private final Context context;
    private final OnItemClickListener listener;
    private File currentlyPlayingFile = null;  // Track currently playing file

    public FolderAdapter(List<File> files, Context context, OnItemClickListener listener) {
        this.files = files;
        this.context = context;
        this.listener = listener;

        // Sort files and folders: directories first, then files
        sortFilesAndFolders();
    }

    // Sort the files: directories on top, files below
    private void sortFilesAndFolders() {
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                if (f1.isDirectory() && f2.isFile()) {
                    return -1; // Place directories before files
                } else if (f1.isFile() && f2.isDirectory()) {
                    return 1;  // Place files after directories
                } else {
                    return f1.getName().compareToIgnoreCase(f2.getName());  // Alphabetical order for both directories and files
                }
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_file_folder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = files.get(position);

        if (file.isDirectory()) {
            holder.icon.setImageResource(R.drawable.music_folder);
        } else if (file.isFile() && file.getName().endsWith(".mp3")) {
            if (file.equals(currentlyPlayingFile)) {
                holder.icon.setImageResource(R.drawable.play);  // Set the playing icon
            } else {
                holder.icon.setImageResource(R.drawable.song);  // Set the normal song icon
            }
        }

        holder.name.setText(file.getName());
        holder.itemView.setOnClickListener(v -> {
            // Update the currently playing file
            if (file.isFile() && file.getName().endsWith(".mp3")) {
                currentlyPlayingFile = file;  // Mark this file as playing
            } else {
                currentlyPlayingFile = null;  // No file playing in case of folder click
            }

            notifyDataSetChanged();  // Refresh the icons for all items

            // Trigger the actual click action
            listener.onItemClick(file);
        });
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public interface OnItemClickListener {
        void onItemClick(File file);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        ImageView icon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.file_name);
            icon = itemView.findViewById(R.id.file_icon);
        }
    }
}
