package com.r00tme.ZeDio.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.r00tme.ZeDio.R;
import com.r00tme.ZeDio.adapters.DownloadedSongsAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RecordsFragment extends Fragment {

    private DownloadedSongsAdapter adapter;
    private List<File> downloadedFiles = new ArrayList<>();
    private static MediaPlayer mediaPlayer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_records, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.records_layout);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new DownloadedSongsAdapter(downloadedFiles, getContext(), this::onFileClick);
        recyclerView.setAdapter(adapter);

        loadDownloadedSongs();

        return view;
    }

    private void loadDownloadedSongs() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {

            File musicDir = new File(String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)));
            if (musicDir.exists() && musicDir.isDirectory()) {
                File[] files = musicDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!file.getName().startsWith(".")) {
                            downloadedFiles.add(file);
                        }
                    }
                    adapter.notifyDataSetChanged();
                }
            } else {
                Toast.makeText(getContext(), "No recordings found", Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(getContext(), "Storage permission is required to load songs", Toast.LENGTH_SHORT).show();
        }
    }
    public static void stopMediaPlayerIfPlaying() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    private void onFileClick(File file) {
        // Stop the radio if it's playing
        if (RadioHomeFragment.player != null) {
            RadioHomeFragment.player.stopMedia(); // Ensure this method is available in PlayerAction
        }

        // Stop the media player if it's already playing
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(file.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            Toast.makeText(getContext(), "Playing: " + file.getName(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error playing file: " + file.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}