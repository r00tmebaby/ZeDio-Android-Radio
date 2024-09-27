package com.r00tme.ZeDio.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.exoplayer2.*;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.r00tme.ZeDio.fragments.RecordsFragment;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

/**
 * Class that handles media playback and recording actions for a radio station.
 * Manages ExoPlayer, wake lock, Wi-Fi lock, and recording streamed audio.
 */
public class PlayerAction {

    private static final String TAG = "PlayerAction";
    private static final long WAKELOCK_REFRESH_INTERVAL = 9 * 60 * 1000L; // 9 minutes (to refresh before the 10-min timeout)
    private final Context context;
    private final Radio currentRadio;
    private final WifiManager.WifiLock wifiLock;
    private final Handler wakeLockHandler = new Handler(Looper.getMainLooper());
    private static ExoPlayer exoPlayer;
    private FileOutputStream outputStream;
    private boolean isRecording = false;
    private boolean isStoppingRecording = false;
    private PowerManager.WakeLock wakeLock;
    private boolean isWakeLockRefreshScheduled = false;
    private final String currentRadioName; // Store the radio name
    private static URL currentRadioURL; // Store the radio URL

    /**
     * Constructor for PlayerAction.
     * Initializes Wi-Fi lock, ExoPlayer, and wake lock.
     *
     * @param context The application context.
     * @param radio The current radio station to play.
     */
    public PlayerAction(Context context, Radio radio) throws MalformedURLException {
        this.context = context;
        this.currentRadio = radio;
        this.currentRadioName = radio.getRadioName();
        currentRadioURL = radio.getRadioURLobj();
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "ZedioLock");

