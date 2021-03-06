package com.hackjunction.messageng;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.common.collect.Lists;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.iid.FirebaseInstanceId;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private String happySong;
    private String sadSong;

    private boolean isPlayingSong = false;

    private MediaPlayer mediaPlayer;

    private boolean state = true;

    private static final String TAG = "MainActivity";

    private LocalBroadcastManager localBroadcastManager;

    public static String stateToString(boolean state) {
        return state ? "happy" : "sad";
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), MessageNG.WAVE_UPDATE)) {

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                double[] alpha = intent.getDoubleArrayExtra("alpha");
                List<Double> alphaList = new ArrayList<>(alpha.length);
                for (Double x : alpha) {
                    alphaList.add(x);
                }
                db.collection("emotion")
                        .document("state")
                        .update(
                                "state", intent.getBooleanExtra("state",true),
                                "alpha", alphaList
                        );
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void permissionCheck() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                Toast.makeText(this, "The permission to get BLE location data is required", Toast.LENGTH_SHORT).show();
            } else {
                requestPermissions(new String[] {
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                }, 1);
            }
        } else {
            Log.i(TAG, "Permissions are fine.");
            //Toast.makeText(this, "Location permissions are fine!", Toast.LENGTH_SHORT).show();
        }
    }

    // Brian Code ¡¡¡¡¡
    private LineGraphSeries<DataPoint> series;

    //private final Handler handler= new Handler();

    /*private final Runnable r = new Runnable() {
        private int x = 0;
        @Override
        public void run() {
            series.appendData(
                    new DataPoint(x,getRandom()),false, 100, false
            );
            x++;
            handler.postDelayed(r,20);
        }
    };*/

    double start = -5;
    double end = 5;
    Random mRand = new Random();

    private double getRandom() {
        return start + (mRand.nextDouble() * (end - start));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    // ¡¡¡
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // !! Brian Code

        //handler.post(r);

        getSupportActionBar().hide();
        initGraph();
        // ¡¡

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissionCheck();
        }

        //FirebaseFirestore.setLoggingEnabled(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        //final DocumentReference docRef = db.collection("heartbeat").document(
        //    FirebaseInstanceId.getInstance().getId()
        //);

        final DocumentReference docRef = db.collection("emotion").document("state");

        //firebase state change listener
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot snapshot, FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, snapshot.getData().toString());
                    Object o = snapshot.get("alpha");
                    if (o == null) {
                        return;
                    }
                    //updateUI(o.toString());

                    List<Double> newData = (List<Double>)o;

                    for(Double y : newData){
                        Long x = System.currentTimeMillis();
                        series.appendData(
                                new DataPoint(x,y),false, 100, false
                        );
                    }

                    Object rawState = snapshot.get("state");
                    if (rawState == null) {
                        state = false;
                    } else {
                        state = (boolean) ((Boolean) rawState);
                    }
                } else {
                    Log.d(TAG, "Current data: null");
                }
            }
        });

        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(MessageNG.WAVE_UPDATE);
        localBroadcastManager.registerReceiver(broadcastReceiver, filter);

        startService(new Intent(MainActivity.this, MuseService.class));
    }

    /*public void updateUI(String stateString) {
        TextView tv1 = findViewById(R.id.stateText);
        tv1.setText(stateString);
    }*/

    void initGraph() {
        GraphView graph = (GraphView) findViewById(R.id.graph);
        this.series = new LineGraphSeries<>();
        series.setThickness(15);
        series.setColor(Color.argb(50, 255, 255, 255));
        series.setBackgroundColor(Color.argb(50, 255, 255, 255));
        series.setDrawBackground(true);
        graph.addSeries(series);
        graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);// It will remove the background grids
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);// remove horizontal x labels and line
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
    }


    public void didTapPlayButton(View view) {
        animateButton();

        if (!this.isPlayingSong) {
            String song;
            if (state) {
                song = this.happySong;
            } else {
                song = this.sadSong;
            }
            //MediaPlayer mediaPlayer = MediaPlayer.create(this, Uri.parse(Environment.getExternalStorageDirectory().getPath()+ sadSong));
            //mediaPlayer.start(); // no need to call prepare(); create() does that for you

            mediaPlayer = new MediaPlayer();
            try {
                if (song == null) {
                    Toast.makeText(this, "Please select a song first! (" + stateToString(state) + ")", Toast.LENGTH_SHORT).show();
                    return;
                }
                mediaPlayer.setDataSource(song);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                mediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    isPlayingSong = false;
                }
            });
            this.isPlayingSong = true;

        } else {
            this.mediaPlayer.stop();
            this.isPlayingSong = false;
        }
    }

    void animateButton() {
        // Load the animation
        final Animation myAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bounce);

        // Use custom animation interpolator to achieve the bounce effect
        MyBounceInterpolator interpolator = new MyBounceInterpolator(0.2, 20);

        myAnim.setInterpolator(interpolator);

        // Animate the button
        Button button = (Button)findViewById(R.id.button);
        button.startAnimation(myAnim);
    }

    public void pickSong(View view) {
        Intent myIntent = new Intent(MainActivity.this, MusicSettings.class);
        //MainActivity.this.startActivity(myIntent);
        //startActivityForResult(MusicChooser, ActivityTwoRequestCode)
        startActivityForResult(myIntent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                this.happySong = data.getStringExtra("happySong");
                this.sadSong = data.getStringExtra("sadSong");

                System.out.println(this.happySong);
            }
        }
    }
}