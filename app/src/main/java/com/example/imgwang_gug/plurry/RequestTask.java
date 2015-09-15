package com.example.imgwang_gug.plurry;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

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

public class RequestTask extends AsyncTask<String, Void, String> {

    public String jsonConverter(String str) {
        str = str.replace("\\", "");
        str = str.replace("\"{", "{");
        str = str.replace("}\",", "},");
        str = str.replace("}\"", "}");

        return str;
    }

    @Override
    protected String doInBackground(String... params) {
        HttpURLConnection conn;

        URL url = null;
        int responseCode = 0;
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

            responseCode = conn.getResponseCode();
            Log.d("responseCode", responseCode + "");

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

        if (responseCode == HttpURLConnection.HTTP_OK) {
            return response;
        } else {
            return "fail";
        }
    }
}