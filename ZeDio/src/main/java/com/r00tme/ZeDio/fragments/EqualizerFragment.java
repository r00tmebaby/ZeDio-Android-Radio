package com.r00tme.ZeDio.fragments;

import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Virtualizer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import androidx.fragment.app.Fragment;
import com.google.android.exoplayer2.ExoPlayer;
import com.r00tme.ZeDio.R;
import com.r00tme.ZeDio.utils.PlayerAction;
import com.r00tme.ZeDio.utils.Helper;
import org.jetbrains.annotations.NotNull;

public class EqualizerFragment extends Fragment {

    public ExoPlayer exoPlayer;
    private final Helper helper = new Helper();
    private Equalizer equalizer;
    private BassBoost bassBoost;
    private Virtualizer virtualizer;

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Retrieve the ExoPlayer instance from PlayerAction
        exoPlayer = PlayerAction.getPlayer();

        View view = inflater.inflate(R.layout.fragment_equalizer, container, false);

        if (exoPlayer != null) {
            helper.Toast(getContext(), getLayoutInflater(), "Player is currently playing", false, false);

            // Initialize Equalizer
            equalizer = new Equalizer(0, exoPlayer.getAudioSessionId());
            equalizer.setEnabled(true);

            // Initialize BassBoost
            bassBoost = new BassBoost(0, exoPlayer.getAudioSessionId());
            bassBoost.setEnabled(true);

            // Initialize Virtualizer
            virtualizer = new Virtualizer(0, exoPlayer.getAudioSessionId());
            virtualizer.setEnabled(true);

            // Setup equalizer SeekBars
            setupEqualizerBand(view, R.id.equalizer_band_1, 0);
            setupEqualizerBand(view, R.id.equalizer_band_2, 1);
            setupEqualizerBand(view, R.id.equalizer_band_3, 2);
            setupEqualizerBand(view, R.id.equalizer_band_4, 3);
            setupEqualizerBand(view, R.id.equalizer_band_5, 4);

            // Setup Bass Boost
            setupBassBoost(view, R.id.bass_boost);

            // Setup Virtualizer
            setupVirtualizer(view, R.id.virtualizer);

        } else {
            helper.Toast(getContext(), getLayoutInflater(), "Player is initialized but not playing", false, false);
        }

        return view;
    }

    private void setupEqualizerBand(View view, int seekBarId, final int band) {
        SeekBar seekBar = view.findViewById(seekBarId);
        seekBar.setMax(equalizer.getBandLevelRange()[1]);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            seekBar.setMin(equalizer.getBandLevelRange()[0]);
        }

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    equalizer.setBandLevel((short) band, (short) progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupBassBoost(View view, int seekBarId) {
        SeekBar bassSeekBar = view.findViewById(seekBarId);
        bassSeekBar.setMax(1000);  // Max boost level is 1000

        bassSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    bassBoost.setStrength((short) progress);  // Set bass boost level
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupVirtualizer(View view, int seekBarId) {
        SeekBar virtualizerSeekBar = view.findViewById(seekBarId);
        virtualizerSeekBar.setMax(1000);  // Max virtualizer level is 1000

        virtualizerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    virtualizer.setStrength((short) progress);  // Set virtualizer level
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (equalizer != null) {
            equalizer.release();
        }
        if (bassBoost != null) {
            bassBoost.release();
        }
        if (virtualizer != null) {
            virtualizer.release();
        }
    }
}
