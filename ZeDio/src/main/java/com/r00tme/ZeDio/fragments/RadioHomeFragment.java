package com.r00tme.ZeDio.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.r00tme.ZeDio.R;
import com.r00tme.ZeDio.RecyclerItemClickListener;
import com.r00tme.ZeDio.actions.PlayerAction;
import com.r00tme.ZeDio.adapters.RadioAdapter;
import com.r00tme.ZeDio.classes.Helper;
import com.r00tme.ZeDio.classes.Radio;
import com.r00tme.ZeDio.parsers.ParsingHeaderData;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InvalidObjectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fragment to manage radio home screen including radio list, media player actions,
 * and recording functionality. Allows filtering, searching, and managing radio playback and recording.
 */
public class RadioHomeFragment extends Fragment implements AdapterView.OnItemSelectedListener {
    // Logger initialization
    private static final Logger logger = LoggerFactory.getLogger(RadioHomeFragment.class);
    private static final int RECORD_AUDIO_REQUEST_CODE = 10;
    private RadioAdapter radioAdapter;
    private String filteredBy;
    private List<Radio> radioList = new ArrayList<>();
    Helper helper = new Helper();

    @SuppressLint("StaticFieldLeak")
    static PlayerAction player; // Static to maintain state across fragments
    private static URL selectedRadioURL;
    private static String selectedRadioName;
    public TextView radioPlayingName;
    public TextView radioInfoText;
    public LinearLayout playingRadioLayout;
    private ImageButton startRecordButton;
    private ImageButton stopRadioButton;

    /**
     * Updates the UI to reflect that the recording is in progress.
     */
    public void setRecordingDesign() {
        startRecordButton.setImageResource(R.drawable.stop_grey);
        stopRadioButton.setImageResource(R.drawable.pause_grey);
        radioPlayingName.setTextColor(getResources().getColor(R.color.white));
        radioPlayingName.setBackgroundColor(getResources().getColor(R.color.red_transparent));
    }

    /**
     * Resets the recording and play buttons to their default state.
     */
    public void resetButtonsDesign() {
        startRecordButton.setImageResource(R.drawable.record_grey);
        stopRadioButton.setImageResource(R.drawable.play_grey);
        radioPlayingName.setTextColor(getResources().getColor(R.color.white));
        radioPlayingName.setBackgroundColor(getResources().getColor(R.color.material_on_background_disabled));
    }

    /**
     * Updates the UI to show that the radio is playing.
     */
    public void setPlayRadioButtons() {
        radioPlayingName.setText(selectedRadioName);
        stopRadioButton.setImageResource(R.drawable.pause_grey);  // Change button to pause icon
        radioPlayingName.setTextColor(getResources().getColor(R.color.white));  // Update UI to indicate playing state
        startRecordButton.setImageResource(R.drawable.record_grey);
        updateRadioMetaText();  // Update metadata display
    }

    /**
     * Updates the UI to reflect that the radio is paused.
     */
    @SuppressLint("SetTextI18n")
    public void setPausePlayRadioButtons() {
        stopRadioButton.setImageResource(R.drawable.play_grey);  // Change button to play icon
        radioPlayingName.setTextColor(getResources().getColor(R.color.material_on_background_disabled));  // Update UI to indicate paused state
        radioInfoText.setText("Radio paused");
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_radio_home, container, false);

        // Initialize Views
        RecyclerView recyclerView = view.findViewById(R.id.radio_view_layout);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        Spinner spinner = view.findViewById(R.id.filter_by);
        ArrayAdapter<CharSequence> filterAdapter = ArrayAdapter.createFromResource(
                getContext(), R.array.filter_bys, android.R.layout.simple_spinner_item
        );
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(filterAdapter);
        spinner.setOnItemSelectedListener(this);

        // Fetch and set the radio list
        try {
            radioAdapter = new RadioAdapter(fetchAllRadios(), getContext());
        } catch (IOException e) {
            logger.error("Error fetching radio list", e);
        }
        recyclerView.setAdapter(radioAdapter);

