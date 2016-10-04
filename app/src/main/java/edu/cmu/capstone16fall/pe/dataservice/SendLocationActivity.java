package edu.cmu.capstone16fall.pe.dataservice;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class SendLocationActivity extends AppCompatActivity {
    private static String uniqueID;
    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";
    private static final String URL = "https://ppa.andrew.cmu.edu:82/api/sensor/timeseries";
    private static final int TIME_INTERVAL = 5000;
    private AccessTokenSingleton accessTokenSingleton = AccessTokenSingleton.getInstance();
    private SensorIDSingleton sensorIDSingleton = SensorIDSingleton.getInstance();
    private String accessToken;
    private String sensorID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_location);

        getID(this);
        System.out.println("start getting accessToken");
        accessTokenSingleton.setContext(this);
        accessToken = accessTokenSingleton.getAccessToken();
        System.out.println("Access Token" + accessToken);
        System.out.println("Time that the token saved " + accessTokenSingleton.getTokenTime());
        System.out.println("Token is valid or not " + accessTokenSingleton.isValidAccessToken());
        System.out.println("android id " + uniqueID);

        sensorIDSingleton.setup(this, uniqueID, accessToken);
        sensorID = sensorIDSingleton.getSensorID();
        System.out.println("Sensor ID " + sensorID);

        sendLocation();

    }

    public synchronized static String getID(Context context) {
        if (uniqueID == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(
                    PREF_UNIQUE_ID, Context.MODE_PRIVATE);
            uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
            if (uniqueID == null) {
                uniqueID = UUID.randomUUID().toString();
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(PREF_UNIQUE_ID, uniqueID);
                editor.commit();
            }
        }
        return uniqueID;
    }

    private void sendLocation() {
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!accessTokenSingleton.isValidAccessToken()) {
                    accessToken = accessTokenSingleton.getAccessToken();
                }
                new SendLocationInfo().execute("test");

                handler.postDelayed(this, TIME_INTERVAL);
            }
        });
    }

    private class SendLocationInfo extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... args) {
            HttpURLConnection urlConnection;
            int responseCode = 0;
            String result;

            try {
                Long time = System.currentTimeMillis() / 1000;
                JSONArray array = new JSONArray();
                JSONObject json = new JSONObject();
                json.put("sensor_id", sensorID);
                json.put("samples", new JSONArray().put(new JSONObject().put("value", args[0]).put("time", time)));
                array.put(json);
                String jsonData = array.toString();
                URL url = new URL(URL);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-type", "application/json");
                urlConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
                Writer writer = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8"));
                writer.write(jsonData);
                writer.close();

                urlConnection.connect();

                responseCode = urlConnection.getResponseCode();

            } catch (Exception e) {
                e.printStackTrace();
            }
            result = "Response Code " + Integer.toString(responseCode) + " Wifi " + args[0];

            return result;
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            System.out.println(result);
        }
    }
}
