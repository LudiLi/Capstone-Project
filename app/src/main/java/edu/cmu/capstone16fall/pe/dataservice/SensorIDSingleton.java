package edu.cmu.capstone16fall.pe.dataservice;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Zheng on 10/4/16.
 */
public class SensorIDSingleton {
    private static SensorIDSingleton ourInstance = new SensorIDSingleton();
    private SharedPreferences sharedPref;
    private Context context;
    private String uniqueID;
    private static final String SENSOR_ID = "sensor_id";
    private static final String URL = "https://ppa.andrew.cmu.edu:81/api/sensor";
    private static String sensorID;
    private static String accessToken;

    public static SensorIDSingleton getInstance() {
        return ourInstance;
    }

    private SensorIDSingleton() {}

    public String getSensorID() {
        sharedPref = context.getSharedPreferences(SENSOR_ID, Context.MODE_PRIVATE);
        String tempToken = sharedPref.getString(SENSOR_ID, null);
        if (sensorID != null) {
            return sensorID;
        } else if (tempToken != null) {
            sensorID = tempToken;
        } else {
            sensorID = createSensorID();
        }

        return sensorID;
    }

    public String createSensorID() {
        GetSensorID doGetSensorID = new GetSensorID();
        try {
            doGetSensorID.execute(URL).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        String tempSensorID = doGetSensorID.tempSensorID;
        sharedPref = context.getSharedPreferences(SENSOR_ID, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(SENSOR_ID, tempSensorID);
        editor.commit();

        return tempSensorID;
    }

    public void setup(Context context, String uniqueID, String accessToken) {
        this.context = context;
        this.accessToken = accessToken;
        this.uniqueID = uniqueID;
    }

    private String fetchSensorID(String myurl) throws IOException {
        JSONObject json = new JSONObject();
        JSONObject array = new JSONObject();
        StringBuilder result = new StringBuilder();
        String tempSensorID = "0";
        URL url = new URL(myurl);
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        try {
            array.put("name", uniqueID);
            array.put("identifier", "SensorTag3");
            array.put("building", "Wean hall");
            json.put("data", array);
            String jsonData = json.toString();
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setReadTimeout(10000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-type", "application/json");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
            Writer writer = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8"));
            writer.write(jsonData);
            writer.close();

            urlConnection.connect();


            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

            } else {
                System.out.println("ResponseCode is not 200");
                throw new Exception();
            }


        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            urlConnection.disconnect();
        }

        try {
            tempSensorID = new JSONObject(result.toString()).getString("uuid");
        } catch ( JSONException e) {
            e.printStackTrace();
        }

        return tempSensorID;
    }

    private class GetSensorID extends AsyncTask<String, Void, String> {

        String tempSensorID;

        @Override
        protected String doInBackground(String... args) {

            try {
                tempSensorID = fetchSensorID(args[0]);
                return tempSensorID;

            }catch( Exception e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }

        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.e("Sensor ID", "" + result);
        }
    }

}
