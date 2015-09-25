package com.example.imgwang_gug.plurry;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import com.example.imgwang_gug.plurry.websocket.WebSocketClient;

public class CreateSchedule extends AppCompatActivity {

    private String product;
    private String group;
    private SharedPreferences pref;
    private String prefName = "plurry";
    private Activity this_activity = this;
    private String token;

    private SeekBar feed;
    private TimePicker timepick;
    private TextView feedText;
    private WebSocketClient client = null;
    private WebSocketClient rs_client = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_schedule);

        feed = (SeekBar)findViewById(R.id.schedule_feed_amount);
        feedText = (TextView) findViewById(R.id.schedule_feed_text);
        timepick = (TimePicker)findViewById(R.id.schedule_time);

        Bundle b = getIntent().getExtras();
        product = b.getString("product");
        group = b.getString("group");
        token = getPreferences("secret_token");
        openWebsocket(product);

        feed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                feedText.setText(Integer.toString(progress + 1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    @Override
    protected void onPause() {
        client.disconnect();
        rs_client.disconnect();
        super.onPause();
    }
    @Override
    protected void onRestart() {
        openWebsocket(product);
        super.onRestart();
    }

    public void openWebsocket(String product_id) {
        List<BasicNameValuePair> extraHeaders = Arrays.asList(
                new BasicNameValuePair("Cookie", "session=" + token)
        );
        client = new WebSocketClient(URI.create("ws://plurry.cycorld.com:3000/ws/" + product_id), new WebSocketClient.Listener() {
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

        rs_client = new WebSocketClient(URI.create("ws://plurry.cycorld.com:3000/ws/debug/" + product_id), new WebSocketClient.Listener() {
            @Override
            public void onConnect() {
                Log.d("Connect", "Connected!");
                Log.d("Connect2", client.toString());
            }

            @Override
            public void onMessage(String message) {
                Log.d("Message", String.format("Debug Got string message! %s", message));
                try {
                    JSONObject rs = new JSONObject(message);

                    if(rs.has("rs") && rs.get("errcode").equals(0) && rs.get("rs").equals(104)) {
                        this_activity.finish();
                        Intent i = new Intent(this_activity, ScheduleList.class);
                        i.putExtra("group", group);
                        startActivity(i);
                    }
                } catch (JSONException e) {
                    Log.d("JSONException", "Error Rised -> " + e.getMessage());
                }
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
        rs_client.connect();
    }

    public void mOnClick(View v) {
        switch (v.getId()) {
            case R.id.create_schedule_btn:
                if (token.isEmpty()) {
                    Intent i = new Intent(this, Login.class);
                    startActivity(i);
                    this.finish();
                } else {
                    new productTask().execute(
                            "http://plurry.cycorld.com:3000/mobile/" + product + "/schedule",
                            "secret_token=" + token
                    );
                }
                break;
            case R.id.cancel_schedule_btn:
                this_activity.finish();
                Intent i = new Intent(this_activity, ScheduleList.class);
                i.putExtra("group", group);
                startActivity(i);
                break;
        }
    }

    public class productTask extends RequestTask {

        ProgressDialog dataPending = new ProgressDialog(this_activity);

        public String jsonConverter(String str) {
            str = str.replace("\\", "");
            str = str.replace("\"{", "{");
            str = str.replace("}\",", "},");
            str = str.replace("}\"", "}");

            return str;
        }

        public int currentTimePickSecond() {
            int h = timepick.getCurrentHour();
            int m = timepick.getCurrentMinute();
            return (h * 3600) + (m * 60);
        }

        public String currentTimePickString() {
            int h = timepick.getCurrentHour();
            int m = timepick.getCurrentMinute();
            String meridian = null;
            if( h == 0 ) {
                h += 12;
                meridian = "AM";
            } else if (h == 12) {
                meridian = "PM";
            } else if (h > 12) {
                h -= 12;
                meridian = "PM";
            } else {
                meridian = "AM";
            }
            String result = (h < 10 ? "0" + h : h) + ":" + (m < 10 ? "0" + m : m) + " " + meridian;
            return result;
        }

        @Override
        protected void onPreExecute() {
            dataPending.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dataPending.setMessage("데이터를 처리하는 중 입니다...");
            dataPending.show();
            super.onPreExecute();
        }

        protected void onPostExecute(String data) {
            dataPending.dismiss();
            // result is what you got from your connection
            if(!data.equals("fail")) {
                JSONObject resultJSON = null;
                String result = null;
                String what = null;
                try {
                    resultJSON = new JSONObject(data);
                    result = resultJSON.getString("result");
                    what = resultJSON.getString("what");
                    JSONObject target = null;
                    if(what.equals("product schedule")) {
                        JSONArray schedules = resultJSON.getJSONArray("data");
                        for(int i = 0; i < schedules.length();i++) {
                            JSONObject schedule = (JSONObject) schedules.get(i);
                            if(schedule.get("time").equals(currentTimePickString())) {
                                target = schedule;
                                break;
                            }
                        }
                        if(target == null) {
                            for(int i = 0; i < schedules.length();i++) {
                                JSONObject schedule = (JSONObject) schedules.get(i);
                                if(schedule.get("status").equals(true)) {
                                    target = schedule;
                                    break;
                                }
                            }
                        }
                        if(target == null) {
                            Toast.makeText(this_activity, "밥주기 예약은 20 개가 한도입니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            int id = target.getInt("id");
                            int amount = feed.getProgress();

                            JSONObject cmd = new JSONObject();
                            cmd.put("cmd", 4);
                            cmd.put("id", id);
                            cmd.put("timestamp", currentTimePickSecond());
                            cmd.put("amount", amount + 1);
                            client.send(cmd.toString());
                        }
                    }
                } catch (JSONException e) {
                    Log.d("JSONException", "ERROR " + e.getMessage());
                }
            } else {
                Toast.makeText(this_activity, "데이터를 불러오기를 실패하였습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_create_schedule, menu);
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
    public String getPreferences(String key) {
        pref = getSharedPreferences(prefName, MODE_PRIVATE);
        String data = pref.getString(key, "");
        return data;
    }

    // 값 저장하기
    public void savePreferences(String key, String value) {
        pref = getSharedPreferences(prefName, MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(key, value);
        editor.commit();
    }

    // 값(Key Data) 삭제하기
    public void removePreferences(String key) {
        pref = getSharedPreferences(prefName, MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.remove(key);
        editor.commit();
    }

    // 값(ALL Data) 삭제하기
    public void removeAllPreferences() {
        pref = getSharedPreferences(prefName, MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.clear();
        editor.commit();
    }
}
