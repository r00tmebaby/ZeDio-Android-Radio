package com.r00tme.ZeDio.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.r00tme.ZeDio.R;
import com.r00tme.ZeDio.adapters.DownloadedSongsAdapter;
import com.r00tme.ZeDio.adapters.FolderAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RecordsFragment extends Fragment {

    private DownloadedSongsAdapter adapter;
    private FolderAdapter folderAdapter;
    private final List<File> downloadedFiles = new ArrayList<>();
    private static MediaPlayer mediaPlayer;
    public final File recordDirectory = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_MUSIC + File.separator + "ZeDio"
    );
    private static final int STORAGE_PERMISSION_CODE = 100;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_records, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.records_layout);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        folderAdapter = new FolderAdapter(downloadedFiles, getContext(), this::onFolderClick);
        recyclerView.setAdapter(folderAdapter);

        // Check for permission
        if (checkStoragePermission()) {
            loadFoldersAndSongs(null); // Load the base Music directory
        } else {
            requestStoragePermission(); // Request permission if not granted
        }

        return view;
    }
    // Method to check if the storage permission is granted
    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    // Method to request storage permission
    private void requestStoragePermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            // Display an explanation to the user (why you need the permission)
            Toast.makeText(getContext(), "Storage permission is required to load songs", Toast.LENGTH_SHORT).show();
        }
        // Request the permission
        ActivityCompat.requestPermissions(
                getActivity(),
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE
        );
    }
    // Handle the result of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            // If permission is granted, load the songs
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadFoldersAndSongs(null); // Load the songs if permission granted
            } else {
                // If permission is denied, show a message or take appropriate action
                Toast.makeText(getContext(), "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    /**
     * Loads folders and songs starting from the specified directory.
     * If directory is null, starts from the root music directory.
     */
    @SuppressLint("NotifyDataSetChanged")
    private void loadFoldersAndSongs(@Nullable File directory) {
        downloadedFiles.clear();

        File baseDir;
        if (directory == null) {
            baseDir = new File(String.valueOf(recordDirectory));
        } else {
            baseDir = directory;
        }

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {

            if (baseDir.exists() && baseDir.isDirectory()) {
                File[] files = baseDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!file.getName().startsWith(".")) {
                            downloadedFiles.add(file);
                        }
                    }
                    folderAdapter.notifyDataSetChanged();
                }
            } else {
                Toast.makeText(getContext(), "No recordings found", Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(getContext(), "Storage permission is required to load songs", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handles folder clicks (opens the folder) and MP3 file clicks (plays the song).
     */
    private void onFolderClick(File file) {
        if (file.isDirectory()) {
            loadFoldersAndSongs(file);  // Navigate into the folder
        } else if (file.isFile() && file.getName().endsWith(".mp3")) {
            onFileClick(file);  // Play the MP3 file
        }
    }

    /**
     * Handles media file click and plays the selected file.
     */
    private void onFileClick(File file) {
        // Stop the radio if it's playing
        if (RadioHomeFragment.player != null) {
            RadioHomeFragment.player.stopMedia(); // Ensure this method is available in PlayerAction
        }

        // Stop the media player if it's already playing
        stopMediaPlayerIfPlaying();

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

    /**
     * Stops the media player if it is currently playing.
     */
    public static void stopMediaPlayerIfPlaying() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
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
