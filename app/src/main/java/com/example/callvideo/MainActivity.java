package com.example.callvideo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.callvideo.model.LoginResponse;
import com.example.callvideo.util.Constants;
import com.facebook.network.connectionclass.ConnectionClassManager;
import com.facebook.network.connectionclass.ConnectionQuality;
import com.facebook.network.connectionclass.DeviceBandwidthSampler;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import fi.vtt.nubomedia.kurentoroomclientandroid.KurentoRoomAPI;
import fi.vtt.nubomedia.kurentoroomclientandroid.RoomError;
import fi.vtt.nubomedia.kurentoroomclientandroid.RoomListener;
import fi.vtt.nubomedia.kurentoroomclientandroid.RoomNotification;
import fi.vtt.nubomedia.kurentoroomclientandroid.RoomResponse;
import fi.vtt.nubomedia.utilitiesandroid.LooperExecutor;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements RoomListener {

    private static final String FCONNECTION = "facebookconnection";
    private static final String CHECKSPEED = "checkspeedconnection";
    private String username, roomname;
    private String TAG = "MainActivity";
    private LooperExecutor executor;
    private static KurentoRoomAPI kurentoRoomAPI;
    private int roomId=0;
    private EditText mTextMessageET;
    private TextView mUsernameTV, mTextMessageTV;
    private String wsUri;
    public static Map<String, Boolean> userPublishList = new HashMap<>();
    Handler mHandler;
    int sum = 0;
    int avg;

    long startTime;
    long endTime;
    long fileSize;
    OkHttpClient client = new OkHttpClient();
    private int checkKbps;
    private int divideKbps;

    // bandwidth in kbps
    private int POOR_BANDWIDTH = 150;
    private int AVERAGE_BANDWIDTH = 550;
    private int GOOD_BANDWIDTH = 2000;

    private ConnectionQuality mConnectionClass;
    private ConnectionClassManager mConnectionClassManager;
    private DeviceBandwidthSampler mDeviceBandwidthSampler;
    private int mTries;
    private ConnectionQuality connectionQuality;
    private WifiManager wifiManager;
    private LoginResponse loginResponse;
    private String token;
    private String userName;
    private TextureView textureView;
    private int bWidth;
    private int bHeight;
    private int sendBandwidth;
    private Button btn_call;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.mUsernameTV = (TextView) findViewById(R.id.main_username);
        this.mTextMessageTV = (TextView) findViewById(R.id.message_textview);
        this.mTextMessageET = (EditText) findViewById(R.id.main_text_message);
        textureView = findViewById(R.id.view_finder);
        btn_call = findViewById(R.id.btn_call);

        this.mTextMessageTV.setText("");
        executor = new LooperExecutor();
        executor.requestStart();
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
//        Constants.ADDRESS_OCBS = mSharedPreferences.getString(Constants.SERVER_NAME, Constants.DEFAULT_SERVER);
        wsUri = mSharedPreferences.getString(Constants.SERVER_NAME, Constants.DEFAULT_SERVER);

        kurentoRoomAPI = new KurentoRoomAPI(executor, wsUri, this);
        mHandler = new Handler();
        permission();
        this.username     = mSharedPreferences.getString(Constants.USER_NAME, "");
        this.roomname     = mSharedPreferences.getString(Constants.ROOM_NAME, "");
//        Log.i(TAG, "username: "+this.username);
//        Log.i(TAG, "roomname: "+this.roomname);

        mConnectionClassManager = ConnectionClassManager.getInstance();
        mDeviceBandwidthSampler = DeviceBandwidthSampler.getInstance();
//        mConnectionClassManager.register(mListener);
        connectionQuality = ConnectionClassManager.getInstance().getCurrentBandwidthQuality();

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Load test certificate from assets
        setTimedHandler();
        getKbps();
//        checkConnection();

    }

    @Override
    public void onStart() {
        super.onStart();
        cameraResolution();

        if (!kurentoRoomAPI.isWebSocketConnected()) {
            this.mUsernameTV.setText(getString(R.string.room_connecting, username, roomname));
            Log.i(TAG, "Connecting to room at " + wsUri);
            kurentoRoomAPI.connectWebSocket();
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null){
            token = extras.getString("myToken");
            userName = extras.getString("myUsername");
        }
//        setTimedHandler();

        IntentFilter intentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(wifiStateReceiver, intentFilter);
    }

    private void showFinishingError(String title, String message) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) { finish(); }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");
