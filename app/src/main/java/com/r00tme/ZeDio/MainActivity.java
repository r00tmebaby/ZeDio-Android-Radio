package com.r00tme.ZeDio;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.r00tme.ZeDio.actions.PlayerAction;
import com.r00tme.ZeDio.adapters.RadioAdapter;
import com.r00tme.ZeDio.classes.Radio;
import com.r00tme.ZeDio.parsers.ParsingHeaderData;
import androidx.appcompat.widget.SearchView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InvalidObjectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private static final int RECORD_AUDIO_REQUEST_CODE = 10;
    private RadioAdapter radioAdapter;
    private String filteredBy;
    private List<Radio> radioList = new ArrayList<>();
    private PlayerAction player;
    private MainActivity currentActivity;
    private URL selectedRadioURL;

    /**
     * Handles the result of permission requests.
     *
     * @param requestCode the request code passed in requestPermissions()
     * @param permissions the requested permissions
     * @param grantResults the corresponding results for each permission
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    player.recordMedia();  // Start recording when permission is granted
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(
                        this,
                        "Recording permission denied. Please enable it in settings to record audio.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    /**
     * Initializes the activity, setting up views and listeners.
     *
     * @param savedInstanceState the state of the activity before it was recreated
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        currentActivity = this;

        // Set program exit activity
        ImageButton exitButton = findViewById(R.id.exit_button);
        exitButton.setOnClickListener(v -> {
            finish();
            System.exit(0);
        });

        // Set filter dropdown activity
        Spinner spinner = findViewById(R.id.filter_by);
        ArrayAdapter<CharSequence> filterAdapter = ArrayAdapter.createFromResource(
                this, R.array.filter_bys, android.R.layout.simple_spinner_item
        );
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(filterAdapter);
        spinner.setOnItemSelectedListener(this);

        // Set radio list activity / Create RecyclerView
        RecyclerView recyclerView = findViewById(R.id.radio_view_layout);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        try {
            radioAdapter = new RadioAdapter(fetchAllRadios(), this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        recyclerView.setAdapter(radioAdapter);
        RadioAdapter finalRadioAdapter = radioAdapter;

        TextView radioPlayingName = currentActivity.findViewById(R.id.playing_name);
        currentActivity.findViewById(R.id.radio_info_data).setAnimation(
                AnimationUtils.loadAnimation(this, R.anim.left_to_right)
        );
        ImageButton startRecordRadio = currentActivity.findViewById(R.id.start_recording);
        ImageButton stopRadio = currentActivity.findViewById(R.id.stop_playing);
        LinearLayout layout = findViewById(R.id.playing_radio_layout);

        // Stop radio and recording
        stopRadio.setOnClickListener(v -> {
            player.stopMedia();
            player.stopRecording();
            layout.setVisibility(View.GONE);
        });

        // Start or stop recording
        startRecordRadio.setOnClickListener(e -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_REQUEST_CODE);
            } else {
                if (!player.isRecording()) {
                    try {
                        player.recordMedia();
                        startRecordRadio.setImageResource(R.drawable.ic_baseline_fiber_manual_record_24);
                        radioPlayingName.setTextColor(getResources().getColor(R.color.white));
                        radioPlayingName.setBackgroundColor(getResources().getColor(R.color.red_transparent));

                        Toast.makeText(this, "Recording Started", Toast.LENGTH_SHORT).show();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                } else {  // Stop recording
                    player.stopRecording();
                    startRecordRadio.setImageResource(R.drawable.ic_stop_record_24);
                    radioPlayingName.setBackgroundColor(getResources().getColor(R.color.material_on_background_disabled));
                    radioPlayingName.setTextColor(getResources().getColor(R.color.white));

                    Toast.makeText(this, "Recording Stopped", Toast.LENGTH_SHORT).show();
                }
            }
        });

        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(this, recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        // Handle item click if needed
                    }

                    @SuppressLint("CheckResult")
                    @Override
                    public void onLongItemClick(View view, int position) throws MalformedURLException {
                        if (!radioList.isEmpty()) {
                            Radio selectedRadio = radioList.get(position);
                            PlayerAction player = new PlayerAction(currentActivity, selectedRadio);
                            new RequestOptions().priority(Priority.HIGH).fitCenter().diskCacheStrategy(DiskCacheStrategy.ALL);

                            layout.setVisibility(View.VISIBLE);
                            radioPlayingName.setText(selectedRadio.getRadioName());
                            startRecordRadio.setImageResource(R.drawable.ic_stop_record_24);
                            currentActivity.player = player;
                            selectedRadioURL = selectedRadio.getRadioURLobj();

                            player.stopRecording();
                            player.playMedia();
                            updateTextView();
                        }
                    }
                })
        );

        // Set search activity
        SearchView simpleSearchView = findViewById(R.id.search_radio);
        simpleSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (finalRadioAdapter != null)
                    finalRadioAdapter.getFilter().filter(filteredBy + "#" + newText);
                return false;
            }
        });
    }

    /**
     * Updates the currently playing track information.
     */
    @SuppressLint("SetTextI18n")
    private void updateTextView() {
        MainActivity.this.runOnUiThread(() -> {
            ParsingHeaderData streaming = new ParsingHeaderData();
            ParsingHeaderData.TrackData trackData = streaming.getTrackDetails(selectedRadioURL);
            TextView radioInfoText = findViewById(R.id.radio_info_data);

            findViewById(R.id.playing_name).setBackgroundColor(getResources().getColor(
                    R.color.material_on_background_disabled)
            );
            String displayInfo = "- No track information -";
            radioInfoText.setBackgroundColor(getResources().getColor(R.color.material_on_surface_stroke));
            if (!trackData.artist.trim().isEmpty()) {
                displayInfo = trackData.artist + " - " + trackData.title;
            }

            radioInfoText.setText(displayInfo);
        });
    }

    /**
     * Fetches the predefined text file containing radio data.
     *
     * The file structure: Name, Genre, Country, StreamingUrl, LogoImage
     * Badly formatted data won't be added. The method expects exactly 5 elements per line.
     *
     * @return List of radios from the text file
     * @throws IOException if the URL is unreachable or file cannot be read
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
                System.out.println(Arrays.toString(radioData));
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
     * Called when an item in the filter dropdown is selected.
     *
     * @param parent the parent view of the spinner
     * @param view the selected view
     * @param position the position of the selected item
     * @param id the id of the selected item
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        SearchView simpleSearchView = findViewById(R.id.search_radio);
        filteredBy = parent.getItemAtPosition(position).toString();
        simpleSearchView.setQuery("", false);
        simpleSearchView.clearFocus();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // No action needed
    }
}
