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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.r00tme.ZeDio.R;
import com.r00tme.ZeDio.adapters.FolderAdapter;
import com.r00tme.ZeDio.utils.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment to display and manage downloaded recordings. Handles folder navigation,
 * media file playback, and permission requests.
 */
public class RecordsFragment extends Fragment {

    private static final Logger logger = LoggerFactory.getLogger(RecordsFragment.class);
    private FolderAdapter folderAdapter;
    private final List<File> downloadedFiles = new ArrayList<>();
    private static MediaPlayer mediaPlayer;
    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final int WRITE_PERMISSION_CODE = 200;
    public final File recordDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC + File.separator + "ZeDio");

    private final Helper helper = new Helper();
    private String currentlyPlayingFilePath = null;  // Variable to store the path of the currently playing file

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_records, container, false);

        // Initialize RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.records_layout);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        folderAdapter = new FolderAdapter(downloadedFiles, getContext(), new FolderAdapter.OnItemClickListener() {
            @Override
            public void onPlayItem(File file) {
                onFileClick(file);  // Logic to play the file
            }

            @Override
            public void onStopPlaying(File file) {
                stopMediaPlayerIfPlaying();  // Logic to stop playing the file
            }

            @Override
            public void onFolderClick(File folder) {
                if (checkStoragePermission()) {
                    loadFoldersAndSongs(folder);  // Navigate into the folder
                } else {
                    requestStoragePermissions();  // Request permission if needed
                }
            }
        });
        recyclerView.setAdapter(folderAdapter);

        // Check for storage permission and attempt to load the music folder
        checkPermissionAndLoadFolders();

        return view;
    }

    private void checkPermissionAndLoadFolders() {
        if (checkStoragePermission()) {
            loadFoldersAndSongs(null);  // Load the base music directory
        } else {
            requestStoragePermissions();  // Request storage permission
        }
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermissions() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) || shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            helper.Toast(getContext(), getLayoutInflater(), "Storage permissions are required to load, edit, and delete songs", false, false);
        }

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    getActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_PERMISSION_CODE
            );
        }
    }

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
                helper.Toast(getContext(), getLayoutInflater(), "No recordings found", false, false);
            }

        } else {
            helper.Toast(getContext(), getLayoutInflater(), "Storage permission is required to load songs", false, false);
        }
    }


    /**
     * Handles media file click and plays/stops the selected file.
     * Ensures that only one media file is playing at a time and stops the radio if it is running.
     *
     * @param file The media file to play or stop.
     */
    private void onFileClick(File file) {
        // Stop the radio if it's playing
        if (RadioHomeFragment.player != null) {
            RadioHomeFragment.player.stopMedia();
        }

        // If the clicked file is already playing, stop it
        if (mediaPlayer != null && mediaPlayer.isPlaying() && file.getPath().equals(currentlyPlayingFilePath)) {
            stopMediaPlayerIfPlaying();
            currentlyPlayingFilePath = null;  // Reset the currently playing file path
            helper.Toast(getContext(), getLayoutInflater(), "Stopped: " + file.getName(), false, false);
            logger.info("Stopped playing file: {}", file.getName());
            return;
        }

        // Stop any currently playing file before starting a new one
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            stopMediaPlayerIfPlaying();
        }

        // Otherwise, play the selected file
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(file.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            currentlyPlayingFilePath = file.getPath();  // Store the currently playing file path
            helper.Toast(getContext(), getLayoutInflater(), "Playing: " + file.getName(), true, false);
            logger.info("Playing file: {}", file.getName());
        } catch (Exception e) {
            logger.error("Error playing file: {}", file.getName(), e);
            helper.Toast(getContext(), getLayoutInflater(), "Error playing file: " + file.getName(), false, false);
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
    // Reload all files after an operation to ensure the list is up to date
    @SuppressLint("NotifyDataSetChanged")
    private void reloadFiles() {
        loadFoldersAndSongs(null); // Reload the base directory
        folderAdapter.notifyDataSetChanged(); // Refresh UI
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                helper.Toast(getContext(), getLayoutInflater(), "Permissions granted", true, false);
                loadFoldersAndSongs(null);  // Load the base music directory
            } else {
                helper.Toast(getContext(), getLayoutInflater(), "Permissions denied", false, false);
                logger.warn("Storage permissions denied by user.");
            }
        }
    }
}
