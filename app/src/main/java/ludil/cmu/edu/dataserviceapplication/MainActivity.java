package ludil.cmu.edu.dataserviceapplication;

import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String username = "1231234";

        try {
            int res = doPostJSON(username);
            System.out.println(res);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public int doPostJSON( String name) throws Exception {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("identifier", "SensorTag3");
        json.put("building", "Wean Hall");
        StringEntity se = new StringEntity(json.toString());
        // Make call to a particular URL
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost("https://ppa.andrew.cmu.edu:81/api/sensor");
        httpPost.setHeader("Authorization", "Bearer 67f15961e1a30c4a1f4b8cd3e149abb0");
        httpPost.setEntity(se);
        HttpResponse response = httpclient.execute(httpPost);
        int statusCode = response.getStatusLine().getStatusCode();
        return statusCode;
    }
}

