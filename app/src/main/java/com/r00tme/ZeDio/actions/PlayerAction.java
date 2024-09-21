package com.r00tme.ZeDio.actions;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.google.android.exoplayer2.*;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.r00tme.ZeDio.classes.Radio;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class PlayerAction {

    private static final String TAG = "PlayerAction";
    private static final long WAKELOCK_REFRESH_INTERVAL = 9 * 60 * 1000L; // 9 minutes (to refresh before the 10-min timeout)
    private final Context context;
    private final Radio currentRadio;
    private final WifiManager.WifiLock wifiLock;
    private final Handler wakeLockHandler = new Handler(Looper.getMainLooper());
    private ExoPlayer exoPlayer;
    private FileOutputStream outputStream;
    private boolean isRecording = false;
    private boolean isStoppingRecording = false;
    private PowerManager.WakeLock wakeLock;
    private boolean isWakeLockRefreshScheduled = false;

    public PlayerAction(Context context, Radio radio) {
        this.context = context;
        this.currentRadio = radio;

        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "ZedioLock");

        initPlayer();
        initWakeLock();  // Initialize wake lock here
    }

    // Initialize the wake lock
    private void initWakeLock() {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Zedio:WakeLock");
    }

    // Acquire the wake lock and schedule refresh
    private void acquireWakeLock() {
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/); // Acquire with timeout
            scheduleWakeLockRefresh(); // Schedule refresh
        }
    }

    // Release the wake lock and cancel refresh
    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        cancelWakeLockRefresh(); // Cancel any scheduled refreshes
    }

    // Cancel the scheduled wake lock refresh
    private void cancelWakeLockRefresh() {
        if (isWakeLockRefreshScheduled) {
            wakeLockHandler.removeCallbacksAndMessages(null); // Cancel refresh callbacks
            isWakeLockRefreshScheduled = false;
        }
    }

    // Schedule wake lock refresh to keep it active
    private void scheduleWakeLockRefresh() {
        if (!isWakeLockRefreshScheduled) {
            wakeLockHandler.postDelayed(this::refreshWakeLock, WAKELOCK_REFRESH_INTERVAL);
            isWakeLockRefreshScheduled = true;
        }
    }

    // Refresh the wake lock
    private void refreshWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/); // Re-acquire to refresh
        }
        // Reschedule refresh if still playing
        if (exoPlayer != null && exoPlayer.getPlayWhenReady()) {
            scheduleWakeLockRefresh();
        } else {
            isWakeLockRefreshScheduled = false; // Stop refreshing if not playing
        }
    }

    // Initialize the player
    private void initPlayer() {
        // Custom LoadControl for buffering configurations
        LoadControl loadControl = new DefaultLoadControl.Builder()
                .setAllocator(new DefaultAllocator(true, 4096))
                .setBufferDurationsMs(
                        4000, // Min buffer before start (4 seconds)
                        9000, // Max buffer size (9 seconds)
                        2000, // Buffer when playback stalls (2 seconds)
                        4000  // Min buffer size while re-buffering (4 seconds)
                )
                .build();

        // Initialize ExoPlayer with custom LoadControl
        exoPlayer = new ExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .build();

        // Configure the audio attributes for media playback
        exoPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(com.google.android.exoplayer2.C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(com.google.android.exoplayer2.C.USAGE_MEDIA)
                        .build(),
                true
        );

        // Acquire Wi-Fi lock to prevent stream disruption
        wifiLock.acquire();

        // Add event listeners for ExoPlayer
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
            public void onPlayerError(@NonNull com.google.android.exoplayer2.PlaybackException error) {
                Log.e(TAG, "Error Occurred: " + error.getMessage());
            }
        });
    }

    // Play media using ExoPlayer with OkHttpDataSource for streams
    public void playMedia() {
        if (exoPlayer != null) {
            // Trust all certificates (Unsafe)
            OkHttpClient okHttpClient = getUnsafeOkHttpClient();

            // Create OkHttpDataSource for Icecast streams
            DataSource.Factory dataSourceFactory = new OkHttpDataSourceFactory(
                    (Call.Factory) okHttpClient,
                    "ExoPlayer-OkHttp"
            );

            // Create a MediaSource with OkHttpDataSource
            MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(currentRadio.getRadioUrl()));

            // Set media source and prepare the player
            exoPlayer.setMediaSource(mediaSource);
            exoPlayer.prepare();

            // Start playback when ready
            exoPlayer.setPlayWhenReady(true);

            // Acquire wake lock when playing
            acquireWakeLock();
        }
    }

    public void stopMedia() {
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
        }
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.release();
        }

        // Release wake lock when stopping
        releaseWakeLock();
    }

    // Get formatted date for file naming
    private String getDate() {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
        Date date = new Date();
        return formatter.format(date);
    }

    // Record streamed media into a file
    // Record streamed media into a file
    // Record streamed media into a file
    public synchronized void recordMedia() throws IOException {
        if (!isRecording) {
            File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            if (!musicDir.exists()) {
                musicDir.mkdirs();  // Create directory if it doesn't exist
            }
            String filePath = musicDir + File.separator + getDate() + ".mp3";

            // Open output stream to save audio data
            outputStream = new FileOutputStream(filePath);

            OkHttpClient okHttpClient = new OkHttpClient();
            InputStream inputStream = Objects.requireNonNull(okHttpClient.newCall(new Request.Builder()
                    .url(currentRadio.getRadioUrl())
                    .build()).execute().body()
            ).byteStream();

            // Write audio data to the file in a background thread
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
            isStoppingRecording = false;  // Reset stopping flag
            Toast.makeText(context, "Recording started", Toast.LENGTH_SHORT).show();
        }
    }

    // Stop recording with proper synchronization
    public synchronized void stopRecording() {
        if (isRecording) {
            try {
                isStoppingRecording = true;  // Set flag to stop recording in the background thread
                if (outputStream != null) {
                    outputStream.flush();  // Ensure any buffered data is written
                    outputStream.close();  // Close the file
                    outputStream = null;   // Set to null to avoid further operations on the file
                }
                isRecording = false;
                Log.d(TAG, "Recording stopped");
            } catch (IOException e) {
                Log.e(TAG, "Error stopping recording", e);
            }
        }
    }

    // Get unsafe OkHttpClient that trusts all certificates
    private OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            @SuppressLint("CustomX509TrustManager") final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @SuppressLint("TrustAllX509TrustManager")
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @SuppressLint("TrustAllX509TrustManager")
                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create a ssl socket factory with our all-trusting manager
            final javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier(new HostnameVerifier() {
                @SuppressLint("BadHostnameVerifier")
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Check if recording is active
    public boolean isRecording() {
        return isRecording;
    }
}
