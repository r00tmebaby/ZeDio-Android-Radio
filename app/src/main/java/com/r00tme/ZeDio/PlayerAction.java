package com.r00tme.ZeDio;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaRecorder;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.audio.AudioAttributes;

import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DataSource;

import okhttp3.Call;
import okhttp3.OkHttpClient;

import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PlayerAction {

    private static final String TAG = "PlayerAction";
    private ExoPlayer exoPlayer;
    private final Context context;
    private final Radio currentRadio;
    private final WifiManager.WifiLock wifiLock;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private PowerManager.WakeLock wakeLock;

    public PlayerAction(Context context, Radio radio) {
        this.context = context;
        this.currentRadio = radio;

        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "ZedioLock");

        initPlayer();
    }

    private void initPlayer() {
        // Custom LoadControl for buffering configurations
        LoadControl loadControl = new DefaultLoadControl.Builder()
                .setAllocator(new DefaultAllocator(true, DefaultLoadControl.DEFAULT_AUDIO_BUFFER_SIZE))
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

        // Acquire CPU wake lock to prevent CPU from sleeping during playback
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Zedio:WakeLock");
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);

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

    // Get formatted date for file naming
    private String getDate() {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
        Date date = new Date();
        return formatter.format(date);
    }

    // Record streamed media into a file
    public void recordMedia() throws IOException {
        if (!isRecording) {
            mediaRecorder = new MediaRecorder();
            File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            if (!musicDir.exists()) {
                musicDir.mkdirs();  // Create directory if it doesn't exist
            }
            String filePath = musicDir + File.separator + getDate() + ".aac";

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);  // Adjust source if needed
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(filePath);

            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;

            Log.d(TAG, "Recording started at: " + filePath);
            Toast.makeText(context, "Recording started: " + filePath, Toast.LENGTH_LONG).show();
        }
    }

    // Stop recording
    public void stopRecording() {
        if (isRecording && mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;

            Log.d(TAG, "Recording stopped");
        }
    }

    // Stop media playback and release resources
    public void stopMedia() {
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.release();
        }
        stopRecording();
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
        }
    }

    // Get unsafe OkHttpClient that trusts all certificates
    private OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

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
