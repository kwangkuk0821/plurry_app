package com.example.imgwang_gug.plurry;

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

public class SignUp extends AppCompatActivity {

    private static HttpURLConnection conn;
    private SharedPreferences pref;
    private final String prefName = "plurry";
    private EditText email;
    private EditText password;
    private EditText password_confirmation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        email = (EditText) findViewById(R.id.sign_up_email);
        password = (EditText) findViewById(R.id.sign_up_password);
        password_confirmation = (EditText) findViewById(R.id.sign_up_password_confirmation);
    }

    public void mOnClick(View v) {
        switch (v.getId()) {
            case R.id.sign_up_btn:
                new SignUpTask().execute(
                        "http://plurry.cycorld.com:3000/mobile/users/sign_up",
                        "email=" + email.getText().toString() + "&password=" + password.getText().toString() + "&password_confirmation=" + password_confirmation.getText().toString()
                );
                break;
            case R.id.link_to_login:
                Intent i = new Intent(SignUp.this, Login.class);
                startActivity(i);
                SignUp.this.finish();
                break;
        }
    }

    public class SignUpTask extends AsyncTask<String, Void, String> {

        ProgressDialog loginPending = new ProgressDialog(SignUp.this);

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
            loginPending.setMessage("회원가입 중 입니다...");

            loginPending.show();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            URL url = null;
            String urlParameters = null;
            String response = null;
            DataOutputStream os = null;
            InputStream is = null;
            BufferedReader br = null;
            try {
                url = new URL(params[0]);
                urlParameters = params[1];
                Log.d("parameters", urlParameters);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setReadTimeout(5000);
                conn.setConnectTimeout(5000);
                conn.setRequestProperty("charset", "euc-kr");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                os = new DataOutputStream(conn.getOutputStream());
                os.writeBytes(urlParameters);
                os.flush();

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {

                    is = conn.getInputStream();
                    br = new BufferedReader(new InputStreamReader(is));

                    response = new String(br.readLine());
                    response = jsonConverter(response);

                    JSONObject responseJSON = new JSONObject(response);

                    Log.i("response", "DATA response = " + responseJSON);
                    Log.i("response", "DATA response = " + responseJSON.get("result"));
                }
            } catch (MalformedURLException e) {
                Log.d("MalformedURLException", "ERROR " + e.getMessage());
            } catch (IOException e) {
                Log.d("IOException", "ERROR " + e.getMessage());
            } catch (JSONException e) {
                Log.d("JSONException", "ERROR " + e.getMessage());
            }
            return response;
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
                if (resultJSON.has("secret_token")) {
                    Toast.makeText(SignUp.this, "회원가입에 성공하였습니다.", Toast.LENGTH_SHORT).show();
                    secret_token = resultJSON.getString("secret_token");
                    savePreferences("secret_token", secret_token);
                    Intent i = new Intent(SignUp.this, MainActivity.class);
                    startActivity(i);
                    SignUp.this.finish();
                } else {
                    Toast.makeText(SignUp.this, "회원가입에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                }
                Log.d("task_result", "result = " + resultJSON);
            } catch (JSONException e) {
                Log.d("JSONException", "ERROR " + e.getMessage());
            }
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sign_up, menu);
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
