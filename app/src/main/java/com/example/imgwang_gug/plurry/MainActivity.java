package com.example.imgwang_gug.plurry;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import com.example.imgwang_gug.plurry.websocket.WebSocketClient;
import com.example.imgwang_gug.plurry.joystick.JoystickView;
import com.example.imgwang_gug.plurry.joystick.JoystickView.OnJoystickMoveListener;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button feed_button;
    private SeekBar feed_amount;
    private TextView feed_text;
    private WebSocketClient client;
    private JoystickView joystick;
    private TextView joystick_debug;
    private SharedPreferences pref;
    private final String prefName = "plurry";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String token = getPreferences("secret_token");
        if(token.isEmpty()) {
            Intent i = new Intent(this, Login.class);
            startActivity(i);
            this.finish();
        }

        feed_button = (Button)findViewById(R.id.feed_btn);
        feed_amount = (SeekBar)findViewById(R.id.feed_amount);
        feed_text = (TextView)findViewById(R.id.feed_text);

        List<BasicNameValuePair> extraHeaders = Arrays.asList(
                new BasicNameValuePair("Cookie", "session=abcd")
        );

        client = new WebSocketClient(URI.create("ws://plurry.cycorld.com:3000/ws/feedbin-20150713-ffe0a3b2c"), new WebSocketClient.Listener() {
            @Override
            public void onConnect() {
                Log.d("Connect", "Connected!");
                Log.d("Connect2", client.toString());
            }

            @Override
            public void onMessage(String message) {
                Log.d("Message", String.format("Got string message! %s", message));
            }

            @Override
            public void onMessage(byte[] data) {
                Log.d("byte", String.format("Got binary message! %s", ("" + data)));
            }

            @Override
            public void onDisconnect(int code, String reason) {
                Log.d("Disconnect", String.format("Disconnected! Code: %d Reason: %s", code, reason));
            }

            @Override
            public void onError(Exception error) {
                Log.e("Error", "Error!", error);
            }

        }, extraHeaders);

        client.connect();
        //End WebsockeSetting

        feed_amount.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                feed_text.setText(Integer.toString(progress + 1));
            }
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        joystick = (JoystickView) findViewById(R.id.joystickView);
        joystick_debug = (TextView) findViewById(R.id.joystick_debug);

        // Listener of events, it'll return the angle in graus and power in percents
        // return to the direction of the moviment
        joystick.setOnJoystickMoveListener(new OnJoystickMoveListener() {
            @Override
            public void onValueChanged(int angle, int power, int direction, float x, float y) {
                try {
                    JSONObject left = new JSONObject();
                    JSONObject right = new JSONObject();
                    left.put("cmd", 8);
                    right.put("cmd", 9);
                    int left_speed = (int) (y + x + 50);
                    int right_speed = (int) (y - x + 50);
                    left.put("speed", left_speed);
                    right.put("speed", right_speed);
                    client.send(left.toString());
                    client.send(right.toString());
                } catch (JSONException e) {
                    Log.e("MYAPP", "unexpected JSON exception", e);
                }
                joystick_debug.setText("x : " + x + " y : " + y);
            }
        }, JoystickView.DEFAULT_LOOP_INTERVAL);

    }

    public void mOnClick(View v) {
        switch (v.getId()) {
            case R.id.feed_btn:
            int progress = feed_amount.getProgress();
            try {
                JSONObject cmd = new JSONObject();
                cmd.put("cmd", 6);
                cmd.put("amount", Integer.toString(progress + 1));
                client.send(cmd.toString());
            } catch(JSONException e) {
                Log.e("MYAPP", "unexpected JSON exception", e);
            }
            break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    //값 불러오기
    private String getPreferences(String key) {
        pref = getSharedPreferences(prefName, MODE_PRIVATE);
        String data = pref.getString(key, "");
        return data;
    }

    // 값 저장하기
    private void savePreferences(String key, String value) {
        pref = getSharedPreferences(prefName, MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(key, value);
        editor.commit();
    }

    // 값(Key Data) 삭제하기
    private void removePreferences(String key) {
        pref = getSharedPreferences(prefName, MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.remove(key);
        editor.commit();
    }

    // 값(ALL Data) 삭제하기
    private void removeAllPreferences() {
        pref = getSharedPreferences(prefName, MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.clear();
        editor.commit();
    }
}
