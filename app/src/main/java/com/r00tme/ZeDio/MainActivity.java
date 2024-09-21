package com.r00tme.ZeDio;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.view.animation.Animation;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        currentActivity = this;

// Set program exit activity  *************************************************
        ImageButton exitButton = findViewById(R.id.exit_button);
        exitButton.setOnClickListener(v -> {
            finish();
            System.exit(0);
        });


//** Set filter dropdown activity **********************************************
        Spinner spinner = findViewById(R.id.filter_by);
        ArrayAdapter<CharSequence> filterAdapter = ArrayAdapter.createFromResource(
                this, R.array.filter_bys, android.R.layout.simple_spinner_item
        );
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(filterAdapter);
        spinner.setOnItemSelectedListener(this);


//** Set radio list activity / Create RecycleView  *****************************

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

        stopRadio.setOnClickListener(v -> {
            player.stopMedia();
            layout.setVisibility(View.GONE);
        });

// Inside your OnClickListener:
        startRecordRadio.setOnClickListener(e -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_REQUEST_CODE);
            } else {
                if (!player.isRecording()) {  // Start recording
                    try {
                        player.recordMedia();
                        startRecordRadio.setImageResource(R.drawable.ic_stop_record_24);
                        radioPlayingName.setBackgroundColor(getResources().getColor(R.color.red_transparent));
                        radioPlayingName.setTextColor(getResources().getColor(R.color.white));
                        //Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
                        Toast.makeText(this, "Recording Started", Toast.LENGTH_SHORT).show();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                } else {  // Stop recording
                    player.stopRecording();
                    startRecordRadio.setImageResource(R.drawable.ic_baseline_fiber_manual_record_24);

                    radioPlayingName.setBackgroundColor(Color.TRANSPARENT); // Remove background color
                    radioPlayingName.setTextColor(getResources().getColor(R.color.default_text_color)); // Reset to default text color

                    Toast.makeText(this, "Recording Stopped", Toast.LENGTH_SHORT).show();
                }
            }
        });
        recyclerView.addOnItemTouchListener(

                new RecyclerItemClickListener(this, recyclerView, new RecyclerItemClickListener.OnItemClickListener() {

                    @Override
                    public void onItemClick(View view, int position) {
                    }

                    @Override
                    public void onLongItemClick(View view, int position) throws MalformedURLException {

                        if (!radioList.isEmpty()) {
                            Radio selectedRadio = radioList.get(position);
                            RequestOptions options = new RequestOptions()
                                    .priority(Priority.HIGH)
                                    .fitCenter()
                                    .diskCacheStrategy(DiskCacheStrategy.ALL);

                            layout.setVisibility(View.VISIBLE);

                            // Glide.with(currentActivity).asBitmap().apply(options).load(selectedRadio.getRadioLogo()).into(radioPlayingLogo);
                            radioPlayingName.setText(selectedRadio.getRadioName());

                            PlayerAction player = new PlayerAction(currentActivity, selectedRadio);
                            currentActivity.player = player;
                            selectedRadioURL = selectedRadio.getRadioURLobj();
                            player.playMedia();
                            updateTextView();

                        }
                    }
                })
        );

//** Set search activity  ******************************************************
        SearchView simpleSearchView = findViewById(R.id.search_radio);
        simpleSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                //TODO improving the construction of the search data. Possibly not regex to be used.

                if (finalRadioAdapter != null)
                    finalRadioAdapter.getFilter().filter(filteredBy + "#" + newText);
                return false;
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateTextView() {
        MainActivity.this.runOnUiThread(() -> {
            ParsingHeaderData streaming = new ParsingHeaderData();
            ParsingHeaderData.TrackData trackData = streaming.getTrackDetails(selectedRadioURL);
            TextView radioInfoText = findViewById(R.id.radio_info_data);
            String displayInfo = "- No track information -";
            if (!trackData.artist.trim().isEmpty()) {
                displayInfo = trackData.artist + " - " + trackData.title;
            }

            radioInfoText.setText(displayInfo);
        });

    }

    /**
     * Fetches the predefined text file from predefined URL
     * <p>
     * The file data structure -> Name, Genre, Country, StreamingUrl, LogoImage
     * The file is read line by line and each line is slitted and added to the new Radio model
     * Badly formatted data won't be added. The method expects exactly 5 elements
     *
     * @return List of type Radio
     * @throws IOException if the URL is unreachable or file can not be read
     */
    private ArrayList<Radio> fetchAllRadios() throws IOException {
        ArrayList<Radio> radioList = new ArrayList<>();
        //TODO Add an additional radio list fetch options/methods. Possibly TuneIn XML API

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .permitNetwork().build());
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
            throw new InvalidObjectException("Can not access the remote link, containing all radios");
        }
        this.radioList = radioList;
        return radioList;
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        SearchView simpleSearchView = findViewById(R.id.search_radio);
        filteredBy = parent.getItemAtPosition(position).toString();
        simpleSearchView.setQuery("", false);
        simpleSearchView.clearFocus();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}