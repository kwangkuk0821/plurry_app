package com.plurry.plurry;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.plurry.plurry.websocket.WebSocketClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.CancellationException;

/**
 * Created by imgwang-gug on 2015. 10. 8..
 */
public class websocketService extends Service {

    private SharedPreferences pref;
    private String prefName = "session";
    private WebSocketClient client = null;
    private WebSocketClient cmd_client = null;
    private String product_id = null;
    private int feedNotificationNumber;
    private int batteryNotificationNumber;
    private int currentBattery;
    private int remainNotificationNumber;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        if (product_id == null) return;
        feedNotificationNumber = (int) ((Math.random() * 3000) + 1);
        batteryNotificationNumber = (int) ((Math.random() * 3000) + 3001);
        remainNotificationNumber = (int) ((Math.random() * 3000) + 6001);
        currentBattery = -1;

        cmd_client = new WebSocketClient(URI.create("ws://plurry.cycorld.com:3000/ws/" + product_id), new WebSocketClient.Listener() {
            @Override
            public void onConnect() {
                Log.d("Connect", "Connected!");
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

        cmd_client.connect();

        client = new WebSocketClient(URI.create("ws://plurry.cycorld.com:3000/ws/debug/" + product_id), new WebSocketClient.Listener() {
            @Override
            public void onConnect() {
                Log.d("Connect", "Connected!");
            }

            @Override
            public void onMessage(String message) {
                Log.e("Message", String.format("Got string message! %s", message));
                try {
                    JSONObject msg = new JSONObject(message);
                    if (msg.has("rs")) {
                        int rs = msg.getInt("rs");
                        switch (rs) {
                            case 107:
                                int amount = msg.getInt("amount");
                                String remain_notice;
                                if (amount == 3) {
                                    remain_notice = "사료가 충분히 남아있습니다.";
                                } else if (amount == 0) {
                                    remain_notice = "밥통에 사료가 없습니다.";
                                } else {
                                    remain_notice = "밥통에 사료가 " + amount + " / 3 가량 남았습니다.";
                                }
                                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                                Notification.Builder mBuilder = new Notification.Builder(getApplicationContext());

                                mBuilder.setSmallIcon(R.drawable.remain_icon);
                                mBuilder.setTicker("사료 알림(" + product_id + ")");
                                mBuilder.setWhen(System.currentTimeMillis());
                                mBuilder.setContentTitle("사료 알림(" + product_id + ")");
                                mBuilder.setContentText(remain_notice);
                                mBuilder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
                                mBuilder.setAutoCancel(true);
                                nm.cancel(remainNotificationNumber);
                                nm.notify(remainNotificationNumber, mBuilder.build());
                                break;
                        }
                    } else if (msg.has("report")) {
                        int report = msg.getInt("report");
                        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        Notification.Builder mBuilder = new Notification.Builder(getApplicationContext());
                        switch (report) {
                            case 201:
                                int feed_amount = msg.getInt("amount");
                                long timestamp = msg.getLong("timestamp") - 32400;
                                Timestamp stamp = new Timestamp(timestamp);

                                Date date = new Date(timestamp * 1000L); // *1000 is to convert seconds to milliseconds
                                SimpleDateFormat sdf = new SimpleDateFormat("dd 일 HH 시 mm 분에 " + feed_amount + "(최소 1 ~ 최대 9)만큼 밥을 주었습니다."); // the format of your date
                                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul")); // give a timezone reference for formating (see comment at the bottom
                                String formattedDate = sdf.format(date);
                                String feed_notice = formattedDate;

                                mBuilder.setSmallIcon(R.drawable.feed_icon);
                                mBuilder.setTicker("밥을 주었습니다");
                                mBuilder.setWhen(System.currentTimeMillis());
                                mBuilder.setContentTitle("밥 알림(" + product_id + ")");
                                mBuilder.setContentText(feed_notice);
                                mBuilder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
                                mBuilder.setAutoCancel(true);
                                nm.cancel(feedNotificationNumber);
                                nm.notify(feedNotificationNumber, mBuilder.build());

                                JSONObject cmd = new JSONObject();
                                cmd.put("cmd", 7);
                                cmd_client.send(cmd.toString());
                                break;
                            case 202:
                                int battery_amount = msg.getInt("amount");
                                if (battery_amount != currentBattery || currentBattery == -1) {
                                    currentBattery = battery_amount;
                                    String battery_notice;
                                    int notice_icon = R.drawable.batteryfull;
                                    switch (battery_amount) {
                                        case 0:
                                            notice_icon = R.drawable.batterymin;
                                            break;
                                        case 1:
                                            notice_icon = R.drawable.batterymid;
                                            break;
                                        case 2:
                                            notice_icon = R.drawable.batteryfull;
                                            break;
                                    }
                                    if (battery_amount == 0)
                                        battery_notice = "밥통의 배터리가 거의 남지 않았습니다.";
                                    else
                                        battery_notice = "밥통의 배터리가 " + (battery_amount + 1) + " / 3 만큼 남았습니다.";
                                    mBuilder.setSmallIcon(notice_icon);
                                    mBuilder.setTicker(battery_notice);
                                    mBuilder.setWhen(System.currentTimeMillis());
                                    mBuilder.setContentTitle("배터리 알림(" + product_id + ")");
                                    mBuilder.setContentText(battery_notice);
                                    mBuilder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
                                    mBuilder.setAutoCancel(true);
                                    nm.cancel(batteryNotificationNumber);
                                    nm.notify(batteryNotificationNumber, mBuilder.build());
                                }
                                break;
                        }
                    }
                } catch (JSONException e) {
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

        }, null);

        client.connect();
        startForeground(1, new Notification());
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.e("WebsocketService", "onDestory");
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        client.disconnect();
        client = null;
        cmd_client.disconnect();
        cmd_client = null;
        nm.cancel(batteryNotificationNumber);
        nm.cancel(feedNotificationNumber);
        nm.cancel(remainNotificationNumber);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("WebsocketService", "onStartCommand");
        Bundle extras = intent.getExtras();
        if (extras != null) product_id = extras.getString("product_id");
        if (client == null) onCreate();
        return START_STICKY;
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
