package com.example.imgwang_gug.plurry;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class Login extends AppCompatActivity {

    private EditText email;
    private EditText password;
    private TextView signup;
    private static HttpURLConnection conn;
    private SharedPref pref = new SharedPref();
    private final String loginUrl = "http://plurry.cycorld.com:3000/mobile/users/sign_in";
    private final String loginTokenUrl = "http://plurry.cycorld.com:3000/mobile/users/sign_in_token";
    private Activity this_activity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        email = (EditText) findViewById(R.id.sign_in_email);
        password = (EditText) findViewById(R.id.sign_in_password);
        signup = (TextView) findViewById(R.id.link_to_register);
        //자동 로그인(secret_token이 있고 일치시에)
        String token = pref.getPreferences("secret_token");
        Log.d("token", token);
        if(!token.isEmpty()) {
            new SignInTask().execute(
                    loginTokenUrl,
                    "secret_token=" + token
            );
        }
    }
    //클릭 리스너
    public void mOnClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_btn:
                new SignInTask().execute(
                        loginUrl,
                        "email=" + email.getText().toString() + "&password=" + password.getText().toString()
                );
                break;
            case R.id.link_to_register:
                signup.setClickable(false);
                Intent i = new Intent(this_activity, SignUp.class);
                startActivity(i);
                break;
        }
    }

    public class SignInTask extends RequestTask {

        ProgressDialog loginPending = new ProgressDialog(this_activity);

        public String jsonConverter(String str) {
            str = str.replace("\\", "");
            str = str.replace("\"{", "{");
            str = str.replace("}\",", "},");
            str = str.replace("}\"", "}");

            return str;
        }

        @Override
        protected void onPreExecute() {
            loginPending.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            loginPending.setMessage("로그인 중 입니다...");

            loginPending.show();
            super.onPreExecute();
        }

        protected void onPostExecute(String data) {
            loginPending.dismiss();
            // result is what you got from your connection
            JSONObject resultJSON = null;
            String result = null;
            String what = null;
            String secret_token = null;
            try {
                resultJSON = new JSONObject(data);
                result = resultJSON.getString("result");
                what = resultJSON.getString("what");
                if (what.equals("sign_in")) {
                    if (resultJSON.has("secret_token")) {
                        Toast.makeText(this_activity, "로그인에 성공하였습니다.", Toast.LENGTH_SHORT).show();
                        secret_token = resultJSON.getString("secret_token");
                        pref.savePreferences("secret_token", secret_token);
                        Intent i = new Intent(this_activity, GroupList.class);
                        startActivity(i);
                        this_activity.finish();
                    } else {
                        Toast.makeText(this_activity, "로그인에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                    }
                } else if(what.equals("sign_in_token")) {
                    if(result.equals("success")) {
                        Toast.makeText(this_activity, "로그인에 성공하였습니다.", Toast.LENGTH_SHORT).show();
                        Intent i = new Intent(this_activity, GroupList.class);
                        startActivity(i);
                        this_activity.finish();
                    }
                }
                Log.d("task_result", "result = " + resultJSON);
            } catch (JSONException e) {
                Log.d("JSONException", "ERROR " + e.getMessage());
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
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
