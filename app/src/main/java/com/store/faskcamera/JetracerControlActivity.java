package com.store.faskcamera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class JetracerControlActivity extends AppCompatActivity {
    private Button buttonForward, buttonLeft, buttonRight, buttonForwardLeft, buttonForwardRight, buttonReverse, buttonReverseLeft, buttonReverseRight;
    private WebView webView;
    private static final String TAG = "4DBG";
    private ConnectionManager connectionManager;

    private TextView tvCelsius, tvFahrenheit;
    private Handler handler = new Handler();
    private Runnable runnable;
    private static final int INTERVAL = 5000; // 5 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jetracer_control);

        connectionManager = ConnectionManager.getInstance();

        buttonForward = findViewById(R.id.buttonForward);
        buttonLeft = findViewById(R.id.buttonLeft);
        buttonRight = findViewById(R.id.buttonRight);
        buttonForwardLeft = findViewById(R.id.buttonForwardLeft);
        buttonForwardRight = findViewById(R.id.buttonForwardRight);
        buttonReverse = findViewById(R.id.buttonReverse);
        buttonReverseRight = findViewById(R.id.buttonReverseRight);
        buttonReverseLeft = findViewById(R.id.buttonReverseLeft);

        setupButton(buttonForward, "forward");
        setupButton(buttonLeft, "left");
        setupButton(buttonRight, "right");
        setupButton(buttonForwardLeft, "forward_left");
        setupButton(buttonForwardRight, "forward_right");
        setupButton(buttonReverse, "reverse");
        setupButton(buttonReverseRight, "reverse_right");
        setupButton(buttonReverseLeft, "reverse_left");

        SharedPreferences sharedPref = getSharedPreferences("userConf", Context.MODE_PRIVATE);
        String ip = sharedPref.getString("serverIp", "192.168.1.13");
        connectionManager.setConnectionListener(() -> connectionManager.sendMessage("control_on"));
        connectionManager.startConnection(ip, Integer.parseInt(Constants.webViewPortControl));

        webView = findViewById(R.id.webViewControl);
        webView.setWebViewClient(new WebViewClient());
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setLoadWithOverviewMode(true);
        webView.setInitialScale(450);

        connectToStream();

        tvCelsius = findViewById(R.id.tvCelsius);
        tvFahrenheit = findViewById(R.id.tvFahrenheit);

        // Define the runnable to fetch data periodically
        runnable = new Runnable() {
            @Override
            public void run() {
                new FetchTemperatureTask().execute();
                handler.postDelayed(this, INTERVAL);
            }
        };
        // Start the periodic task
        handler.post(runnable);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupButton(Button button, String startCommand) {
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    connectionManager.sendMessage(startCommand);
                    return true;
                case MotionEvent.ACTION_UP:
                    connectionManager.sendMessage("stop");
                    return true;
            }
            return false;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connectionManager.stopConnection();
        handler.removeCallbacks(runnable);
    }

    private void connectToStream() {
        Log.i(TAG, "<strm> CONNECT");

        SharedPreferences sharedPref = getSharedPreferences("userConf", Context.MODE_PRIVATE);
        String ip = sharedPref.getString("serverIp", "192.168.1.13");
        Log.i(TAG, "http://" + ip + ":" + Constants.flaskVideoFeed);

        webView.loadUrl("http://" + ip + ":" + Constants.flaskVideoFeed);
    }

    @Override
    protected void onPause() {
        super.onPause();
        disconnectFromStream();
    }

    private void disconnectFromStream() {
        Log.i(TAG, "<strm> DISCONNECT");
        webView.loadUrl("");
    }

    private class FetchTemperatureTask extends AsyncTask<Void, Void, String[]> {

        @Override
        protected String[] doInBackground(Void... voids) {
            String[] result = new String[2];

            SharedPreferences sharedPref = getSharedPreferences("userConf", Context.MODE_PRIVATE);
            String ip = sharedPref.getString("serverIp", "192.168.1.13");
            try {
                // Fetch Celsius
                URL urlCelsius = new URL("http://" + ip + "/celsius");
                HttpURLConnection urlConnectionCelsius = (HttpURLConnection) urlCelsius.openConnection();
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnectionCelsius.getInputStream()));
                    result[0] = in.readLine();
                } finally {
                    urlConnectionCelsius.disconnect();
                }

                // Fetch Fahrenheit
                URL urlFahrenheit = new URL("http://" + ip + "/fahrenheit");
                HttpURLConnection urlConnectionFahrenheit = (HttpURLConnection) urlFahrenheit.openConnection();
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnectionFahrenheit.getInputStream()));
                    result[1] = in.readLine();
                } finally {
                    urlConnectionFahrenheit.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String[] result) {
            super.onPostExecute(result);
            if (result[0] != null) {
                tvCelsius.setText(result[0]  + " \u2103");
            }
            if (result[1] != null) {
                tvFahrenheit.setText(result[1]  + " \u2109");
            }
        }
    }
}
