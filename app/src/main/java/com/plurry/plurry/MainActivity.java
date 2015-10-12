package com.plurry.plurry;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.plurry.plurry.websocket.WebSocketClient;
import com.plurry.plurry.joystick.JoystickView;
import com.plurry.plurry.joystick.JoystickView.OnJoystickMoveListener;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.opengl.GLSurfaceView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.plurry.plurry.skylink.*;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;
import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.MediaListener;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;
import sg.com.temasys.skylink.sdk.rtc.UserInfo;

public class MainActivity extends AppCompatActivity implements LifeCycleListener, MediaListener, RemotePeerListener {

    private static HttpURLConnection conn;
    private SeekBar feed_amount;
    private TextView feed_text;
    private WebSocketClient[] client;
    private JoystickView joystick;
    private LinearLayout feed_area;
    private LinearLayout joystick_area;
    private String token;
    private final int feed_product = 1;
    private final int move_product = 2;
    private final int phone_product = 3;
    private Activity this_activity = this;
    private String group;
    private SharedPreferences pref;
    private String prefName = "plurry";

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private ListView navList;

    //Skylink
    private static final String TAG = MainActivity.class.getCanonicalName();
    public static final String ROOM_NAME = Constants.ROOM_NAME_VIDEO;
    public static final String MY_USER_NAME = "videoCallUser";
    private static final String ARG_SECTION_NUMBER = "section_number";
    //set height width for self-video when in call
    public static final int WIDTH = 350;
    public static final int HEIGHT = 350;
    private LinearLayout parentFragment;
    private Button toggleAudioButton;
    private Button toggleVideoButton;
    private SkylinkConnection skylinkConnection;
    private String roomName;
    private String peerId;
    private ViewGroup.LayoutParams selfLayoutParams;
    private boolean audioMuted;
    private boolean videoMuted;
    private boolean connected;
    private AudioRouter audioRouter;

    //Variable End

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUi();
        setListeners();

