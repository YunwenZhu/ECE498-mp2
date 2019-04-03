package com.practice.ece498mp1;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
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
    private Sensor accelerometer, gyroscope, magneticField, lightSensor;
    private double Accel_x, Accel_y, Accel_z, Gyro_x, Gyro_y, Gyro_z, Mag_x, Mag_y, Mag_z, lightIntensity;
    private Timestamp timestamp;
    private HashSet<String> s = new HashSet<>();
    private CSVPrinter printer;
    private boolean recording_;
    private String outputFile;

    float [] mag = new float[3];
    float [] acc = new float[3];
    Boolean firstMag = false;
    Boolean firstAccel = false;
    Boolean firstData = false;
    float degreesTurned;
    float previousVel, prevOrientation;



    TextView xValue, yValue, zValue, step_count, distance, turned;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        xValue = (TextView) findViewById(R.id.xValue);
        yValue = (TextView) findViewById(R.id.yValue);
        zValue = (TextView) findViewById(R.id.zValue);
        step_count = (TextView) findViewById(R.id.step_count);
        distance = (TextView) findViewById(R.id.distance);
        turned = (TextView) findViewById(R.id.turned);

        Log.d(TAG, "onCreate: Initializing Sensor Services");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        Log.d(TAG, "onCreate: Registered accelerometer listener");

        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        Log.d(TAG, "onCreate: Registered gyroscope listener");
//
        magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_NORMAL);
        Log.d(TAG, "onCreate: Registered magneticField listener");
//
//        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
//        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
//        Log.d(TAG, "onCreate: Registered lightSensor listener");

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
            float [] orientationValues = new float[3];

            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER && !s.contains("Accel")) {
                acc = event.values;
                firstAccel = true;

                Accel_x = event.values[0];
                Accel_y = event.values[1];
                Accel_z = event.values[2];

                xValue.setText("Accel_x:" + Accel_x);
                yValue.setText("Accel_y:" + Accel_y);
                zValue.setText("Accel_z:" + Accel_z);
                Log.d(TAG, "acccccce");

//            } else if (sensor.getType() == Sensor.TYPE_GYROSCOPE && !s.contains("Gyro")) {
//                Gyro_x = event.values[0];
//                Gyro_y = event.values[1];
//                Gyro_z = event.values[2];

            } else if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD && !s.contains("Mag")) {
                mag = event.values;
                firstMag = true;
                Log.d(TAG, "#######################");
            }

            else if (sensor.getType() == Sensor.TYPE_GYROSCOPE){
                if (!firstData && firstMag && firstAccel){
                    Log.e("working??", "bruhtf");

                    Log.e("wein", "______________________________");

                    previousVel = Math.abs(event.values[2]);
                    findOrientation(orientationValues);
                    prevOrientation = orientationValues[0];
                    firstData = true;
                }
                else if (firstData){
                    Log.e("working??", "START");

                    float z = Math.abs(event.values[2]);

                    findOrientation(orientationValues);

                    if ((z - previousVel) > 0.005 && orientationValues != null) {
                        float orientation = orientationValues[0];
                        if (event.values[2] > 0) {
                            //turned Left (relatively)
                            if ((orientation > 0 && prevOrientation < 0)) {
                                degreesTurned += ((Math.PI + prevOrientation) + (Math.PI - orientation)) * 180/Math.PI;
                            } else if (prevOrientation > 0 && orientation < 0) {
                                degreesTurned += ((prevOrientation) + (0 - orientation)) * 180 / Math.PI;
                            } else {
                                degreesTurned += (Math.abs(orientation - prevOrientation)) * 180/Math.PI;
                            }
                        }
                        else {
                            //turned right (relatively)
                            if ((orientation > 0 && prevOrientation < 0)) {
                                degreesTurned += ((0 - prevOrientation) + orientation) * 180/Math.PI;
                            } else if (prevOrientation > 0 && orientation < 0) {
                                degreesTurned += ((Math.PI + orientation) + (Math.PI - prevOrientation)) * 180/Math.PI;
                            } else {
                                degreesTurned += (Math.abs(orientation - prevOrientation)) * 180/Math.PI;
                            }

                        }
//                            degreesTurned += (dt * ((z + previousVel)/2))*180/Math.PI;
                        turned = (TextView) findViewById(R.id.turned);
                        turned.setText("degreesTurned:" + degreesTurned);

                    }
                    previousVel = z;




            }
            }

//            } else if (sensor.getType() == Sensor.TYPE_LIGHT && !s.contains("Light")) {
//                lightIntensity = event.values[0];

//            }

            timestamp = new Timestamp(System.currentTimeMillis());
            Log.i(TAG, "log to file");
            try {
                printer.printRecord(timestamp.getTime(), Accel_x, Accel_y, Accel_z);
            } catch (IOException ex) {
                ex.printStackTrace();
            }


        }
    }

    protected Boolean findOrientation(float [] values){
        float [] R = new float[9];
        float [] I = new float[9];
        if (acc != null && mag != null) {
            SensorManager.getRotationMatrix(R, I , acc, mag);
            SensorManager.getOrientation(R, values);
            return true;
        }
        return false;
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
                File folder = new File(Environment.getExternalStorageDirectory() + "/MP2/");
                boolean isPresent = true;
                if (!folder.exists()) {
                    isPresent = folder.mkdir();
                }
                if (isPresent) {

                    outputFile = Environment.getExternalStorageDirectory() + "/ECE498 mp2/" + t.getText().toString() + "-" + df.format(new Date()) + ".csv";
                    printer = new CSVPrinter(new FileWriter(outputFile), CSVFormat.DEFAULT.withHeader("Time Stamp", "Accel_x", "Accel_y", "Accel_z"));
                } else {
                    Log.e("stop", "folder not made");
                }
//                outputFile = Environment.getExternalStorageDirectory() + "/ECE498 mp2/" + t.getText().toString() + "-" + df.format(new Date()) + ".csv";
//                printer = new CSVPrinter(new FileWriter(outputFile), CSVFormat.DEFAULT.withHeader("Time Stamp", "Accel_x", "Accel_y", "Accel_z"));
            } catch (IOException ex) {
                Log.e(TAG, ex.toString());
                throw ex;
            }
            b.setText("Stop");
            degreesTurned = 0;
            t.setEnabled(false);
            Log.i(TAG, "Start recording");
        } else {
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