//        unregisterReceiver(wifiStateReceiver);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        if (kurentoRoomAPI.isWebSocketConnected()) {
            kurentoRoomAPI.sendLeaveRoom(roomId);
            kurentoRoomAPI.disconnectWebSocket();
        }
        executor.requestStop();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            Intent intent = new Intent(this, PreferencesActivity.class);
//            startActivity(intent);
            return true;
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_sign_out) {
            kurentoRoomAPI.sendLeaveRoom(roomId);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void joinRoom () {
        Constants.id++;
        roomId = Constants.id;
        Log.i(TAG, "Joinroom: User: "+this.username+", Room: "+this.roomname+" id:"+roomId);
        if (kurentoRoomAPI.isWebSocketConnected()) {
            kurentoRoomAPI.sendJoinRoom(this.username, this.roomname, true, roomId);
        }
    }

    public void makeCall(final int sendBandwidth){

            btn_call.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(MainActivity.this, ActivityVideoChat.class);
                    intent.putExtra("bandwidth", sendBandwidth);
                    intent.putExtra(Constants.USER_NAME, username);
                    startActivity(intent);
                }
            });
    }

    private void permission(){

        Dexter.withActivity(MainActivity.this)
                .withPermissions(
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO
                )
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        Log.d("checkpermission", String.valueOf(report.areAllPermissionsGranted()));

                        if (report.areAllPermissionsGranted()){
                            Log.d("checkpermission", "granted");
                        } else if(report.isAnyPermissionPermanentlyDenied()) {
                            Log.d("checkpermission", "not granted");
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                    }
                }).onSameThread().check();


    }

    private void openSettingDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Required Permissions");
        builder.setMessage("This app require permission to use some features. Please Grant them in app settings.");
        builder.setPositiveButton("Go To Setting", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent, 101);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    @Override
    public void onRoomResponse(RoomResponse response) {
        // joinRoom response
        if (response.getMethod()==KurentoRoomAPI.Method.JOIN_ROOM) {
            userPublishList = new HashMap<>(response.getUsers());
        }
    }

    @Override
    public void onRoomError(RoomError error) {
        Log.wtf(TAG, error.toString());
        if(error.getCode() == 104) {
            showFinishingError("Room error", "Username already taken");
        }
    }


    private Runnable clearMessageView = new Runnable() {
        @Override
        public void run() {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTextMessageTV.setText("");
                }
            });
        }
    };


    @Override
    public void onRoomNotification(RoomNotification notification) {
        Log.i(TAG, notification.toString());
        Map<String, Object> map = notification.getParams();

        // Somebody wrote a message to other users in the room
        if(notification.getMethod().equals(RoomListener.METHOD_SEND_MESSAGE)) {
            final String user = map.get("user").toString();
            final String message = map.get("message").toString();
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTextMessageTV.setText(getString(R.string.room_text_message, user, message));
                    mHandler.removeCallbacks(clearMessageView);
                    mHandler.postDelayed(clearMessageView, 5000);
                }
            });
        }

        // Somebody left the room
        else if(notification.getMethod().equals(RoomListener.METHOD_PARTICIPANT_LEFT)) {
            final String user = map.get("name").toString();
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run(){
                    mTextMessageTV.setText(getString(R.string.participant_left, user));
                    mHandler.removeCallbacks(clearMessageView);
                    mHandler.postDelayed(clearMessageView, 3000);
                }
            });
        }

        // Somebody joined the room
        else if(notification.getMethod().equals(RoomListener.METHOD_PARTICIPANT_JOINED)) {
            final String user = map.get("id").toString();
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTextMessageTV.setText(getString(R.string.participant_joined, user));
                    mHandler.removeCallbacks(clearMessageView);
                    mHandler.postDelayed(clearMessageView, 3000);
                }
            });
        }

        // Somebody in the room published their video
        else if(notification.getMethod().equals(RoomListener.METHOD_PARTICIPANT_PUBLISHED)) {
            final String user = map.get("id").toString();
            userPublishList.put(user, true);
            Log.i(TAG, "I'm " + username + " DERP: Other peer published already:" + notification.toString());
        }

    }

    @Override
    public void onRoomConnected() {
        if (kurentoRoomAPI.isWebSocketConnected()) {
            joinRoom();
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mUsernameTV.setText(getString(R.string.room_title, username, roomname));
                }
            });
        }
    }

    @Override
    public void onRoomDisconnected() {
//        showFinishingError("Disconnected", "You have been disconnected from room.");
    }

    public static KurentoRoomAPI getKurentoRoomAPIInstance(){
        return kurentoRoomAPI;
    }


    private BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int wifiStateExtra = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN);

            ConnectivityManager cm =
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {

        } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE){
            Log.d(FCONNECTION, "listener ");

        }

            switch (wifiStateExtra) {
                case WifiManager.WIFI_STATE_ENABLED:
                    break;
                case WifiManager.WIFI_STATE_DISABLED:
                    break;

            }
        }
    };



    private void setTimedHandler(){
//        int begin = 0;
//        int timeInterval = 1000;
//        final Timer timer = new Timer();
//
//        TimerTask timerTask = new TimerTask() {
//            int counter = 0;
//            @Override
//            public void run() {
//                    sum = sum + checkKbps();
//                Log.d("check average " , " average " + sum );
//                if (counter++ >=10){
//                    timer.cancel();
//                }
//            }
//        };
//        Log.d("check avg ",  " avg " + sum / 10);
//
//        timer.schedule(timerTask, begin, timeInterval);
    }

    private void cameraResolution(){
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(displaySize);

        DisplayMetrics displayMetrics = new DisplayMetrics();


        bHeight = displaySize.y / 8;
        bWidth = displaySize.x / 5;

        Rational aspectRatio = new Rational(textureView.getWidth(), textureView.getHeight());
        Size screen = new Size(textureView.getWidth(), textureView.getHeight()); //size of the screen

        Log.d("check camera nya", "checkcamera " + displaySize.x + " dan " + displaySize.y);
        Log.d("check camera nya", "checkcamera 2 " + bWidth + " dan " + bHeight);
        PreviewConfig pConfig = new PreviewConfig.Builder().setTargetAspectRatio(aspectRatio).setTargetResolution(screen).build();
        Preview preview = new Preview(pConfig);
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();
        final ImageCapture imgCap = new ImageCapture(imageCaptureConfig);



    }

    @TargetApi(Build.VERSION_CODES.M)
    private int checkConnection(){
        int speedLevel = 0;
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
//        boolean isConnected = activeNetwork != null &&
//                activeNetwork.isConnectedOrConnecting();

        boolean isWifi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
        boolean isHP = activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE;

        //should check null because in airplane mode it will be null
        ConnectivityManager connectivityManager = (ConnectivityManager)this.getSystemService(CONNECTIVITY_SERVICE);
        NetworkCapabilities nc = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        int downSpeed = nc.getLinkDownstreamBandwidthKbps();
        int upSpeed = nc.getLinkUpstreamBandwidthKbps();

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int numberOfLevels = 5;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int level = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), numberOfLevels);

        if (isHP){
            speedLevel = switchConnection(downSpeed);
        } else if (isWifi){
            switchCase(level, wifiInfo, downSpeed);
        }

        makeCall(sendBandwidth);

        return speedLevel;

    }

    private int switchConnection(int something){
        int speedLevel = 0;
        Log.d(CHECKSPEED, " mobile " + (something));

        if (something == 0){
            speedLevel = 0;
            Log.d(CHECKSPEED, " No Connection");
        } else if (something < 250){
            speedLevel = 100;
            Log.d(CHECKSPEED, " very poor");
        } else if (something <= 10000){
            speedLevel = 110;
            Log.d(CHECKSPEED, " Poor");
        } else if (something < 35000) {
            speedLevel = 150;
            Log.d(CHECKSPEED, " moderate");
        } else if (something < 80000) {
            speedLevel = 200;
            Log.d(CHECKSPEED, " Good");
        } else if (something > 80000) {
            speedLevel = 250;
            Log.d(CHECKSPEED, " Excelent");
        }
        return speedLevel;
    }

    private int switchCase(int level, WifiInfo wifiInfo, int something){

        int speedLevel = 0;
        Log.d(CHECKSPEED, " wifi " + (something));
        switch (level)
        {
            case 0:
                speedLevel = 0;
                Log.d(CHECKSPEED, "No Connection" + wifiInfo.getRssi() + " dan " + level);
                break;

            case 1:
                Log.d(CHECKSPEED,"Very Poor " + wifiInfo.getRssi());
                speedLevel = 50;
                break;

            case 2:
                Log.d(CHECKSPEED,"Poor" + wifiInfo.getRssi());
                speedLevel = 100;
                break;

            case 3:
                Log.d(CHECKSPEED,"Moderate"+ wifiInfo.getRssi());
                speedLevel = 500;
                break;

            case 4:
                Log.d(CHECKSPEED,"Good"+ wifiInfo.getRssi() + " dan dan "+ level);
                speedLevel = 1000;
                break;

            case 5:
                Log.d(CHECKSPEED,"Excellent"+ wifiInfo.getRssi());
                speedLevel = 1000;
                break;
        }
        return speedLevel;
    }

    private int checkKbps(){
        Request request = new Request.Builder()
                .url("http://139.180.134.73/img/250K.txt")
                .build();

        startTime = System.currentTimeMillis();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.d("checkKbps", e.toString());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                Headers responseHeaders = response.headers();
                for (int i = 0, size = responseHeaders.size(); i < size; i++) {
//                    Log.d(CHECKSPEED, responseHeaders.name(i) + ": " + responseHeaders.value(i));
                }

                InputStream input = response.body().byteStream();

                try {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];

                    while (input.read(buffer) != -1) {
                        bos.write(buffer);
                    }
                    byte[] docBuffer = bos.toByteArray();
                    fileSize = bos.size();

                } finally {
                    input.close();
                }

                endTime = System.currentTimeMillis();

                double timeTakenMills = Math.floor(endTime - startTime);  // time taken in milliseconds
                double timeTakenSecs = timeTakenMills / 1000;  // divide by 1000 to get time in seconds
                divideKbps = (int) Math.round(1024 / timeTakenSecs);

                if(divideKbps <= POOR_BANDWIDTH){
                    // slow connection
                }
