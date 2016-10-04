package edu.cmu.capstone16fall.pe.dataservice;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

/**
 * Created by Zheng on 10/3/16.
 */

public class AccessTokenSingleton {
    private static final String LOCATION_PREFERENCE = "location_preference";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String TOKEN_TIME = "token_time";
    private static final String URL = "https://ppa.andrew.cmu.edu:81/oauth/access_token/" +
            "client_id=LRFWc9NPl95Qrsd4Jtugzs9gwj1Oto1Njsihmx5j/" +
            "client_secret=mwqNr6DT0fdWL3ZNYovRS6mSaAvYXVf5ojBGfa9yFPoYfiALMc";
    private static final long EXPIRE_TIME = 11 * 3600 * 1000;
    private static AccessTokenSingleton instance = new AccessTokenSingleton();
    private String accessToken;
    private Date tokenTime;
    private Context context;
    private SharedPreferences sharedPref;

    private AccessTokenSingleton(){}

    public static AccessTokenSingleton getInstance(){
        return instance;
    }

    public String getAccessToken() {
        sharedPref = context.getSharedPreferences(LOCATION_PREFERENCE,Context.MODE_PRIVATE);
        String tempToken = sharedPref.getString(ACCESS_TOKEN, null);
        String tempTime = sharedPref.getString(TOKEN_TIME, null);
        if (tempToken == null) {
            generateAccessToken();
        } else if (accessToken == null) {
            accessToken = tempToken;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            try {
                tokenTime = dateFormat.parse(tempTime);
            } catch (ParseException e){
                e.printStackTrace();
            }
        }
        if (!isValidAccessToken()) {
            generateAccessToken();
        }


        return accessToken;
    }

    public boolean isValidAccessToken() {
        if (tokenTime != null) {
            Date now = new Date();
            Date tokenExpire = new Date(tokenTime.getTime() + EXPIRE_TIME);
            if (tokenExpire.after(now)) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    private void generateAccessToken() {

            GetAccessToken doGetAccessToken = new GetAccessToken();
            try {
                doGetAccessToken.execute(URL).get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            String tempToken = doGetAccessToken.accessToken;

            addSharedPreferences(tempToken);

    }

    private void addSharedPreferences(String tempToken) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String timeStamp;

        Date date = new Date();
        timeStamp = dateFormat.format(date);

        sharedPref = context.getSharedPreferences(LOCATION_PREFERENCE,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(ACCESS_TOKEN, tempToken);
        editor.putString(TOKEN_TIME, timeStamp);
        editor.commit();
        accessToken = tempToken;
        tokenTime = date;
        Log.e("access token", accessToken);
        Log.e("token time", timeStamp);
    }


    public void setContext(Context context) {
        this.context = context;
    }

    public String getTokenTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return dateFormat.format(tokenTime);
    }

    private class GetAccessToken extends AsyncTask<String, Void, String> {
        HttpURLConnection urlConnection;
        String accessToken;

        @Override
        protected String doInBackground(String... args) {
            StringBuilder result = new StringBuilder();

            try {
                URL url = new URL(args[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                Log.e("ResponseCode", Integer.toString(urlConnection.getResponseCode()));
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

            }catch( Exception e) {
                e.printStackTrace();
            }
            finally {
                urlConnection.disconnect();
            }

            try {
                accessToken = new JSONObject(result.toString()).getString("access_token");
            } catch ( JSONException e) {
                e.printStackTrace();
            }

            return accessToken;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.e("Json Response", "" + result);

        }

    }

}




