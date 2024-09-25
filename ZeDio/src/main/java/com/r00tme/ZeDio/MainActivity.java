package com.r00tme.ZeDio;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.r00tme.ZeDio.fragments.RadioHomeFragment;
import com.r00tme.ZeDio.fragments.RecordsFragment;
import com.r00tme.ZeDio.fragments.SettingsFragment;
import com.r00tme.ZeDio.classes.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main activity for ZeDio app that manages the top menu buttons and loads different fragments.
 * Handles interactions with radio, records, settings, and Google Assistant (BeatFind) functionality.
 */
public class MainActivity extends AppCompatActivity {

    private static final Logger logger = LoggerFactory.getLogger(MainActivity.class);

    private static final String BEAT_FIND_PACKAGE_NAME = "com.beat.light";
    private static final String BEAT_FIND_PLAY_STORE_LINK = "https://play.google.com/store/apps/details?id=com.beat.light&hl=en_GB";

    private ImageButton radioHomeButton;
    private ImageButton recordsButton;
    private ImageButton settingsButton;
    private ImageButton googleAssistant;
    private final Helper helper = new Helper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize buttons
        radioHomeButton = findViewById(R.id.radio_home);
        recordsButton = findViewById(R.id.records);
        settingsButton = findViewById(R.id.settings);
        googleAssistant = findViewById(R.id.g_assistant);

        // Set default alpha (90% transparency) for non-selected buttons
        radioHomeButton.setAlpha(1f);
        recordsButton.setAlpha(0.4f);
        settingsButton.setAlpha(0.4f);
        googleAssistant.setAlpha(0.4f);

        // Handle app exit button click
        ImageButton exitButton = findViewById(R.id.exit_button);
        exitButton.setOnClickListener(v -> {
            finish();
            System.exit(0);
        });

        // Load the default fragment (Radio Home) on startup
        if (savedInstanceState == null) {
            loadFragment(new RadioHomeFragment());
            updateButtonImagesAndAlpha(R.id.radio_home);  // Default selected state for radio_home
        }

        // Handle button interactions
        setupButtonListeners();
    }

    /**
     * Sets up listeners for all buttons on the main screen.
     * Each button will load the respective fragment or launch the BeatFind app.
     */
    private void setupButtonListeners() {
        // Radio Home button click
        radioHomeButton.setOnClickListener(v -> {
            loadFragment(new RadioHomeFragment());
            updateButtonImagesAndAlpha(R.id.radio_home);
        });

        // Records button click
        recordsButton.setOnClickListener(v -> {
            loadFragment(new RecordsFragment());
            updateButtonImagesAndAlpha(R.id.records);
        });

        //// Settings button click
        //settingsButton.setOnClickListener(v -> {
        //    loadFragment(new SettingsFragment());
        //    updateButtonImagesAndAlpha(R.id.settings);
        //});

        // BeatFind button click
        googleAssistant.setOnClickListener(v -> {
            updateButtonImagesAndAlpha(R.id.g_assistant);
            if (isAppInstalled(BEAT_FIND_PACKAGE_NAME)) {
                launchBeatFind();
            } else {
                showInstallBeatFindPrompt();
            }
        });
    }

    /**
     * Checks if BeatFind is installed on the device.
     *
     * @param packageName The package name of the app to check.
     * @return true if the app is installed, false otherwise.
     */
    private boolean isAppInstalled(String packageName) {
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            logger.warn("BeatFind app not installed");
            return false;
        }
    }

    /**
     * Launches the BeatFind app if installed.
     */
    private void launchBeatFind() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(BEAT_FIND_PACKAGE_NAME);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            logger.info("Launching BeatFind app");
        } else {
            helper.Toast(this, getLayoutInflater(), "Could not open BeatFind", false, false);
            logger.error("Failed to launch BeatFind app");
        }
    }

    /**
     * Shows a prompt to install BeatFind app from the Play Store.
     */
    private void showInstallBeatFindPrompt() {
        helper.Toast(this, getLayoutInflater(), "BeatFind is not installed. Redirecting to Play Store...", false, false);
        Intent playStoreIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(BEAT_FIND_PLAY_STORE_LINK));
        playStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(playStoreIntent);
        logger.info("Redirecting user to install BeatFind from Play Store");
    }

    /**
     * Loads the specified fragment into the fragment container.
     *
     * @param fragment The fragment to be loaded.
     */
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);  // Optional: allows navigating back
        transaction.commit();
        logger.info("Loaded fragment: " + fragment.getClass().getSimpleName());
    }

    /**
     * Updates the button images and alpha based on the selected button.
     * Also applies a bounce animation to the selected button.
     *
     * @param selectedButtonId The ID of the button that was selected.
     */
    private void updateButtonImagesAndAlpha(int selectedButtonId) {
        resetButtonScale();

        // Update the button images and alpha based on selection
        if (selectedButtonId == R.id.radio_home) {
            radioHomeButton.setAlpha(1.0f);
            applyBounceAnimation(radioHomeButton);
        } else {
            radioHomeButton.setAlpha(0.4f);
        }

        if (selectedButtonId == R.id.records) {
            recordsButton.setAlpha(1.0f);
            applyBounceAnimation(recordsButton);
        } else {
            recordsButton.setAlpha(0.4f);
        }

        if (selectedButtonId == R.id.settings) {
            settingsButton.setAlpha(1.0f);
            applyBounceAnimation(settingsButton);
        } else {
            settingsButton.setAlpha(0.4f);
        }

        if (selectedButtonId == R.id.g_assistant) {
            googleAssistant.setAlpha(1.0f);
            applyBounceAnimation(googleAssistant);
        } else {
            googleAssistant.setAlpha(0.4f);
        }
    }

    /**
     * Applies a bounce animation to the selected button.
     *
     * @param button The button to animate.
     */
    private void applyBounceAnimation(View button) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1.0f, 1.2f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1.0f, 1.2f, 1.0f);

        scaleX.setDuration(300);
        scaleY.setDuration(300);

        scaleX.setInterpolator(new BounceInterpolator());
        scaleY.setInterpolator(new BounceInterpolator());

        scaleX.start();
        scaleY.start();
    }

    /**
     * Resets the scale of all buttons to the default size.
     */
    private void resetButtonScale() {
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
