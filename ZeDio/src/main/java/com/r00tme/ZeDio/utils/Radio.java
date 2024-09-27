package com.r00tme.ZeDio.utils;

import java.net.MalformedURLException;
import java.net.URL;

public class Radio {
    private final String radioName;
    private final String radioGenre;
    private final String radioCountry;
    private final String radioUrl;
    private final String radioLogo;

    public Radio(String radio_name, String radio_genre, String radio_country, String radio_url, String radio_logo) {
        this.radioName = radio_name.trim();
        this.radioGenre = radio_genre.trim();
        this.radioCountry = radio_country.trim();
        this.radioUrl = radio_url;
        this.radioLogo = radio_logo.trim();
    }


    public String getRadioUrl() {
        return radioUrl;
    }

    public URL getRadioURLobj() throws MalformedURLException {
        return new URL(radioUrl);
    }

    public String getRadioName() {
        return radioName;
    }

    public String getRadioLogo() {
        return radioLogo;
    }

    public String getRadioGenre() {
        return radioGenre;
    }

    public String getRadioCountry() {
        return radioCountry;
    }

}
