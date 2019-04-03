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
    private Sensor accelerometer, gyroscope;
    private double Accel_x, Accel_y, Accel_z;
    private Timestamp timestamp;
    private HashSet<String> s = new HashSet<>();
    private CSVPrinter printer;
    private boolean recording_;
    private String outputFile;
    private boolean firstTime = true;

    private double ema = 10.47; //hard code
    private double prev = 0.0;
    private int step = 0;
    private double dis = 0.0;
    private double dis_per_step = 0.35;

    TextView degree, step_count, distance;

    private float previousVel = 0;
    private double prevTimestamp = 0;
    private Boolean firstData = false;

    int bufferSize = 18;
    double[] checkDrift = new double[bufferSize];
    float[] gyroSign = new float[bufferSize];
    int ind = 0;
    float checkDegreesTurned, degreesTurned, angle;
    double timestampE;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        degree = (TextView) findViewById(R.id.degree);
        step_count = (TextView) findViewById(R.id.step_count);
        distance = (TextView) findViewById(R.id.distance);

        Log.d(TAG, "onCreate: Initializing Sensor Services");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);

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
            } else if (sensor.getType() == Sensor.TYPE_GYROSCOPE && !s.contains("Gyro")) {
                if (!firstData) {

                    previousVel = Math.abs(event.values[2]);
                    prevTimestamp = event.timestamp / 1000000000.0;
                    firstData = true;

                } else if (firstData) {

                    float z = Math.abs(event.values[2]);
                    double timestampSec = event.timestamp / 1000000000.0;
                    double dt = timestampSec - prevTimestamp;
                    double integral = (dt * ((z + previousVel) / 2)) * 180 / Math.PI;

                    Log.e("INTEGRAL: ", integral + " dtime: " + dt);
                    checkDegreesTurned += (dt * ((z + previousVel) / 2)) * 180 / Math.PI;
                    float accumulateAngle = 0;

                    if (ind < bufferSize) {
                        gyroSign[ind] = event.values[2];
                        checkDrift[ind] = integral;
                        ind++;
                    } else {
                        for (int i = 0; i < bufferSize; i++) {
                            if (gyroSign[i] < 0) {
                                accumulateAngle += checkDrift[i];
                            } else {
                                accumulateAngle -= checkDrift[i];
                            }
                            gyroSign[i] = 0;
                            checkDrift[i] = 0;
                        }
                        if (accumulateAngle > 4 || accumulateAngle < -4) {
                            degreesTurned += checkDegreesTurned;
                            Log.e("check if real", "ITS GOOD " + accumulateAngle + "   " + checkDegreesTurned);

                        } else {
                            Log.e("check if real", "____" + accumulateAngle + "   " + checkDegreesTurned);

                        }

                        ind = 0;
                        checkDegreesTurned = 0;


                        if (accumulateAngle < 0) {
                            angle = (angle + accumulateAngle) % 360;
                            if (angle < 0) {
                                angle = 360 + angle;
                            }

                        } else if (accumulateAngle >= 0) {
                            angle = (angle + accumulateAngle) % 360;
                        }

                    }
                    degree.setText("Degrees Turned:" + degreesTurned +
                            "Actual Angle:" + angle);
//                    try {
//                        printer.printRecord(timestamp.getTime(), angle);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }

                    previousVel = z;
                    prevTimestamp = timestampSec;

                }
            }

            timestamp = new Timestamp(System.currentTimeMillis());
            Log.i(TAG, "log to file");
            try {
                printer.printRecord(timestamp.getTime(), Accel_x, Accel_y, Accel_z, angle, degreesTurned);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void cleanVariables(){
        ind = 0;
        degreesTurned = 0;
        checkDegreesTurned = 0;
        firstData = false;
        step = 0;
        dis = 0.0;
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
            cleanVariables();
            b.setText("Stop");
            t.setEnabled(false);
            Log.i(TAG, "Start recording");
        } else {
            step_count.setText("Step: " + 0);
            distance.setText("Distance:" + 0.0 + "m");
            degree.setText("Degrees Turned: 0  Current Angle: 0");
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
