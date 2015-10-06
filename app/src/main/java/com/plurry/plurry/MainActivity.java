package com.plurry.plurry;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import com.ericsson.research.owr.Owr;
import com.ericsson.research.owr.sdk.InvalidDescriptionException;
import com.ericsson.research.owr.sdk.RtcCandidate;
import com.ericsson.research.owr.sdk.RtcCandidates;
import com.ericsson.research.owr.sdk.RtcConfig;
import com.ericsson.research.owr.sdk.RtcConfigs;
import com.ericsson.research.owr.sdk.RtcSession;
import com.ericsson.research.owr.sdk.RtcSessions;
import com.ericsson.research.owr.sdk.SessionDescription;
import com.ericsson.research.owr.sdk.SessionDescriptions;
import com.ericsson.research.owr.sdk.SimpleStreamSet;
import com.plurry.plurry.openwebrtc.Config;
import com.plurry.plurry.openwebrtc.SignalingChannel;
import com.plurry.plurry.websocket.WebSocketClient;
import com.plurry.plurry.joystick.JoystickView;
import com.plurry.plurry.joystick.JoystickView.OnJoystickMoveListener;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.ericsson.research.owr.sdk.VideoView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements
        SignalingChannel.JoinListener,
        SignalingChannel.DisconnectListener,
        SignalingChannel.SessionFullListener,
        SignalingChannel.MessageListener,
        SignalingChannel.PeerDisconnectListener,
        RtcSession.OnLocalCandidateListener,
        RtcSession.OnLocalDescriptionListener {

    //openwebrtc

    private static final String TAG = "NativeCall";

    private static final String PREFERENCE_KEY_SERVER_URL = "url";
    private static final int SETTINGS_ANIMATION_DURATION = 400;
    private static final int SETTINGS_ANIMATION_ANGLE = 90;

    static {
        Log.d(TAG, "Initializing OpenWebRTC");
        Owr.init();
        Owr.runInBackground();
    }

    private Button mJoinButton;
    private Button mCallButton;
    private EditText mSessionInput;

    private SignalingChannel mSignalingChannel;
    private InputMethodManager mInputMethodManager;
    private WindowManager mWindowManager;
    private SignalingChannel.PeerChannel mPeerChannel;
    private RtcSession mRtcSession;
    private SimpleStreamSet mStreamSet;
    private VideoView mRemoteView;
    private RtcConfig mRtcConfig;

    private static HttpURLConnection conn;
    private SeekBar feed_amount;
    private TextView feed_text;
    private WebSocketClient[] client;
    private JoystickView joystick;
    private LinearLayout feed_area;
    private LinearLayout joystick_area;
    private LinearLayout video_area;
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
    private ProgressDialog videopending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //webrtc
        initUi();

        mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mRtcConfig = RtcConfigs.defaultConfig(Config.STUN_SERVER);

        tokenCheck();
        makeNavigationDrawer();
    }

    private void updateVideoView(boolean running) {
        if (mStreamSet != null) {
            TextureView remoteView = (TextureView) findViewById(R.id.remote_view);
            remoteView.setVisibility(running ? View.VISIBLE : View.INVISIBLE);
            if (running) {
                mRemoteView.setView(remoteView);
                //mStreamSet.setDeviceOrientation(mWindowManager.getDefaultDisplay().getRotation());
                Log.d(TAG, "" + mWindowManager.getDefaultDisplay().getRotation());
            } else {
                mRemoteView.stop();
            }
        }
    }

    public void initUi() {
        setContentView(R.layout.activity_main);

        mCallButton = (Button) findViewById(R.id.call);
        mJoinButton = (Button) findViewById(R.id.join);
        mSessionInput = (EditText) findViewById(R.id.session_id);
        mJoinButton.setEnabled(true);

        feed_area = (LinearLayout) findViewById(R.id.feed_area);
        joystick_area = (LinearLayout) findViewById(R.id.joystick_area);
        feed_amount = (SeekBar) findViewById(R.id.feed_amount);
        feed_text = (TextView) findViewById(R.id.feed_text);
        joystick = (JoystickView) findViewById(R.id.joystickView);
        video_area = (LinearLayout) findViewById(R.id.videoLayout);

    }

    public void onJoinClicked(final View view) {
        Log.d(TAG, "onJoinClicked");

        String sessionId = mSessionInput.getText().toString();
        if (sessionId.isEmpty()) {
            mSessionInput.requestFocus();
            mInputMethodManager.showSoftInput(mSessionInput, InputMethodManager.SHOW_IMPLICIT);
            return;
        }

        mInputMethodManager.hideSoftInputFromWindow(mSessionInput.getWindowToken(), 0);
        mSessionInput.setEnabled(false);
        mJoinButton.setEnabled(false);

        mSignalingChannel = new SignalingChannel(getUrl(), sessionId);
        mSignalingChannel.setJoinListener(this);
        mSignalingChannel.setDisconnectListener(this);
        mSignalingChannel.setSessionFullListener(this);

        mStreamSet = SimpleStreamSet.defaultConfig(true, true);
        mRemoteView = mStreamSet.createRemoteView();
        mRemoteView.setRotation(0);
        updateVideoView(true);
    }

    @Override
    public void onPeerJoin(final SignalingChannel.PeerChannel peerChannel) {
        Log.v(TAG, "onPeerJoin => " + peerChannel.getPeerId());
        mCallButton.setEnabled(true);
        mPeerChannel = peerChannel;
        mPeerChannel.setDisconnectListener(this);
        mPeerChannel.setMessageListener(this);

        mRtcSession = RtcSessions.create(mRtcConfig);
        mRtcSession.setOnLocalCandidateListener(this);
        mRtcSession.setOnLocalDescriptionListener(this);


        AlertDialog.Builder alert = new AlertDialog.Builder(this_activity);
        alert.setTitle("알림(Alert)!");
        alert.setCancelable(false);
        alert.setMessage("화상통화를 연결하시겠습니까?");
        alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                mCallButton.performClick();
            }
        });

        alert.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
            }
        });
        alert.show();
    }

    @Override
    public void onPeerDisconnect(final SignalingChannel.PeerChannel peerChannel) {
        Log.d(TAG, "onPeerDisconnect => " + peerChannel.getPeerId());
        mRtcSession.stop();
        mPeerChannel = null;
        updateVideoView(false);
        mSessionInput.setEnabled(true);
        mJoinButton.setEnabled(true);
        mCallButton.setEnabled(false);
        video_area.setBackgroundColor(Color.WHITE);
    }

    @Override
    public synchronized void onMessage(final JSONObject json) {
        if (json.has("candidate")) {
            JSONObject candidate = json.optJSONObject("candidate");
            Log.v(TAG, "candidate: " + candidate);
            RtcCandidate rtcCandidate = RtcCandidates.fromJsep(candidate);
            if (rtcCandidate != null) {
                mRtcSession.addRemoteCandidate(rtcCandidate);
            } else {
                Log.w(TAG, "invalid candidate: " + candidate);
            }
        }
        if (json.has("sdp")) {
            JSONObject sdp = json.optJSONObject("sdp");
            Log.v(TAG, "sdp: " + sdp);
            try {
                SessionDescription sessionDescription = SessionDescriptions.fromJsep(sdp);
                if (sessionDescription.getType() == SessionDescription.Type.OFFER) {
                    onInboundCall(sessionDescription);
                } else {
                    onAnswer(sessionDescription);
                }
            } catch (InvalidDescriptionException e) {
                e.printStackTrace();
            }
        }
        if (json.has("orientation")) {
//                handleOrientation(json.getInt("orientation"));
        }
    }

    @Override
    public void onLocalCandidate(final RtcCandidate candidate) {
        if (mPeerChannel != null) {
            try {
                JSONObject json = new JSONObject();
                json.putOpt("candidate", RtcCandidates.toJsep(candidate));
                json.getJSONObject("candidate").put("sdpMid", "video");
                Log.d(TAG, "sending candidate: " + json);
                mPeerChannel.send(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void onCallClicked(final View view) {
        Log.d(TAG, "onCallClicked");

        videopending = new ProgressDialog(this_activity);
        videopending.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        videopending.setMessage("영상을 불러오는 중 입니다...");

        videopending.show();

        mRtcSession.start(mStreamSet);
        mCallButton.setEnabled(false);
    }

    private void onInboundCall(final SessionDescription sessionDescription) {
        try {
            mRtcSession.setRemoteDescription(sessionDescription);
            mRtcSession.start(mStreamSet);
        } catch (InvalidDescriptionException e) {
            e.printStackTrace();
        }
    }

    private void onAnswer(final SessionDescription sessionDescription) {
        videopending.dismiss();
        video_area.setBackgroundColor(Color.BLACK);
        if (mRtcSession != null) {
            try {
                updateVideoView(true);
                mRtcSession.setRemoteDescription(sessionDescription);
            } catch (InvalidDescriptionException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onLocalDescription(final SessionDescription localDescription) {
        if (mPeerChannel != null) {
            try {
                JSONObject json = new JSONObject();
                json.putOpt("sdp", SessionDescriptions.toJsep(localDescription));
                Log.d(TAG, "sending sdp: " + json);
                mPeerChannel.send(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDisconnect() {
        Toast.makeText(this, "예기치못한 오류가 발생하였습니다. 양쪽 앱을 재시작시켜 주세요.", Toast.LENGTH_LONG).show();
        updateVideoView(false);
        try {
            mStreamSet = null;
            mRtcSession.stop();
            mRtcSession = null;
            mSignalingChannel = null;
        } catch (NullPointerException e) {
        }
    }

    @Override
    public void onSessionFull() {
        Toast.makeText(this, "Session is full", Toast.LENGTH_LONG).show();
        mJoinButton.setEnabled(true);
    }

    private String getUrl() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getString(PREFERENCE_KEY_SERVER_URL, Config.DEFAULT_SERVER_ADDRESS);
    }

    private void tokenCheck() {
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
    }

    private void makeNavigationDrawer() {
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
        initUi();
        updateVideoView(true);
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

        }, null);

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
                    client = new WebSocketClient[3];
                    for(int i = 0; i < products.length();i++) {
                        JSONObject product = (JSONObject) products.get(i);
                        if(product.getInt("product_type") == 1 || product.getInt("product_type") == 2) {
                            websocket(product.getString("product_id"), product.getInt("product_type"));
                        } else if(product.getInt("product_type") == 3) {
                            mSessionInput.setText(product.getString("owr_session_id"));
                            mJoinButton.performClick();
                        }
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
                int defaultVelocity = 180;
                int maxVelocity = 255;
                @Override
                public void onValueChanged(int angle, int power, int direction, float x, float y, float width, float height) {
                    try {
                        JSONObject left = new JSONObject();
                        JSONObject right = new JSONObject();
                        left.put("cmd", 8);
                        right.put("cmd", 9);
                        int maxX = (int)(width * ((float)3/8));
                        int maxY = (int)(height * ((float)3/8));

                        Log.d("angle", "angle : " + angle);
                        int left_speed;
                        int right_speed;
                        if(angle == 0) {
                            left_speed = 0;
                            right_speed = 0;
                        } else if(angle <= 60 && angle > -60) {
                            int plusX = (int) Math.abs(((maxVelocity - defaultVelocity) * (((x / maxX) * 100) / 100))) * -1;
                            int plusY = (int) Math.abs(((maxVelocity - defaultVelocity) * (((y / maxY) * 100) / 100)));
                            if(x > 0) {
                                left_speed = defaultVelocity + plusY;
                                right_speed = defaultVelocity + plusY + plusX;
                            } else {
                                left_speed = defaultVelocity + plusY + plusX;
                                right_speed = defaultVelocity + plusY;
                            }
                        } else if(angle > 120 || angle <= -120) {
                            int plusX = (int) Math.abs(((maxVelocity - defaultVelocity) * (((x / maxX) * 100) / 100)));
                            int plusY = (int) Math.abs(((maxVelocity - defaultVelocity) * (((y / maxY) * 100) / 100))) * -1;
                            Log.d("plusXPlusY", "plusX : " + plusX + " plusY : " + plusY);
                            if(x > 0) {
                                left_speed = (defaultVelocity * -1) + plusY;
                                right_speed = (defaultVelocity * -1) + plusY + plusX;
                            } else {
                                left_speed = (defaultVelocity * -1) + plusY + plusX;
                                right_speed = (defaultVelocity * -1) + plusY;
                            }

                        } else if(angle > 60 && angle <= 120) {
                            if(x > (maxX / 2)) {
                                left_speed = 255;
                                right_speed = -255;
                            } else {
                                left_speed = 0;
                                right_speed = 0;
                            }
                        } else if(angle <= -60 && angle > -120) {
                            if(Math.abs(x) > (maxX / 2)) {
                                left_speed = -255;
                                right_speed = 255;
                            } else {
                                left_speed = 0;
                                right_speed =0;
                            }
                        } else {
                            left_speed = 0;
                            right_speed = 0;
                        }
                        Log.e("speed", "left = " + left_speed);
                        Log.e("speed", "right = " + right_speed);
                        left.put("speed", left_speed);
                        right.put("speed", right_speed);
                        client[move_product].send(left.toString());
                        client[move_product].send(right.toString());
                    } catch (JSONException e) {
                        Log.e("MYAPP", "unexpected JSON exception", e);
                    }
                }
            }, JoystickView.DEFAULT_LOOP_INTERVAL);
        }
    }
    @Override
    protected void onPause() {
        for (int i = 0; i < client.length; i++) {
            if (client[i] != null) client[i].disconnect();
        }
        updateVideoView(false);
        super.onPause();
    }

    @Override
    protected void onRestart() {
        for(int i = 0; i < client.length;i++) {
            if(client[i] != null) client[i].connect();
        }
        super.onRestart();
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
