package com.example.imgwang_gug.plurry;

import android.app.Activity;
import android.app.ProgressDialog;
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

import com.example.imgwang_gug.plurry.MainActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Scanner;

public class AddProducts extends AppCompatActivity {

    private String group;
    private SharedPref pref = new SharedPref();

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private ListView navList;
    private Activity this_activity = this;
    private String token;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_products);

        Bundle b = getIntent().getExtras();
        group = b.getString("group");

        token = pref.getPreferences("secret_token");
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

        //툴바
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
            // result is what you got from your connection
            if(!data.equals("fail")) {
                JSONObject resultJSON = null;
                String result = null;
                String what = null;
                JSONArray products = null;
                try {
                    resultJSON = new JSONObject(data);
                    Log.d("task_result", "result = " + resultJSON);
                } catch (JSONException e) {
                    Log.d("JSONException", "ERROR " + e.getMessage());
                }
            } else {
                Toast.makeText(this_activity, "데이터를 불러오기를 실패하였습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent i;

            switch (position) {
                case 0:
                    i = new Intent(this_activity, MainActivity.class);
                    i.putExtra("group", group);
                    startActivity(i);
                    this_activity.finish();
                    break;
                case 1:
                    i = new Intent(this_activity, Schedule.class);
                    i.putExtra("group", group);
                    startActivity(i);
                    this_activity.finish();
                    break;
                case 2:
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
}
