package com.r00tme.ZeDio.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.r00tme.ZeDio.fragments.EqualizerFragment;
import com.r00tme.ZeDio.fragments.RadioManagementFragment;
import com.r00tme.ZeDio.fragments.AppSettingsFragment;

public class SettingsPagerAdapter extends FragmentStateAdapter {

    public SettingsPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new EqualizerFragment();
            case 1:
                return new RadioManagementFragment();
            case 2:
                return new AppSettingsFragment();
            default:
                return new EqualizerFragment();  // Default to EqualizerFragment
        }
    }

    @Override
    public int getItemCount() {
        return 3;  // Three tabs: Equalizer, Radio Management, App Settings
    }
}
