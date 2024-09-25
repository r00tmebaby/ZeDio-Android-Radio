package com.r00tme.ZeDio.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import com.google.android.exoplayer2.ExoPlayer;
import com.r00tme.ZeDio.R;
import com.r00tme.ZeDio.actions.PlayerAction;
import com.r00tme.ZeDio.classes.Helper;
import org.jetbrains.annotations.NotNull;

public class AppSettingsFragment extends Fragment {

    public ExoPlayer exoPlayer;
    private Helper helper = new Helper();

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_app_settings, container, false);
    }


}
