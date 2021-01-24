package com.r00tme.radiojava;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private RadioAdapter radioAdapter = null;
    private static final String[] filterBy = {"Name", "Genre", "Country"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        // Set program exit activity
        ImageButton exit = findViewById(R.id.exit_button);
        exit.setOnClickListener(v -> {
            finish();
            System.exit(0);
        });

        //** Set filter dropdown activity
        Spinner spinner = findViewById(R.id.filter_by);
        ArrayAdapter<CharSequence> filterAdapter = ArrayAdapter.createFromResource(
                this, R.array.filter_bys, android.R.layout.simple_spinner_item
        );
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(filterAdapter);
        spinner.setOnItemSelectedListener(this);

        //** Set radio list activity
        RecyclerView recyclerView = findViewById(R.id.radio_view_layout);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        try {
            radioAdapter = new RadioAdapter(fetchAllRadios(), this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        recyclerView.setAdapter(radioAdapter);
        RadioAdapter finalRadioAdapter = radioAdapter;

        //** Set search activity
        SearchView simpleSearchView = findViewById(R.id.search_radio);
        simpleSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                finalRadioAdapter.getFilter().filter(newText);
                return false;
            }
        });
    }


    private ArrayList<Radio> fetchAllRadios() throws IOException {

        ArrayList<Radio> radioList = new ArrayList<>();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .permitNetwork().build());
        try {
            URL radioListUrl = new URL("https://t-nikolov.com/radio/radio.txt");
            BufferedReader file = new BufferedReader(new InputStreamReader(radioListUrl.openStream()));
            String str;

            while ((str = file.readLine()) != null){
                String[] radioData = str.split(",");
                if (radioData.length != 5){
                    continue;
                }
                radioList.add(new Radio(radioData[0],radioData[1], radioData[2], radioData[3], radioData[4]));
            }
            file.close();
        }
        catch (MalformedURLException e){
            e.printStackTrace();
        }
        return radioList;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String filterBy = parent.getItemAtPosition(position).toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}