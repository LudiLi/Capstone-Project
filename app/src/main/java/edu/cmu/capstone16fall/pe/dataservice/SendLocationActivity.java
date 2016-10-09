package edu.cmu.capstone16fall.pe.dataservice;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

/**
 * The app start from here. Create AccessTokenSingleton object and SendLocationActivity object.
 * Send location information with the strongest signal every 5s.
 *
 */
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

        //Check network connection. If no network, exit and print out error.
        System.out.println("Check network connection");
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            System.out.println("No network connection available.");
            System.exit(0);
        }

        //Get access token
        System.out.println("Start getting accessToken");
        accessTokenSingleton.setContext(this);
        accessToken = accessTokenSingleton.getAccessToken();
        System.out.println("Access Token" + accessToken);
        System.out.println("Time that the token saved " + accessTokenSingleton.getTokenTime());
        System.out.println("Token is valid or not " + accessTokenSingleton.isValidAccessToken());
        System.out.println("android id " + uniqueID);

        //Get sensor ID
        sensorIDSingleton.setup(this, uniqueID, accessToken);
        sensorID = sensorIDSingleton.getSensorID();
        System.out.println("Sensor ID " + sensorID);

        //Send location every 5s
        System.out.println("Start sending location info to server");
        sendLocation();

    }

    /**
     * Generate a random ID for the first time using our app and store it locally
     *
     * @param context
     * @return the random ID
     */
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

    /**
     * Create a thread to send location information and display all the wifi information to screen
     * every 5s.
     *
     */
    private void sendLocation() {
        final Handler handler = new Handler();


        handler.post(new Runnable() {
            @Override
            public void run() {
                TextView displayWifi = (TextView) findViewById(R.id.wifi_info);

                if (!accessTokenSingleton.isValidAccessToken()) {
                    accessToken = accessTokenSingleton.getAccessToken();
                }

                WifiReturnType wifiResult = getWifiID();
                if (wifiResult.maxScanResult != null) {
                    new SendLocationInfo().execute(wifiResult.maxScanResult.BSSID);
                    StringBuffer sb = new StringBuffer();
                    for (ScanResult sc : wifiResult.scanResult) {
                        sb.append(sc.SSID + "  " + sc.level + "  " + sc.BSSID + "\n");
                    }
                    displayWifi.setText(sb.toString());
                    displayWifi.invalidate();

                } else {
                    System.out.println("Not detect wifi signal");
                }

                handler.postDelayed(this, TIME_INTERVAL);
            }
        });
    }

    /**
     * Save all detected wifi information and the strongest wifi information
     *
     * @return wifi information
     */
    private WifiReturnType getWifiID() {
        //-100 means lowest value (no signal at all), and 0 means extremely good signal (100%)
        int maxLevel = Integer.MIN_VALUE;
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        List<ScanResult> list = wifiManager.getScanResults();
        ScanResult maxResult = null;
        for (ScanResult result : list) {
            if (result.level > maxLevel) {
                maxLevel = result.level;
                maxResult = result;
            }
        }

        WifiReturnType returnType = new WifiReturnType(list.toArray(new ScanResult[list.size()]),
                maxResult);


        return returnType;
        //return maxResult.SSID;
    }

    /**
     * Http Post location information.
     *
     */
    private class SendLocationInfo extends AsyncTask<String, Void, String> {
        HttpsURLConnection urlConnection;
        @Override
        protected String doInBackground(String... args) {
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
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);
                urlConnection.setDoOutput(true);
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
            } finally {
                urlConnection.disconnect();
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
