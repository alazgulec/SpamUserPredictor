package com.alazgulec.spamuserpretender;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;


public class MainActivity extends ActionBarActivity {

    private TextView pinPlaceHolder;
    private EditText pinInput;
    private EditText userNameInput;
    private Button submitButton;
    private Button userSubmitButton;

    private String TOKEN_PREFS = "TOKEN_PREFS";
    private String CONSUMER_KEY = "yoVk94q24DSmzKw5ZnLJ88adx";
    private String CONSUMER_SECRET = "jnGks3E6du8d1KsxJsAC8mt5qAQx0XFC8Uw1usWB33j9w4nUe2";

    Twitter twitter;
    AccessToken accessToken;
    RequestToken requestToken;
    String pin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        pinPlaceHolder = (TextView) findViewById(R.id.pin_placeholder);
        pinInput = (EditText) findViewById(R.id.pin);
        userNameInput = (EditText) findViewById(R.id.username_input);
        submitButton = (Button) findViewById(R.id.submit_button);
        userSubmitButton = (Button) findViewById(R.id.user_submit_button);

        userSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userName = userNameInput.getText().toString();
                Intent intent = new Intent(MainActivity.this, QueryActivity.class);
                intent.putExtra("userName", userName);
                startActivity(intent);
            }
        });

        twitter = TwitterFactory.getSingleton();
        twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
        try {
            requestToken = twitter.getOAuthRequestToken();
        } catch (TwitterException te) {
            requestToken = null;
        }

        SharedPreferences prefs = getSharedPreferences(TOKEN_PREFS, MODE_PRIVATE);
        String token = prefs.getString("token", null);
        String secret = prefs.getString("secret", null);
        if (token != null && secret != null) {
            accessToken = new AccessToken(token, secret);
        } else {
            accessToken = null;
        }
        if (accessToken == null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Open the following URL and grant access to your account:");
            String url = requestToken.getAuthorizationURL();
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
            System.out.print("Enter the PIN(if aviailable) or just hit enter.[PIN]:");

            submitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    pin = pinInput.getText().toString();
                    Toast.makeText(getApplicationContext(), pin, Toast.LENGTH_SHORT);
                    try {
                        if (pin.length() > 0) {
                            accessToken = twitter.getOAuthAccessToken(requestToken, pin);
                            SharedPreferences.Editor editor = getSharedPreferences(TOKEN_PREFS, MODE_PRIVATE).edit();
                            editor.putString("token", accessToken.getToken());
                            editor.putString("secret", accessToken.getTokenSecret());
                            editor.commit();
                            pinPlaceHolder.setVisibility(View.GONE);
                            pinInput.setVisibility(View.GONE);
                            submitButton.setVisibility(View.GONE);
                            userNameInput.setVisibility(View.VISIBLE);
                            userSubmitButton.setVisibility(View.VISIBLE);
                        } else {
                            accessToken = twitter.getOAuthAccessToken();
                        }
                    } catch (TwitterException te) {
                        if (401 == te.getStatusCode()) {
                            System.out.println("Unable to get the access token.");
                            System.out.println(te.toString());
                        } else {
                            te.printStackTrace();
                        }
                    }
                }
            });

        } else {
            twitter.setOAuthAccessToken(accessToken);
            pinPlaceHolder.setVisibility(View.GONE);
            pinInput.setVisibility(View.GONE);
            submitButton.setVisibility(View.GONE);
            userNameInput.setVisibility(View.VISIBLE);
            userSubmitButton.setVisibility(View.VISIBLE);
        }
    }

}
