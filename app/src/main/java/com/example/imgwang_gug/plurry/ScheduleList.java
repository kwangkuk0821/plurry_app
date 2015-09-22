package com.example.imgwang_gug.plurry;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.example.imgwang_gug.plurry.Schedule.*;
import com.example.imgwang_gug.plurry.websocket.WebSocketClient;

public class ScheduleList extends AppCompatActivity {

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private ListView navList;
    private Activity this_activity = this;
    private WebSocketClient client;
    private WebSocketClient rs_client;

    private String group;
    private String token;
    private SharedPreferences pref;
    private String prefName = "plurry";
    private String product_id;

    //test
    private ArrayList<HashMap> list;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_list);

        Bundle b = getIntent().getExtras();
        //group = b.getString("group");
        group = "myrobot";

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

        //메뉴 툴바
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
    @Override
    protected  void onPause() {
        client.disconnect();
        rs_client.disconnect();
        super.onPause();
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent i;

            switch (position) {
                case 0:
                    i = new Intent(this_activity ,MainActivity.class);
                    i.putExtra("group", group);
                    startActivity(i);
                    this_activity.finish();
                    break;
                case 1:
                    break;
                case 2:
                    i = new Intent(this_activity ,AddProducts.class);
                    i.putExtra("group", group);
                    startActivity(i);
                    this_activity.finish();
                    break;
            }
        }
    }

    private class listLongClick implements ListView.OnItemLongClickListener {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            final int mypos = position;
            Toast.makeText(this_activity, "click " + position, Toast.LENGTH_SHORT).show();
            AlertDialog.Builder alert = new AlertDialog.Builder(this_activity);
            alert.setTitle("title");
            alert.setCancelable(false);
            alert.setMessage("안녕하세요");
            alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    Log.d("test", "result = " + list);
                    try {
                        HashMap h = list.get(mypos);
                        JSONObject cmd = new JSONObject();
                        int nid = Integer.parseInt(h.get("nid").toString());
                        String time = "empty";
                        int amount = 0;
                        cmd.put("cmd", 4);
                        cmd.put("id", nid);
                        cmd.put("timestamp", 0);
                        cmd.put("amount", 0);
                        client.send(cmd.toString());
                        new productTask().execute(
                                "http://plurry.cycorld.com:3000/mobile/" + product_id + "/schedule/update",
                                "secret_token=" + token + "&nid=" + nid + "&time=" + time + "&amount=" + amount
                        );
                    } catch (JSONException e) {
                        Log.e("MYAPP", "unexpected JSON exception", e);
                    }
                }
            });

            alert.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                }
            });
            alert.show();

            return false;
        }
    }

    public void mOnClick(View v) {
        switch (v.getId()) {
            case R.id.new_schedule_btn:
                this_activity.finish();
                Intent i = new Intent(this_activity, CreateSchedule.class);
                i.putExtra("product", product_id);
                i.putExtra("group", group);
                startActivity(i);
                break;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // This to ensure the navigation icon is displayed as
        // burger instead of arrow.
        // Call syncState() from your Activity's onPostCreate
        // to synchronize the indicator with the state of the
        // linked DrawerLayout after onRestoreInstanceState
        // has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // This method should always be called by your Activity's
        // onConfigurationChanged method.
        mDrawerToggle.onConfigurationChanged(newConfig);
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
                    if(what.equals("products")) {
                        JSONArray products = resultJSON.getJSONArray("data");
                        for(int i = 0; i < products.length();i++) {
                            JSONObject product = (JSONObject) products.get(i);
                            if(product.get("product_type").equals(1)) {
                                JSONArray schedule = product.getJSONArray("schedule");
                                ListView lview = (ListView) findViewById(R.id.schedule_list);
                                InputList(schedule);
                                ScheduleAdapter adapter = new ScheduleAdapter(this_activity, list);
                                lview.setAdapter(adapter);
                                lview.setOnItemLongClickListener(new listLongClick());
                                product_id = product.getString("product_id");
                                openWebsocket(product_id);
                                break;
                            }
                        }
                    } else if(what.equals("schedule update")) {
                        finish();
                        startActivity(getIntent());
                    }
                } catch (JSONException e) {
                    Log.d("JSONException", "ERROR " + e.getMessage());
                }
            } else {
                Toast.makeText(this_activity, "데이터를 불러오기를 실패하였습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void InputList(JSONArray schedule) {
        list = new ArrayList<HashMap>();
        try {
            for(int i = 0; i < schedule.length(); i++) {
                JSONObject s = (JSONObject) schedule.get(i);
                String time = s.getString("time");
                if(time.equals("empty")) continue;
                int amount = s.getInt("amount");
                int id = s.getInt("id");
                HashMap h = new HashMap();
                h.put(TIME_COLUMN, time);
                h.put(AMOUNT_COLUMN, amount);
                h.put("nid", id);
                list.add(h);
            }
        } catch (JSONException e) {
            Log.d("JSONException", "ERROR " + e.getMessage());
        }
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
                        finish();
                        startActivity(getIntent());
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
            logoutPending.setMessage("로그아웃 중 입니다...");

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_schedule, menu);
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