        initPlayer();
        initWakeLock();  // Initialize wake lock here
    }

    public static URL getCurrentRadioURL() {
        return currentRadioURL;
    }

    public static void setCurrentRadioURL(URL currentRadioURL) {
        PlayerAction.currentRadioURL = currentRadioURL;
    }

    /**
     * Initializes and acquires a wake lock to prevent the device from sleeping during playback.
     */
    private void initWakeLock() {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Zedio:WakeLock");
    }

    /**
     * Acquires the wake lock and schedules refresh to prevent timeout.
     */
    private void acquireWakeLock() {
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(10 * 60 * 1000L); // Acquire for 10 minutes
            scheduleWakeLockRefresh();
        }
    }

    public static ExoPlayer getPlayer(){
        return exoPlayer;
    }

    /**
     * Releases the wake lock and cancels any scheduled refreshes.
     */
    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        cancelWakeLockRefresh();
    }

    /**
     * Cancels the scheduled wake lock refresh.
     */
    private void cancelWakeLockRefresh() {
        if (isWakeLockRefreshScheduled) {
            wakeLockHandler.removeCallbacksAndMessages(null);
            isWakeLockRefreshScheduled = false;
        }
    }

    /**
     * Schedules a refresh of the wake lock to prevent timeout.
     */
    private void scheduleWakeLockRefresh() {
        if (!isWakeLockRefreshScheduled) {
            wakeLockHandler.postDelayed(this::refreshWakeLock, WAKELOCK_REFRESH_INTERVAL);
            isWakeLockRefreshScheduled = true;
        }
    }
    // Method to check if the player is currently playing media
    public boolean isPlaying() {
        return exoPlayer != null && exoPlayer.getPlayWhenReady() && exoPlayer.getPlaybackState() == Player.STATE_READY;
    }
    /**
     * Refreshes the wake lock by releasing and re-acquiring it.
     */
    private void refreshWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock.acquire(10 * 60 * 1000L); // Re-acquire
        }
        if (exoPlayer != null && exoPlayer.getPlayWhenReady()) {
            scheduleWakeLockRefresh();
        } else {
            isWakeLockRefreshScheduled = false;
        }
    }

    /**
     * Initializes ExoPlayer with a custom load control and prepares it for media playback.
     */
    private void initPlayer() {
        LoadControl loadControl = new DefaultLoadControl.Builder()
                .setAllocator(new DefaultAllocator(true, 4096))
                .setBufferDurationsMs(
                        4000, // Min buffer before start (4 seconds)
                        9000, // Max buffer size (9 seconds)
                        2000, // Buffer when playback stalls (2 seconds)
                        4000  // Min buffer size while re-buffering (4 seconds)
                )
                .build();

        exoPlayer = new ExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .build();

        exoPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                true
        );

        wifiLock.acquire();

        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) {
                    Log.d(TAG, "Buffering...");
                } else if (playbackState == Player.STATE_READY && playWhenReady) {
                    Log.d(TAG, "Playing");
                } else if (playbackState == Player.STATE_ENDED) {
                    Log.d(TAG, "Playback Ended");
                } else if (playbackState == Player.STATE_IDLE) {
                    Log.d(TAG, "Player Idle");
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.e(TAG, "Error Occurred: " + error.getMessage());
            }
        });
    }
    /**
     * Pauses the media playback instead of fully stopping and releasing the player.
     */
    public void pauseMedia() {
        if (exoPlayer != null) {
            exoPlayer.setPlayWhenReady(false);  // Pause playback
        }
        releaseWakeLock();
    }

    /**
     * Resumes media playback if the player is paused.
     */
    public void resumeMedia() {
        if (exoPlayer != null) {
            exoPlayer.setPlayWhenReady(true);  // Resume playback
            acquireWakeLock();  // Ensure wake lock is acquired again
        }
    }
    /**
     * Plays the media stream from the current radio station using ExoPlayer.
     * Utilizes OkHttpDataSource for Icecast streams.
     */
    public void playMedia() {
        if (exoPlayer != null) {
            OkHttpClient okHttpClient = getUnsafeOkHttpClient();

            DataSource.Factory dataSourceFactory = new OkHttpDataSourceFactory(
                    (Call.Factory) okHttpClient,
                    "ExoPlayer-OkHttp"
            );

            MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(currentRadio.getRadioUrl()));

            exoPlayer.setMediaItem(mediaSource.getMediaItem());
            exoPlayer.prepare();
            exoPlayer.setPlayWhenReady(true);

            acquireWakeLock();
        }
    }

    /**
     * Stops the media playback and releases the player and Wi-Fi lock.
     */
    public void stopMedia() {
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
        }
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.release();
        }
        releaseWakeLock();
    }

    /**
     * Records the media stream from the current radio station to an MP3 file.
     * @throws IOException If there is an error during recording.
     */
    public synchronized void recordMedia() throws IOException {
        if (!isRecording) {
            RecordsFragment record = new RecordsFragment();
            File musicDir = new File(record.recordDirectory + File.separator + currentRadio.getRadioName());

            if (!musicDir.exists()) {
                musicDir.mkdirs();
            }
            String filePath = musicDir + File.separator + getDate() + ".mp3";

            outputStream = new FileOutputStream(filePath);

            OkHttpClient okHttpClient = new OkHttpClient();
            InputStream inputStream = Objects.requireNonNull(
                    okHttpClient.newCall(
                            new Request.Builder()
                                    .url(currentRadio.getRadioUrl())
                                    .build()
                            )
                            .execute()
                            .body()
            ).byteStream();

            new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1 && !isStoppingRecording) {
                        synchronized (PlayerAction.this) {
                            if (outputStream != null) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                        }
                    }
                    synchronized (PlayerAction.this) {
                        if (outputStream != null) {
                            outputStream.flush();
                            outputStream.close();
                            outputStream = null;
                        }
                    }
                    Log.d(TAG, "Recording saved to " + filePath);
                } catch (IOException e) {
                    Log.e(TAG, "Error writing stream to file", e);
                }
            }).start();

            isRecording = true;
            isStoppingRecording = false;
        }
    }

    /**
     * Stops recording the media stream and closes the output file.
     */
    public synchronized void stopRecording() {
        if (isRecording) {
            try {
                isStoppingRecording = true;
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                    outputStream = null;
                }
                isRecording = false;
                Log.d(TAG, "Recording stopped");
            } catch (IOException e) {
                Log.e(TAG, "Error stopping recording", e);
            }
        }
    }

    /**
     * Creates an OkHttpClient that trusts all certificates.
     * This is unsafe and should not be used in production environments.
     *
     * @return An OkHttpClient that trusts all SSL certificates.
     */
    private OkHttpClient getUnsafeOkHttpClient() {
        try {
            @SuppressLint("CustomX509TrustManager") final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @SuppressLint("TrustAllX509TrustManager")
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @SuppressLint("TrustAllX509TrustManager")
                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks whether recording is currently active.
     *
     * @return True if recording is active, false otherwise.
     */
    public boolean isRecording() {
        return isRecording;
    }

    /**
     * Generates a timestamped filename for recording media files.
     *
     * @return A formatted string representing the current date and time.
     */
    private String getDate() {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
        Date date = new Date();
        return formatter.format(date);
    }

    public String getCurrentRadioName() {
        return currentRadioName;
    }
}
