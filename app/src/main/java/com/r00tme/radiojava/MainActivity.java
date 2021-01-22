package com.r00tme.radiojava;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Button;
import android.widget.SearchView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public String search = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        /* Exit functionality */
        Button exit = findViewById(R.id.exit_button);
        exit.setOnClickListener(v -> {
            finish();
            System.exit(0);
        });


        RecyclerView recyclerView = findViewById(R.id.radio_view_layout);
        RadioAdapter radioAdapter = null;
        try {
            radioAdapter = new RadioAdapter(this, fetchAllRadios(search));
        } catch (IOException e) {
            e.printStackTrace();
        }

        recyclerView.setAdapter(radioAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        /* Main */

        SearchView simpleSearchView = findViewById(R.id.search_radio);

        simpleSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                search = newText;
                return true;
            }
        });


    }


    private ArrayList<Radio> fetchAllRadios(String search) throws IOException {

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
        if (search.length() > 0){
            ArrayList<Radio> newList = new ArrayList<>();
            for (Radio radio: radioList) {
                if(radio.getRadioName().contains(search)){
                    newList.add(radio);
                }
            }
            return newList;
        }
        return radioList;
    }
}