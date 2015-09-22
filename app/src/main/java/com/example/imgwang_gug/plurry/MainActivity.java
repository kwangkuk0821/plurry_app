package com.example.imgwang_gug.plurry;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import com.example.imgwang_gug.plurry.websocket.WebSocketClient;
import com.example.imgwang_gug.plurry.joystick.JoystickView;
import com.example.imgwang_gug.plurry.joystick.JoystickView.OnJoystickMoveListener;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static HttpURLConnection conn;
    private SeekBar feed_amount;
    private TextView feed_text;
    private WebSocketClient[] client;
    private JoystickView joystick;
    private TextView joystick_debug;
    private LinearLayout feed_area;
    private RelativeLayout joystick_area;
    private String token;
    private final int feed_product = 1;
    private final int move_product = 2;
    private Activity this_activity = this;
    private String group;
    private SharedPreferences pref;
    private String prefName = "plurry";

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private ListView navList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        feed_area = (LinearLayout) findViewById(R.id.feed_area);
        joystick_area = (RelativeLayout) findViewById(R.id.joystick_area);
        feed_amount = (SeekBar) findViewById(R.id.feed_amount);
        feed_text = (TextView) findViewById(R.id.feed_text);
        joystick = (JoystickView) findViewById(R.id.joystickView);
        joystick_debug = (TextView) findViewById(R.id.joystick_debug);

        Bundle b = getIntent().getExtras();
        group = b.getString("group");

        token = getPreferences("secret_token");
        if (token.isEmpty()) {
            Intent i = new Intent(this, Login.class);
            startActivity(i);
            this.finish();
        } else {
            new productTask().execute(
                    "http://plurry.cycorld.com:3000/mobile/" + group + "/products",
                    "secret_token=" + token
            );
        }
        //메뉴 빛 툴바
        final Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTitle = getTitle();
        mDrawerTitle = "Products";

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navList = (ListView) findViewById(R.id.nav_list);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                toolbar,
                R.string.open_drawer,
                R.string.close_drawer)
        {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                toolbar.setTitle(mTitle);
                invalidateOptionsMenu();
                syncState();
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                toolbar.setTitle(mDrawerTitle);
                invalidateOptionsMenu();
                syncState();
            }
        };
        final String[] navItems = { "control", "schedule", "new product" };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        navList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, navItems));
        navList.setOnItemClickListener(new DrawerItemClickListener());

    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent i;
            switch (position) {
                case 0:
                    break;
                case 1:
                    i = new Intent(this_activity, ScheduleList.class);
                    i.putExtra("group", group);
                    startActivity(i);
                    this_activity.finish();
                    break;

                case 2:
                    i = new Intent(this_activity, AddProducts.class);
                    i.putExtra("group", group);
                    startActivity(i);
                    this_activity.finish();
                    break;
            }
        }
    }
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    public void mOnClick(View v) {
        switch (v.getId()) {
            case R.id.feed_btn:
                int progress = feed_amount.getProgress();
                try {
                    JSONObject cmd = new JSONObject();
                    cmd.put("cmd", 6);
                    cmd.put("amount", Integer.toString(progress + 1));
                    client[feed_product].send(cmd.toString());
                } catch (JSONException e) {
                    Log.e("MYAPP", "unexpected JSON exception", e);
                }
                break;
            case R.id.menu_sign_out:
                new logoutTask().execute(
                        "http://plurry.cycorld.com:3000/mobile/users/sign_out",
                        "secret_token=" + token
                );
                break;
        }
    }

    public void websocket(String product_id, int type) {
        List<BasicNameValuePair> extraHeaders = Arrays.asList(
                new BasicNameValuePair("Cookie", "session=" + token)
        );
        client[type] = new WebSocketClient(URI.create("ws://plurry.cycorld.com:3000/ws/" + product_id), new WebSocketClient.Listener() {
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

        client[type].connect();
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

        @Override
        protected void onPreExecute() {
            dataPending.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dataPending.setMessage("데이터를 불러오는 중 입니다...");

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
                JSONArray products = null;
                try {
                    resultJSON = new JSONObject(data);
                    products = resultJSON.getJSONArray("data");
                    client = new WebSocketClient[4];
                    for(int i = 0; i < products.length();i++) {
                        JSONObject product = (JSONObject) products.get(i);
                        websocket(product.getString("product_id"), product.getInt("product_type"));
                    }
                    removeProductView();
                    Log.d("task_result", "result = " + resultJSON);
                    Log.d("websockes", "result = " + client);
                } catch (JSONException e) {
                    Log.d("JSONException", "ERROR " + e.getMessage());
                }
            } else {
                Toast.makeText(this_activity, "데이터를 불러오기를 실패하였습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public class logoutTask extends RequestTask {

        ProgressDialog logoutPending = new ProgressDialog(this_activity);

        public String jsonConverter(String str) {
            str = str.replace("\\", "");
            str = str.replace("\"{", "{");
            str = str.replace("}\",", "},");
            str = str.replace("}\"", "}");

            return str;
        }

        @Override
        protected void onPreExecute() {
            logoutPending.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            logoutPending.setMessage("데이터를 불러오는 중 입니다...");

            logoutPending.show();
            super.onPreExecute();
        }

        protected void onPostExecute(String data) {
            logoutPending.dismiss();
            // result is what you got from your connection
            if(!data.equals("fail")) {
                JSONObject resultJSON = null;
                String result = null;
                String what = null;
                try {
                    resultJSON = new JSONObject(data);
                    result = resultJSON.getString("result");
                    what = resultJSON.getString("what");
                    if(result.equals("success") && what.equals("logout")) {
                        removePreferences("secret_token");
                        Intent i = new Intent(this_activity, Login.class);
                        startActivity(i);
                        Toast.makeText(this_activity, "로그아웃 성공하였습니다.", Toast.LENGTH_SHORT).show();
                        this_activity.finish();
                    } else {
                        Toast.makeText(this_activity, "로그아웃 실패하였습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    Log.d("JSONException", "ERROR " + e.getMessage());
                }
            } else {
                Toast.makeText(this_activity, "로그아웃 실패하였습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void removeProductView() {
        if(client[feed_product] == null) {
            feed_area.removeAllViews();
        } else {
            feed_area.setVisibility(View.VISIBLE);
            feed_amount.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    feed_text.setText(Integer.toString(progress + 1));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

        }
        if(client[move_product] == null) {
            joystick_area.removeAllViews();
        } else {
            joystick_area.setVisibility(View.VISIBLE);
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
                        client[move_product].send(left.toString());
                        client[move_product].send(right.toString());
                    } catch (JSONException e) {
                        Log.e("MYAPP", "unexpected JSON exception", e);
                    }
                    joystick_debug.setText("x : " + x + " y : " + y);
                }
            }, JoystickView.DEFAULT_LOOP_INTERVAL);
        }
    }
    @Override
    protected  void onPause() {
        Log.d("stop","stop!!!!!!!!!");
        for(int i = 0; i < client.length;i++) {
            if(client[i] != null) client[i].disconnect();
        }
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_logined, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        // This handle among other things open & close the drawer
        // when the navigation icon(burger/arrow) is clicked on.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.menu_sign_out) {
            new logoutTask().execute(
                    "http://plurry.cycorld.com:3000/mobile/users/sign_out",
                    "secret_token=" + token
            );
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
