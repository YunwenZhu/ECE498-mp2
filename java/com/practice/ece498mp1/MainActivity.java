package com.practice.ece498mp1;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

import org.apache.commons.csv.*;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "MainActivity";
    private SensorManager sensorManager;
    private Sensor accelerometer, sound, wifi;
    private double Accel_x, Accel_y, Accel_z;
    private Timestamp timestamp;
    private HashSet<String> s = new HashSet<>();
    private CSVPrinter printer;
    private boolean recording_;
    private String outputFile;
    private boolean firstTime = true;

    private double ema = 10.45; //hard code
    private double prev = 0.0;
    private int step = 0;
    private double dis = 0.0;
    private double dis_per_step = 0.35;

    TextView xValue, yValue, zValue, step_count, distance;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        xValue = (TextView) findViewById(R.id.xValue);
        yValue = (TextView) findViewById(R.id.yValue);
        zValue = (TextView) findViewById(R.id.zValue);
        step_count = (TextView) findViewById(R.id.step_count);
        distance = (TextView) findViewById(R.id.distance);

        Log.d(TAG, "onCreate: Initializing Sensor Services");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        Log.d(TAG, "onCreate: Registered accelerometer listener");

        Timestamp startTime;

        recording_ = false;
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (recording_) {
            Sensor sensor = event.sensor;

            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER && !s.contains("Accel")) {

                Accel_x = event.values[0];
                Accel_y = event.values[1];
                Accel_z = event.values[2];

                xValue.setText("Accel_x:" + Accel_x);
                yValue.setText("Accel_y:" + Accel_y);
                zValue.setText("Accel_z:" + Accel_z);
                Log.d(TAG, "acccccce");

                if(firstTime){
                    prev = Math.sqrt(Math.pow(Accel_x, 2) + Math.pow(Accel_y, 2) + Math.pow(Accel_z, 2));
                    firstTime = false;
                }
                else {
                    double cur = Math.sqrt(Math.pow(Accel_x, 2) + Math.pow(Accel_y, 2) + Math.pow(Accel_z, 2));
                    if(cur <= ema && prev >= ema){
                        if(Math.abs(cur - ema) >= 0.1){
                            step += 1;
                        }
                    }
                    dis = dis_per_step * step;
                    step_count.setText("Step: " + step);
                    distance.setText("Distance:" + dis + "m");
                    prev = cur;
                }

            }

            timestamp = new Timestamp(System.currentTimeMillis());
            Log.i(TAG, "log to file");
            try {
                printer.printRecord(timestamp.getTime(), Accel_x, Accel_y, Accel_z);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


    public void toggleRecording(View view) throws IOException {
        Button b = (Button) view;
        TextView t = findViewById(R.id.editText);
        Context c = getApplicationContext();
        if (!recording_) {
            if (t.getText().length() == 0) {
                Toast.makeText(c, "file name can't be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                DateFormat df = new SimpleDateFormat("YYYY-MM-DD-HH:mm:ss");
                outputFile = getExternalFilesDir(null) + t.getText().toString() + "-" + df.format(new Date()) + ".csv";
                printer = new CSVPrinter(new FileWriter(outputFile), CSVFormat.DEFAULT.withHeader("Time Stamp", "Accel_x", "Accel_y", "Accel_z"));
            } catch (IOException ex) {
                Log.e(TAG, ex.toString());
                throw ex;
            }
            step = 0;
            dis = 0.0;
            b.setText("Stop");
            t.setEnabled(false);
            Log.i(TAG, "Start recording");
        } else {
            step_count.setText("Step: " + 0);
            distance.setText("Distance:" + 0.0 + "m");
            printer.close(true);
            b.setText("Start");
            t.setEnabled(true);
            t.setText("");
            Log.i(TAG, "Stop recording");
            Toast.makeText(c, "file saved to " + outputFile, Toast.LENGTH_SHORT).show();
        }
        recording_ = !recording_;
    }
}
