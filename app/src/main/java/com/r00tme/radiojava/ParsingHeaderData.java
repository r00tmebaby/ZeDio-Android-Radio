package com.r00tme.radiojava;

import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParsingHeaderData {
    public class TrackData {
        public String artist = "";
        public String title = "";
    }

    protected URL streamUrl;
    private Map<String, String> metadata;
    private TrackData trackData;

    public ParsingHeaderData() {

    }

    public TrackData getTrackDetails(URL streamUrl) {
        trackData = new TrackData();
        setStreamUrl(streamUrl);
        String strTitle;
        String strArtist = null;
        try {
            metadata = executeToFetchData();
            if (metadata != null) {
                String streamHeading = "";
                Map<String, String> data = metadata;
                if (data != null && data.containsKey("StreamTitle")) {
                    strArtist = data.get("StreamTitle");
                    streamHeading = strArtist;
                }
                if (!TextUtils.isEmpty(strArtist) && strArtist.contains("-")) {
                    strArtist = strArtist.substring(0, strArtist.indexOf("-"));
                    trackData.artist = strArtist.trim();
                }
                if (!TextUtils.isEmpty(streamHeading)) {
                    if (streamHeading.contains("-")) {
                        strTitle = streamHeading.substring(streamHeading
                                .indexOf("-") + 1);
                        trackData.title = strTitle.trim();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return trackData;
    }

    private URLConnection con;
    private InputStream stream;
    private List<String> headerList;

    private Map<String, String> executeToFetchData() throws IOException {
        try {
            con = streamUrl.openConnection();

            con.setRequestProperty("Icy-MetaData", "1");
            // con.setRequestProperty("Connection", "close");
            // con.setRequestProperty("Accept", null);
            con.connect();

            int metaDataOffset;
            Map<String, List<String>> headers = con.getHeaderFields();
            stream = con.getInputStream();

            if (headers.containsKey("icy-metaint")) {
                headerList = headers.get("icy-metaint");
                if (headerList != null) {
                    if (headerList.size() > 0) {
                        metaDataOffset = Integer.parseInt(headers.get(
                                "icy-metaint").get(0));
                    } else
                        return null;
                } else
                    return null;

            } else {
                return null;

            }

            // In case no data was sent
            if (metaDataOffset == 0) {
                return null;
            }

            // Read metadata
            int b;
            int count = 0;
            int metaDataLength = 4080; // 4080 is the max length
            boolean inData;
            StringBuilder metaData = new StringBuilder();
            while ((b = stream.read()) != -1) {
                count++;
                if (count == metaDataOffset + 1) {
                    metaDataLength = b * 16;
                }
                inData = count > metaDataOffset + 1
                        && count < (metaDataOffset + metaDataLength);
                if (inData) {
                    if (b != 0) {
                        metaData.append((char) b);
                    }
                }
                if (count > (metaDataOffset + metaDataLength)) {
                    break;
                }

            }
            metadata = ParsingHeaderData.parsingMetadata(metaData.toString());
            stream.close();
        } catch (Exception e) {
            if (e != null && e.equals(null))
                Log.e("Error", e.getMessage());
        } finally {
            if (stream != null)
                stream.close();
        }
        return metadata;

    }

    public URL getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(URL streamUrl) {
        this.metadata = null;
        this.streamUrl = streamUrl;
    }

    public static Map<String, String> parsingMetadata(String metaString) {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        Map<String, String> metadata = new HashMap();
        String[] metaParts = metaString.split(";");
        Pattern p = Pattern.compile("^([a-zA-Z]+)='([^']*)'$");
        Matcher m;
        for (String metaPart : metaParts) {
            m = p.matcher(metaPart);
            if (m.find()) {
                metadata.put((String) m.group(1), (String) m.group(2));
            }
        }

        return metadata;
    }
}