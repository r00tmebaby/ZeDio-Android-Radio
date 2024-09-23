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

public class RadioHomeFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    private static final int RECORD_AUDIO_REQUEST_CODE = 10;
    private RadioAdapter radioAdapter;
    private String filteredBy;
    private List<Radio> radioList = new ArrayList<>();

    @SuppressLint("StaticFieldLeak")
    static PlayerAction player; // Static to maintain state across fragments
    private static URL selectedRadioURL;
    private static String selectedRadioName;
    public TextView radioPlayingName;
    public TextView radioInfoText;
    public LinearLayout playingRadioLayout;
    private ImageButton startRecordRadio;

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

        // Fetch and Set Radio List
        try {
            radioAdapter = new RadioAdapter(fetchAllRadios(), getContext());
        } catch (IOException e) {
            e.printStackTrace();
        }
        recyclerView.setAdapter(radioAdapter);

        // Set Up Search View
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

        // Initialize Media Player and Recording Button
        startRecordRadio = view.findViewById(R.id.start_recording);
        ImageButton stopRadio = view.findViewById(R.id.stop_playing);
        playingRadioLayout = view.findViewById(R.id.playing_radio_layout);
        radioPlayingName = view.findViewById(R.id.playing_name);
        radioInfoText = view.findViewById(R.id.radio_info_data);

        // Set animations for text scrolling
        radioInfoText.setAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.left_to_right));


        updateTextView();  // Update UI with current track info


        // Set stop radio and recording logic
        stopRadio.setOnClickListener(v -> {
            if (player != null) {
                player.stopMedia();
                player.stopRecording();
            }
        });

        // Set start/stop recording logic
        startRecordRadio.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_REQUEST_CODE);
            } else {
                if (player != null && !player.isRecording()) {
                    try {
                        player.recordMedia();
                        startRecordRadio.setImageResource(R.drawable.ic_baseline_fiber_manual_record_24);
                        radioPlayingName.setTextColor(getResources().getColor(R.color.white));
                        radioPlayingName.setBackgroundColor(getResources().getColor(R.color.red_transparent));
                        Toast.makeText(getContext(), "Recording Started", Toast.LENGTH_SHORT).show();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                } else if (player != null) {
                    player.stopRecording();
                    startRecordRadio.setImageResource(R.drawable.ic_stop_record_24);
                    radioPlayingName.setBackgroundColor(getResources().getColor(R.color.material_on_background_disabled));
                    radioPlayingName.setTextColor(getResources().getColor(R.color.white));
                    Toast.makeText(getContext(), "Recording Stopped", Toast.LENGTH_SHORT).show();
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
                    player = new PlayerAction(getActivity(), selectedRadio);
                    new RequestOptions().priority(Priority.HIGH).fitCenter().diskCacheStrategy(DiskCacheStrategy.ALL);

                    selectedRadioName = selectedRadio.getRadioName();
                    selectedRadioURL = selectedRadio.getRadioURLobj();
                    radioPlayingName.setText(selectedRadioName);
                    startRecordRadio.setImageResource(R.drawable.ic_stop_record_24);


                    player.stopRecording();
                    player.playMedia();
                    updateTextView();
                }
            }
        }));

        return view;
    }

    // Fetch the list of radios
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

    // Updates the currently playing track information.
    @SuppressLint("SetTextI18n")
    private void updateTextView() {
        new Thread(() -> {
            requireActivity().runOnUiThread(() -> {
                ParsingHeaderData streaming = new ParsingHeaderData();
                ParsingHeaderData.TrackData trackData = streaming.getTrackDetails(selectedRadioURL);
                radioPlayingName.setText(selectedRadioName);


                radioPlayingName.setBackgroundColor(getResources().getColor(R.color.material_on_background_disabled));
                String displayInfo = "- No track information -";
                radioInfoText.setBackgroundColor(getResources().getColor(R.color.material_on_surface_stroke));

                if (!trackData.artist.trim().isEmpty()) {
                    displayInfo = trackData.artist + " - " + trackData.title;
                }
                radioInfoText.setText(displayInfo);
            });

    }).start();
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