        // Set up the search view
        SearchView searchView = view.findViewById(R.id.search_radio);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (radioAdapter != null)
                    radioAdapter.getFilter().filter(filteredBy + "#" + newText);
                return false;
            }
        });

        // Initialize media player and recording button
        startRecordButton = view.findViewById(R.id.start_recording);
        stopRadioButton = view.findViewById(R.id.stop_playing);
        playingRadioLayout = view.findViewById(R.id.playing_radio_layout);
        radioPlayingName = view.findViewById(R.id.playing_name);
        radioInfoText = view.findViewById(R.id.radio_info_data);

        if (player != null && player.isPlaying()) {
            //helper.Toast(getContext(), getLayoutInflater(), "Radio is playing", true, false);
            setPlayRadioButtons();
        } else {
            setPausePlayRadioButtons();
        }

        // Set animations for text scrolling
        radioInfoText.setAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.left_to_right));

        updateRadioMetaText();  // Update UI with current track info

        // Set stop radio and recording logic (play/pause toggle)
        stopRadioButton.setOnClickListener(v -> {
            if (player != null) {
                if (player.isPlaying()) {
                    // If the radio is playing, pause it
                    player.pauseMedia();
                    setPausePlayRadioButtons();

                    // Stop recording if it's ongoing
                    if (player.isRecording()) {
                        player.stopRecording();
                        helper.Toast(getContext(), getLayoutInflater(), "Recording stopped due to radio pause!", false, false);
                        resetButtonsDesign();  // Reset the button icons
                    }
                } else {
                    // If the radio is paused, resume it
                    player.resumeMedia();
                    setPlayRadioButtons();
                }
            } else {
                helper.Toast(getContext(), getLayoutInflater(), "No radio selected! Hold over radio card to play radio", false, false);
            }
        });

        // Set start/stop recording logic
        startRecordButton.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(Objects.requireNonNull(getContext()),
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(Objects.requireNonNull(getActivity()), new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_REQUEST_CODE);
            } else {
                if (player != null && player.isPlaying()) {
                    // Only allow recording if the radio is playing
                    if (!player.isRecording()) {
                        try {
                            player.recordMedia();
                            setRecordingDesign();
                            helper.Toast(getContext(), getLayoutInflater(), "Recording Started", true, false);
                        } catch (IOException ioException) {
                            logger.error("Error recording", ioException);
                        }
                    } else {
                        // Stop recording if already ongoing
                        player.stopRecording();
                        resetButtonsDesign();
                        helper.Toast(getContext(), getLayoutInflater(), "Recording Stopped", true, false);
                    }
                } else {
                    // Show a message if the radio isn't playing
                    helper.Toast(getContext(), getLayoutInflater(), "Recording can only start when the radio is playing!", false, false);
                }
            }
        });

        // Handle long click on recycler view items to play selected radio
        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getContext(), recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                // Handle item click if needed
            }

            @SuppressLint("CheckResult")
            @Override
            public void onLongItemClick(View view, int position) throws MalformedURLException {
                if (!radioList.isEmpty()) {
                    Radio selectedRadio = radioList.get(position);
                    // Stop the media player if it's playing
                    RecordsFragment.stopMediaPlayerIfPlaying();
                    player = new PlayerAction(Objects.requireNonNull(getActivity()), selectedRadio);
                    new RequestOptions().priority(Priority.HIGH).fitCenter().diskCacheStrategy(DiskCacheStrategy.ALL);

                    selectedRadioName = selectedRadio.getRadioName();
                    selectedRadioURL = selectedRadio.getRadioURLobj();

                    player.stopRecording();
                    player.playMedia();
                    setPlayRadioButtons();
                }
            }
        }));

        return view;
    }

    /**
     * Fetches the list of radios from a remote URL.
     *
     * @return A list of Radio objects.
     * @throws IOException If an input or output error occurs.
     */
    private ArrayList<Radio> fetchAllRadios() throws IOException {
        ArrayList<Radio> radioList = new ArrayList<>();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitNetwork().build());

        try {
            URL radioListUrl = new URL("https://zdpainters.com/zedio/radio-android.txt");
            BufferedReader file = new BufferedReader(new InputStreamReader(radioListUrl.openStream()));
            String str;

            while ((str = file.readLine()) != null) {
                String[] radioData = str.split(",");
                if (radioData.length != 5) {
                    continue;
                }
                radioList.add(new Radio(radioData[0], radioData[1], radioData[2], radioData[3], radioData[4]));
            }
            file.close();
        } catch (MalformedURLException e) {
            throw new InvalidObjectException("Cannot access the remote link containing all radios");
        }
        this.radioList = radioList;
        return radioList;
    }

    /**
     * Updates the metadata text for the currently playing radio.
     */
    @SuppressLint("SetTextI18n")
    private void updateRadioMetaText() {
        if (player != null) {
            new Thread(() -> {
                requireActivity().runOnUiThread(() -> {
                    ParsingHeaderData streaming = new ParsingHeaderData();
                    ParsingHeaderData.TrackData trackData = streaming.getTrackDetails(selectedRadioURL);

                    String displayInfo = "- No track information -";

                    if (!trackData.artist.trim().isEmpty()) {
                        displayInfo = trackData.artist + " - " + trackData.title;
                    }
                    radioInfoText.setText(displayInfo);
                });
            }).start();
        }
    }

    // Handle the dropdown spinner selection for filtering
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        filteredBy = parent.getItemAtPosition(position).toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // No action needed
    }
}
