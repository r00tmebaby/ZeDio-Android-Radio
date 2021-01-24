package com.r00tme.radiojava;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.Spinner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private RadioAdapter radioAdapter;
    private String filteredBy;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


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

                finalRadioAdapter.getFilter().filter(filteredBy + "#" + newText);
                return false;
            }
        });
    }



/**
 *  Fetches the predefined text file from predefined URL
 *
 *  The file data structure -> Name, Genre, Country, StreamingUrl, LogoImage
 *  The file is read line by line and each line is slitted and added to the new Radio model
 *  Badly formatted data wont be added. The method expects exactly 5 elements
 *
 * @return Array of type Radio
 * @throws IOException if the URL is unreachable or file can not be read
 */

    private ArrayList<Radio> fetchAllRadios() throws IOException {
        ArrayList<Radio> radioList = new ArrayList<>();
        //TODO Add an additional radio list fetch options/methods. Possibly TuneIn XML API

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
        SearchView simpleSearchView = findViewById(R.id.search_radio);
        filteredBy = parent.getItemAtPosition(position).toString();
        simpleSearchView.setQuery("", false);
        simpleSearchView.clearFocus();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) { }
}