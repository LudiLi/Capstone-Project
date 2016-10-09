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

import javax.net.ssl.HttpsURLConnection;

/**
 * AccessTokenSingleton can get access token from server, store access toeken and the timestamp to
 * get this token locally, and verify whether the token is valid.
 *
 * Created by Zheng on 10/3/16.
 */
public class AccessTokenSingleton {
    private static final String LOCATION_PREFERENCE = "location_preference";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String TOKEN_TIME = "token_time";
    private static final String URL = "https://ppa.andrew.cmu.edu:81/oauth/access_token/" +
            "client_id=LRFWc9NPl95Qrsd4Jtugzs9gwj1Oto1Njsihmx5j/" +
            "client_secret=mwqNr6DT0fdWL3ZNYovRS6mSaAvYXVf5ojBGfa9yFPoYfiALMc";
    private static final long EXPIRE_TIME = 1 * 3600 * 1000;
    private static AccessTokenSingleton instance = new AccessTokenSingleton();
    private String accessToken;
    private Date tokenTime;
    private Context context;
    private SharedPreferences sharedPref;

    private AccessTokenSingleton(){}

    public static AccessTokenSingleton getInstance(){
        return instance;
    }

    /**
     * Main activity will call this method to get access token either from local storage or generate
     * a new one.
     *
     * @return the valid access token
     */
    public String getAccessToken() {
        sharedPref = context.getSharedPreferences(LOCATION_PREFERENCE,Context.MODE_PRIVATE);
        String tempToken = sharedPref.getString(ACCESS_TOKEN, null);
        String tempTime = sharedPref.getString(TOKEN_TIME, null);
        //If no access token stored locally, generate one.
        //If access token stored locally, pass it the accessToken local variable.
        if (tempToken == null) {
            generateAccessToken();
        } else {
            //if the local variable accessToken has not been initialized. Initialize it.
            if (accessToken == null) {
                accessToken = tempToken;
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                try {
                    tokenTime = dateFormat.parse(tempTime);
                } catch (ParseException e){
                    e.printStackTrace();
                }
            }
        }
        //If the access token we stored is not valid, generate new one.
        if (!isValidAccessToken()) {
            generateAccessToken();
        }

        return accessToken;
    }

    /**
     * Whether the access token is valid by comparing expiration time.
     *
     * @return true if the token is still good and not expired
     */
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

    /**
     * Generate a new access token
     *
     */
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

    /**
     * Add the new generated access token to the local storage.
     *
     * @param tempToken
     */
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

    /**
     * Use this context to access local storage.
     *
     * @param context from main activity
     */
    public void setContext(Context context) {
        this.context = context;
    }

    public String getTokenTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return dateFormat.format(tokenTime);
    }

    /**
     * Http request to get access token.
     *
     */
    private class GetAccessToken extends AsyncTask<String, Void, String> {
        HttpsURLConnection urlConnection;
        String accessToken;

        @Override
        protected String doInBackground(String... args) {
            StringBuilder result = new StringBuilder();

            try {
                URL url = new URL(args[0]);
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setDoInput(true);
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);
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




