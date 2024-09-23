package com.r00tme.ZeDio;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.r00tme.ZeDio.fragments.RadioHomeFragment;
import com.r00tme.ZeDio.fragments.RecordsFragment;
import com.r00tme.ZeDio.fragments.SettingsFragment;

import android.animation.ObjectAnimator;
import android.view.animation.BounceInterpolator;

public class MainActivity extends AppCompatActivity {
    private static final String BEATFIND_PACKAGE_NAME = "com.beat.light";
    private static final String BEATFIND_PLAY_STORE_LINK = "https://play.google.com/store/apps/details?id=com.beat.light&hl=en_GB";

    private ImageButton radioHomeButton;
    private ImageButton recordsButton;
    private ImageButton settingsButton;
    private ImageButton googleAssistant;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);  // Top menu remains static here

        // Initialize buttons
        radioHomeButton = findViewById(R.id.radio_home);
        recordsButton = findViewById(R.id.records);
        settingsButton = findViewById(R.id.settings);
        googleAssistant = findViewById(R.id.g_assistant);
        // Set default alpha (90% transparency)
        radioHomeButton.setAlpha(1f);
        recordsButton.setAlpha(0.4f);
        settingsButton.setAlpha(0.4f);
        googleAssistant.setAlpha(0.4f);

        // Set program exit activity
        ImageButton exitButton = findViewById(R.id.exit_button);
        exitButton.setOnClickListener(v -> {
            finish();
            System.exit(0);
        });

        // Handle radio_home button click
        radioHomeButton.setOnClickListener(v -> {
            loadFragment(new RadioHomeFragment());
            updateButtonImagesAndAlpha(R.id.radio_home);  // Update button states and alpha, apply animation
        });

        // Handle records button click
        recordsButton.setOnClickListener(v -> {
            loadFragment(new RecordsFragment());
            updateButtonImagesAndAlpha(R.id.records);  // Update button states and alpha, apply animation
        });

        // Handle settings button click
        settingsButton.setOnClickListener(v -> {
            loadFragment(new SettingsFragment());
            updateButtonImagesAndAlpha(R.id.settings);  // Update button states and alpha, apply animation
        });

        // Handle BeatFind button click
        findViewById(R.id.g_assistant).setOnClickListener(v -> {
            updateButtonImagesAndAlpha(R.id.g_assistant);
            if (isAppInstalled(BEATFIND_PACKAGE_NAME)) {

                launchBeatFind();
            } else {
                // Show prompt to install the app
                showInstallBeatFindPrompt();
            }
        });

        // Load the default fragment (main screen) when the app starts
        if (savedInstanceState == null) {
            loadFragment(new RadioHomeFragment());  // Show radios by default
            updateButtonImagesAndAlpha(R.id.radio_home);  // Set default selected state for radio_home
        }
    }
    // Method to check if BeatFind is installed
    private boolean isAppInstalled(String packageName) {
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    // Method to launch BeatFind if it's installed
    private void launchBeatFind() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(BEATFIND_PACKAGE_NAME);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Could not open BeatFind", Toast.LENGTH_SHORT).show();
        }
    }
    // Method to show prompt to install BeatFind from Google Play
    private void showInstallBeatFindPrompt() {
        Toast.makeText(this, "BeatFind is not installed. Redirecting to Play Store...", Toast.LENGTH_LONG).show();
        Intent playStoreIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(BEATFIND_PLAY_STORE_LINK));
        playStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(playStoreIntent);
    }
    private void loadFragment(Fragment fragment) {
        // Replace the fragment in the fragment_container (below the top menu)
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);  // Optional: allows navigating back
        transaction.commit();
    }

    private void updateButtonImagesAndAlpha(int selectedButtonId) {
        // Reset the scale of all buttons to default
        resetButtonScale();

        // Update the radio_home button image, alpha, and apply animation
        if (selectedButtonId == R.id.radio_home) {
            radioHomeButton.setAlpha(1.0f);  // Full opacity for selected button
            applyBounceAnimation(radioHomeButton);  // Apply bounce animation for selected button
        } else {
            radioHomeButton.setAlpha(0.4f);  // Set alpha to 90% for non-selected
        }

        // Update the records button image, alpha, and apply animation
        if (selectedButtonId == R.id.records) {
            recordsButton.setAlpha(1.0f);  // Full opacity for selected button
            applyBounceAnimation(recordsButton);  // Apply bounce animation for selected button
        } else {
            recordsButton.setAlpha(0.4f);  // Set alpha to 90% for non-selected
        }

        // Update the settings button image, alpha, and apply animation
        if (selectedButtonId == R.id.settings) {
            settingsButton.setAlpha(1.0f);  // Full opacity for selected button
            applyBounceAnimation(settingsButton);  // Apply bounce animation for selected button
        } else {
            settingsButton.setAlpha(0.4f);  // Set alpha to 90% for non-selected
        }
        // Update the radio_home button image, alpha, and apply animation
        if (selectedButtonId == R.id.g_assistant) {
            googleAssistant.setAlpha(1.0f);  // Full opacity for selected button
            applyBounceAnimation(googleAssistant);  // Apply bounce animation for selected button
        } else {
            googleAssistant.setAlpha(0.4f);  // Set alpha to 90% for non-selected
        }
    }

    private void applyBounceAnimation(View button) {
        // Create an ObjectAnimator to scale the button in both X and Y directions
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1.0f, 1.2f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1.0f, 1.2f, 1.0f);

        // Set the duration of the animation
        scaleX.setDuration(300);  // 300 ms for the bounce effect
        scaleY.setDuration(300);

        // Set a BounceInterpolator for a nice bounce effect
        scaleX.setInterpolator(new BounceInterpolator());
        scaleY.setInterpolator(new BounceInterpolator());

        // Start the animations
        scaleX.start();
        scaleY.start();
    }

    private void resetButtonScale() {
        // Reset all buttons to default scale (1.0) before applying new animation
        radioHomeButton.setScaleX(1.0f);
        radioHomeButton.setScaleY(1.0f);
        recordsButton.setScaleX(1.0f);
        recordsButton.setScaleY(1.0f);
        settingsButton.setScaleX(1.0f);
        settingsButton.setScaleY(1.0f);
        googleAssistant.setScaleX(1.0f);
        googleAssistant.setScaleY(1.0f);
    }
}