//                 get the download speed by dividing the file size by time taken to download
//                double speed = fileSize / timeTakenMills;

//                Log.d(CHECKSPEED, "Time taken in secs: " + timeTakenSecs);
//                Log.d(CHECKSPEED, "File size: " + fileSize);
            }
        });

        return divideKbps;
    }

    private int getKbps (){
        Log.d("getKbps ", "getKbps 2 " + checkKbps());
        for (int i = 0; i < 10; i++){
            sum = sum + checkKbps();

        }
        Log.d("getKbps ", "getKbps 2 " + sum);
        return sum;
    }
}


//    private void facebookConnectionClass(){
//        OkHttpClient client = new OkHttpClient();
//        Request request = new Request.Builder()
//                .url("http://139.180.134.73/img/spinner.gif")
//                .build();
//
//        mDeviceBandwidthSampler.startSampling();
//        client.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(@NotNull Call call, @NotNull IOException e) {
//                mTries = 0;
//                Log.d(FCONNECTION, "exception" + e.toString());
//                mDeviceBandwidthSampler.stopSampling();
//
//                if(mConnectionClass == ConnectionQuality.UNKNOWN && mTries < 10){
//                    mTries++;
////                    checkConnection();
//                    Log.d(FCONNECTION, "onFailure");
//                }
//
//                if (!mDeviceBandwidthSampler.isSampling()) {
//                    Log.d(FCONNECTION, "mDeviceBandwidthSampler.isSampling");
//                }
//
//            }
//
//            @Override
//            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
//                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
//
//                Headers responseHeaders = response.headers();
//                for (int i = 0, size = responseHeaders.size(); i < size; i++) {
//                    Log.d(TAG, responseHeaders.name(i) + ": " + responseHeaders.value(i));
//                }
//
//                Log.d("facebookCon", response.body().string());
//                Log.d("facebookCon", mConnectionClassManager.getCurrentBandwidthQuality().toString());
//
//                mDeviceBandwidthSampler.stopSampling();
//            }
//        });
//    }
//
//private  class ConnectionChangedListener implements ConnectionClassManager.ConnectionClassStateChangeListener{
//
//    @Override
//    public void onBandwidthStateChange(ConnectionQuality bandwidthState) {
//        Log.d(FCONNECTION, "onBandwidthStateChange");
//        mConnectionClass = bandwidthState;
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//
//                Toast.makeText(MainActivity.this, mConnectionClass + "", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//}
//
//    public void sendTextMessage(View view){
//        String message=mTextMessageET.getText().toString();
//        if(message.length()>0) {
//            Log.d("SendMessage: ", this.roomname + ", " + this.username + ", " + message);
//            mTextMessageET.setText("");
//            InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
//            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
//            if(kurentoRoomAPI.isWebSocketConnected()){
//                Log.i(TAG, "sendMessage");
//                kurentoRoomAPI.sendMessage(this.roomname, this.username, message, Constants.id++);
//            }
//        }
//        else {
//            Toast.makeText(MainActivity.this, "no message", Toast.LENGTH_SHORT).show();
//        }
//    }