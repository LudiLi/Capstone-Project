package ludil.cmu.edu.dataserviceapplication;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.List;
import java.util.ArrayList;

//nest class to store the string result as well as the status
class Result {
    String value;
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
}


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initializeWiFiListener();

        TextView textView = (TextView)findViewById(R.id.textView);
        Result r = new Result();
        String username = "1231234";
        Double value = 23.12;
        int shareLocRes = 0, registerRes = 0;
        String accessToken = "";

        try {
            accessToken = doGetAccessToken();
            registerRes = doPostSensorId(r, username, accessToken);
            if (registerRes == 200) {
                 shareLocRes = doPostDataInfo(r, value, accessToken);
                 textView.append("\n Now you've registered successfully.");
            }
            if (shareLocRes == 200) {
                textView.append("\n We are now collecting your location information for safety reasons.");
            }
            int i = 0;
            while(shareLocRes == 200) {
                shareLocRes = doPostDataInfo(r, value, accessToken);
                Thread.sleep(5000);
                System.out.println(i++);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public String doGetAccessToken() throws Exception{
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet("https://ppa.andrew.cmu.edu:81/oauth/access_token/client_id=LRFWc9NPl95Qrsd4Jtugzs9gwj1Oto1Njsihmx5j/client_secret=mwqNr6DT0fdWL3ZNYovRS6mSaAvYXVf5ojBGfa9yFPoYfiALMc");
        HttpResponse response = httpclient.execute(httpGet);
        String content = EntityUtils.toString(response.getEntity()) ;
        String accessToken = new JSONObject(content).getString("access_token");
        return accessToken;
    }


    public int doPostSensorId(Result r, String name, String accessToken) throws Exception {
        JSONObject json = new JSONObject();
        JSONObject array = new JSONObject();
        array.put("name", name);
        array.put("identifier", "SensorTag3");
        array.put("building", "Wean hall");
        json.put("data", array);
        // Make call to a particular URL
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost("https://ppa.andrew.cmu.edu:81/api/sensor");
        httpPost.setHeader("Content-type", "application/json");
        httpPost.setHeader("Authorization", "Bearer " + accessToken);
        httpPost.setEntity(new StringEntity(json.toString(), "UTF8"));
        HttpResponse response = httpclient.execute(httpPost);
        int statusCode = response.getStatusLine().getStatusCode();
        // If things went poorly, don't try to read any response, just return.
        if (statusCode != 200) {
            // not using msg
            String msg = response.getEntity().toString();
            return statusCode;
        }
        String content = EntityUtils.toString(response.getEntity()) ;
        String sensorId = new JSONObject(content).getString("uuid");
        r.setValue(sensorId);
        return statusCode;
    }

    public int doPostDataInfo(Result r, Double value, String accessToken) throws Exception {
        Long time = System.currentTimeMillis() / 1000;
        System.out.println("%%%%%%%%" + time);
        JSONArray array = new JSONArray();
        JSONObject json = new JSONObject();
        json.put("sensor_id", r.getValue());
        json.put("samples", new JSONArray().put(new JSONObject().put("value", value).put("time", time)));
        array.put(json);
        // Make call to a particular URL
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost("https://ppa.andrew.cmu.edu:82/api/sensor/timeseries");
        httpPost.setHeader("Content-type", "application/json");
        httpPost.setHeader("Authorization", "Bearer " + accessToken);
        httpPost.setEntity(new StringEntity(array.toString(), "UTF8"));
        HttpResponse response = httpclient.execute(httpPost);
        int statusCode = response.getStatusLine().getStatusCode();
        return statusCode;
    }

    // scan all the wifi access point and find the strongest one
    public void initializeWiFiListener(){
        System.out.println("executing initializeWiFiListener");

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 12345);
        }
    }

    public void onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 12345) {
            System.out.println("lalalalalala");
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                System.out.println("22222222222222222222");
                // Permission granted.
                String connectivity_context = Context.WIFI_SERVICE;
                final WifiManager wifi = (WifiManager)getSystemService(connectivity_context);
                wifi.startScan();
                final List<ScanResult> results = wifi.getScanResults();//list of access points from the last scan
                for (final ScanResult result : results) {
                    System.out.println("ScanResult level: " + result.level);
                }
            }
        }
    }

}