        tokenCheck();
        makeNavigationDrawer();
    }

    private void initUi() {
        setContentView(R.layout.activity_main);

        feed_area = (LinearLayout) findViewById(R.id.feed_area);
        joystick_area = (LinearLayout) findViewById(R.id.joystick_area);
        feed_amount = (SeekBar) findViewById(R.id.feed_amount);
        feed_text = (TextView) findViewById(R.id.feed_text);
        joystick = (JoystickView) findViewById(R.id.joystickView);


        parentFragment = (LinearLayout) findViewById(R.id.ll_video_call);
        toggleAudioButton = (Button) findViewById(R.id.toggle_audio);
        toggleVideoButton = (Button) findViewById(R.id.toggle_video);

    }

    private void joinRoom(String phone, String session) {
        if (session.isEmpty()) return;

        String toast = "스마트폰 코드 [" + phone + "] 과 연결 중 입니다.";
        Toast.makeText(this_activity, toast, Toast.LENGTH_SHORT).show();

        String appKey = getString(R.string.app_key);
        String appSecret = getString(R.string.app_secret);

        // Initialize the skylink connection
        initializeSkylinkConnection();

        // Initialize the audio router
        initializeAudioRouter();

        // Obtaining the Skylink connection string done locally
        // In a production environment the connection string should be given
        // by an entity external to the App, such as an App server that holds the Skylink App secret
        // In order to avoid keeping the App secret within the application
        String skylinkConnectionString = Utils.
                getSkylinkConnectionString(session, appKey,
                        appSecret, new Date(), SkylinkConnection.DEFAULT_DURATION);

        skylinkConnection.connectToRoom(skylinkConnectionString,
                MY_USER_NAME);

        // Use the Audio router to switch between headphone and headset
        audioRouter.startAudioRouting(this_activity.getApplicationContext());
        connected = true;
    }

    private void setListeners() {
        toggleAudioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If audio is enabled, mute audio and if audio is enabled, mute it
                audioMuted = !audioMuted;
                if (audioMuted) {
                    toggleAudioButton.setText(getString(R.string.enable_audio));
                } else {
                    toggleAudioButton.setText(getString(R.string.mute_audio));
                }

                skylinkConnection.muteLocalAudio(audioMuted);
            }
        });

        toggleVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If video is enabled, mute video and if video is enabled, mute it
                videoMuted = !videoMuted;
                if (videoMuted) {
                    toggleVideoButton.setText(getString(R.string.enable_video));
                } else {
                    toggleVideoButton.setText(getString(R.string.mute_video));
                }

                skylinkConnection.muteLocalVideo(videoMuted);
            }
        });
    }

    private void initializeAudioRouter() {
        if (audioRouter == null) {
            audioRouter = AudioRouter.getInstance();
            audioRouter.init(((AudioManager) this_activity.
                    getSystemService(android.content.Context.AUDIO_SERVICE)));
        }
    }

    private void initializeSkylinkConnection() {
        if (skylinkConnection == null) {
            skylinkConnection = SkylinkConnection.getInstance();
            //the app_key and app_secret is obtained from the temasys developer console.
            skylinkConnection.init(getString(R.string.app_key),
                    getSkylinkConfig(), this.this_activity.getApplicationContext());
            //set listeners to receive callbacks when events are triggered
            skylinkConnection.setLifeCycleListener(this);
            skylinkConnection.setMediaListener(this);
            skylinkConnection.setRemotePeerListener(this);
        }
    }

    private SkylinkConfig getSkylinkConfig() {
        SkylinkConfig config = new SkylinkConfig();
        //AudioVideo config options can be NO_AUDIO_NO_VIDEO, AUDIO_ONLY, VIDEO_ONLY, AUDIO_AND_VIDEO;
        config.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        config.setAudioVideoReceiveConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        config.setHasPeerMessaging(true);
        config.setHasFileTransfer(true);
        config.setTimeout(Constants.TIME_OUT);
        config.setMirrorLocalView(true);
        return config;
    }

    @Override
    public void onStop() {
        //close the connection when the fragment is detached, so the streams are not open.
        super.onStop();
        for (int i = 0; i < client.length; i++) {
            if (client[i] != null) client[i].disconnect();
        }
        if (skylinkConnection != null && connected) {
            skylinkConnection.disconnectFromRoom();
            skylinkConnection.setLifeCycleListener(null);
            skylinkConnection.setMediaListener(null);
            skylinkConnection.setRemotePeerListener(null);
            connected = false;
            audioRouter.stopAudioRouting(this_activity.getApplicationContext());
        }
    }

    /***
     * Lifecycle Listener Callbacks -- triggered during events that happen during the SDK's lifecycle
     */

    /**
     * Triggered when connection is successful
     *
     * @param isSuccess
     * @param message
     */

    @Override
    public void onConnect(boolean isSuccess, String message) {
        if (isSuccess) {
            toggleAudioButton.setVisibility(View.VISIBLE);
            toggleVideoButton.setVisibility(View.VISIBLE);
        } else {
            Log.e(TAG, "Skylink Failed " + message);
        }
    }

    @Override
    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
        //Toast.makeText(this_activity, "Peer " + remotePeerId +
        //       " has changed Room locked status to " + lockStatus, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onWarning(int errorCode, String message) {
        Log.d(TAG, message + "warning");
        //Toast.makeText(this_activity, "Warning is errorCode" + errorCode, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisconnect(int errorCode, String message) {
        Log.d(TAG, message + " disconnected");
        //Toast.makeText(this_activity, "onDisconnect " + message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onReceiveLog(String message) {
        Log.d(TAG, message + " on receive log");
    }

    /**
     * Media Listeners Callbacks - triggered when receiving changes to Media Stream from the remote peer
     */

    /**
     * Triggered after the user's local media is captured.
     *
     * @param videoView
     */
    @Override
    public void onLocalMediaCapture(GLSurfaceView videoView) {
        if (videoView != null) {
            View self = parentFragment.findViewWithTag("self");
            videoView.setTag("self");
            videoView.setVisibility(View.GONE);
            // Allow self view to switch between different cameras (if any) when tapped.
            videoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    skylinkConnection.switchCamera();
                }
            });

            if (self == null) {
                //show media on screen
                parentFragment.removeView(videoView);
                parentFragment.addView(videoView);
            } else {
                videoView.setLayoutParams(self.getLayoutParams());

                // If peer video exists, remove it first.
                View peer = parentFragment.findViewWithTag("peer");
                if (peer != null) {
                    parentFragment.removeView(peer);
                }

                // Remove the old self video and add the new one.
                parentFragment.removeView(self);
                parentFragment.addView(videoView);

                // Return the peer video, if it was there before.
                if (peer != null) {
                    parentFragment.addView(peer);
                }
            }

        }
    }

    @Override
    public void onVideoSizeChange(String peerId, Point size) {
        Log.d(TAG, "PeerId: " + peerId + " got size " + size.toString());
    }

    @Override
    public void onRemotePeerAudioToggle(String remotePeerId, boolean isMuted) {
        String message = null;
        if (isMuted) {
            message = "Your peer muted their audio";
        } else {
            message = "Your peer unmuted their audio";
        }

        //Toast.makeText(this_activity, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRemotePeerVideoToggle(String peerId, boolean isMuted) {
        String message = null;
        if (isMuted)
            message = "Your peer muted video";
        else
            message = "Your peer unmuted their video";

        //Toast.makeText(this_activity, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Remote Peer Listener Callbacks - triggered during events that happen when data or connection
     * with remote peer changes
     */

    @Override
    public void onRemotePeerJoin(String remotePeerId, Object userData, boolean hasDataChannel) {
        Toast.makeText(this_activity, "상대방과 연결되었습니다.", Toast.LENGTH_SHORT).show();
        UserInfo remotePeerUserInfo = skylinkConnection.getUserInfo(remotePeerId);
        Log.d(TAG, "isAudioStereo " + remotePeerUserInfo.isAudioStereo());
        Log.d(TAG, "video height " + remotePeerUserInfo.getVideoHeight());
        Log.d(TAG, "video width " + remotePeerUserInfo.getVideoHeight());
        Log.d(TAG, "video frameRate " + remotePeerUserInfo.getVideoFps());
    }

    @Override
    public void onRemotePeerMediaReceive(String remotePeerId, GLSurfaceView videoView) {
        if (videoView == null) {
            return;
        }

        if (!TextUtils.isEmpty(this.peerId) && !remotePeerId.equals(this.peerId)) {
            Toast.makeText(this_activity, "이미 상대방과 연결되어 있는 상태입니다.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Resize self view
        View self = parentFragment.findViewWithTag("self");
        if (this.selfLayoutParams == null) {
            // Record the original size of the layout
            this.selfLayoutParams = self.getLayoutParams();
        }

        self.setLayoutParams(new ViewGroup.LayoutParams(WIDTH, HEIGHT));
        parentFragment.removeView(self);
        parentFragment.addView(self);

        // Remove previous peer video if it exist
        View viewToRemove = parentFragment.findViewWithTag("peer");
        if (viewToRemove != null) {
            parentFragment.removeView(viewToRemove);
        }

        // Add new peer video
        videoView.setTag("peer");
        parentFragment.addView(videoView);

        this.peerId = remotePeerId;
    }

    @Override
    public void onRemotePeerLeave(String remotePeerId, String message) {
        Toast.makeText(this_activity, "상대방과의 연결이 끊어졌습니다.", Toast.LENGTH_SHORT).show();
        if (remotePeerId != null && remotePeerId.equals(this.peerId)) {
            this.peerId = null;
            View peerView = parentFragment.findViewWithTag("peer");
            parentFragment.removeView(peerView);

            // Resize self view to original size
            if (this.selfLayoutParams != null) {
                View self = parentFragment.findViewWithTag("self");
                self.setLayoutParams(selfLayoutParams);
            }
        }
    }

    @Override
    public void onRemotePeerUserDataReceive(String remotePeerId, Object userData) {
        Log.d(TAG, "onRemotePeerUserDataReceive " + remotePeerId);
    }

    @Override
    public void onOpenDataConnection(String peerId) {
        Log.d(TAG, "onOpenDataConnection");
    }

    //Skylink Code end!

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
                R.string.close_drawer) {
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
        final String[] navItems = {"control", "schedule", "new product"};
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
                    cmd.put("amount", progress + 1);
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
        if (type == phone_product) {
            client[type] = new WebSocketClient(URI.create("ws://plurry.cycorld.com:3000/ws/" + product_id), new WebSocketClient.Listener() {
                @Override
                public void onConnect() {
                    Log.d("Connect", "Connected!");
                    client[phone_product].send("remote on");
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
        } else {
            client[type] = new WebSocketClient(URI.create("ws://plurry.cycorld.com:3000/ws/" + product_id), new WebSocketClient.Listener() {
                @Override
                public void onConnect() {
                    Log.d("Connect", "Connected!");
                    Log.d("Connect2", client.toString());
                }

                @Override
                public void onMessage(String message) {
                    Log.e("Message", String.format("Got string message! %s", message));
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
            if (!data.equals("fail")) {
                JSONObject resultJSON = null;
                String result = null;
                String what = null;
                JSONArray products = null;
                try {
                    resultJSON = new JSONObject(data);
                    products = resultJSON.getJSONArray("data");
                    client = new WebSocketClient[4];
                    for (int i = 0; i < products.length(); i++) {
                        JSONObject product = (JSONObject) products.get(i);
                        websocket(product.getString("product_id"), product.getInt("product_type"));
                        if (product.getInt("product_type") == 1) {
                            Intent intent = new Intent(this_activity, websocketService.class);
                            intent.putExtra("product_id", product.getString("product_id"));
                            startService(intent);
                        }
                        if (product.getInt("product_type") == 3) {
                            String phone = product.getString("code");
                            String session = product.getString("owr_session_id");
                            joinRoom(phone, session);
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
            if (!data.equals("fail")) {
                JSONObject resultJSON = null;
                String result = null;
                String what = null;
                try {
                    resultJSON = new JSONObject(data);
                    result = resultJSON.getString("result");
                    what = resultJSON.getString("what");
                    if (result.equals("success") && what.equals("logout")) {
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
        if (client[feed_product] == null) {
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
        if (client[move_product] == null) {
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
                        int maxX = (int) (width * ((float) 3 / 8));
                        int maxY = (int) (height * ((float) 3 / 8));

                        Log.d("angle", "angle : " + angle);
                        int left_speed;
                        int right_speed;
                        if (angle == 0) {
                            left_speed = 0;
                            right_speed = 0;
                        } else if (angle <= 60 && angle > -60) {
                            int plusX = (int) Math.abs(((maxVelocity - defaultVelocity) * (((x / maxX) * 100) / 100))) * -1;
                            int plusY = (int) Math.abs(((maxVelocity - defaultVelocity) * (((y / maxY) * 100) / 100)));
                            if (x > 0) {
                                left_speed = defaultVelocity + plusY;
                                right_speed = defaultVelocity + plusY + plusX;
                            } else {
                                left_speed = defaultVelocity + plusY + plusX;
                                right_speed = defaultVelocity + plusY;
                            }
                        } else if (angle > 120 || angle <= -120) {
                            int plusX = (int) Math.abs(((maxVelocity - defaultVelocity) * (((x / maxX) * 100) / 100)));
                            int plusY = (int) Math.abs(((maxVelocity - defaultVelocity) * (((y / maxY) * 100) / 100))) * -1;
                            Log.d("plusXPlusY", "plusX : " + plusX + " plusY : " + plusY);
                            if (x > 0) {
                                left_speed = (defaultVelocity * -1) + plusY;
                                right_speed = (defaultVelocity * -1) + plusY + plusX;
                            } else {
                                left_speed = (defaultVelocity * -1) + plusY + plusX;
                                right_speed = (defaultVelocity * -1) + plusY;
                            }

                        } else if (angle > 60 && angle <= 120) {
                            if (x > (maxX / 2)) {
                                left_speed = 255;
                                right_speed = -255;
                            } else {
                                left_speed = 0;
                                right_speed = 0;
                            }
                        } else if (angle <= -60 && angle > -120) {
                            if (Math.abs(x) > (maxX / 2)) {
                                left_speed = -255;
                                right_speed = 255;
                            } else {
                                left_speed = 0;
                                right_speed = 0;
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
    protected void onRestart() {
        for (int i = 0; i < client.length; i++) {
            if (client[i] != null) client[i].connect();
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
