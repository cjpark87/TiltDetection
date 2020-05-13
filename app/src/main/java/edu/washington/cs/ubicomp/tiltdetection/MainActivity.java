package edu.washington.cs.ubicomp.tiltdetection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "TiltDetection";
    private static final double SAMPLE_INTERVAL = 0.02;
    private static final double GRAVITY = 9.81;

    Button startButton;
    TextView tiltText;
    GraphView accelGraph;
    GraphView gyroGraph;
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private Sensor accelerometer;
    private Sensor gyroscope_raw;
    private Sensor accelerometer_raw;

    private double curr_pitch;
    private double curr_roll;
    private double curr_pitch_acc;
    private double curr_roll_acc;
    private double curr_pitch_gyro;
    private double curr_roll_gyro;

    private double alpha = 0.05;

    private ArrayList<float[]> acc_data_raw;
    private ArrayList<float[]> gyro_data_raw;

    private ArrayList<LineGraphSeries<DataPoint>> acc_display_raw = new ArrayList<>();
    private ArrayList<LineGraphSeries<DataPoint>> acc_display_calibrated = new ArrayList<>();
    private ArrayList<ArrayList<DataPoint>> acc_result_raw = new ArrayList<>();
    private ArrayList<ArrayList<DataPoint>> acc_result_calibrated = new ArrayList<>();

    private ArrayList<LineGraphSeries<DataPoint>> gryo_display_raw = new ArrayList<>();
    private ArrayList<LineGraphSeries<DataPoint>> gryo_display_calibrated = new ArrayList<>();
    private ArrayList<ArrayList<DataPoint>> gryo_result_raw = new ArrayList<>();
    private ArrayList<ArrayList<DataPoint>> gryo_result_calibrated = new ArrayList<>();

    private double[] gyro_bias = {0.0, 0.0, 0.0};
    private double[] accel_bias = {0.0, 0.0, 0.0};

    private ArrayList<Double> pitch_results;
    private ArrayList<Double> roll_results;
    private ArrayList<Double> pitch_results_acc;
    private ArrayList<Double> roll_results_acc;
    private ArrayList<Double> pitch_results_gyro;
    private ArrayList<Double> roll_results_gyro;

    private int graphColor[] = {Color.argb(255,255,180,9), // orange
            Color.argb(255,46, 168, 255), // blue
            Color.argb(255, 129, 209, 24), // green
            Color.argb(255, 225, 225, 0), // yellow
            Color.argb(255, 150, 150, 150)};

    private int graphColorDotted[] = {Color.argb(125,255,180,9), // orange
            Color.argb(125,46, 168, 255), // blue
            Color.argb(125, 129, 209, 24), // green
            Color.argb(125, 225, 225, 0), // yellow
            Color.argb(125, 150, 150, 150)};

    private Paint dottedLine;
    private int MOTION_PREVIEW_SIZE = 20000;
    private boolean recording = false;
    private boolean calibrated = false;
    private MainActivity thisActivity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gyroscope_raw = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
        accelerometer_raw = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED);

        startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recording) {
                    sensorManager.unregisterListener(thisActivity);
                    startButton.setText("Start");
                    recording = false;
                    calibrated = false;

                    writeResults();
                } else {
                    pitch_results = new ArrayList<>();
                    roll_results = new ArrayList<>();
                    pitch_results_acc = new ArrayList<>();
                    roll_results_acc = new ArrayList<>();
                    pitch_results_gyro = new ArrayList<>();
                    roll_results_gyro = new ArrayList<>();

                    acc_data_raw = new ArrayList<>();
                    gyro_data_raw = new ArrayList<>();

                    acc_display_raw = new ArrayList<>();
                    acc_display_calibrated = new ArrayList<>();

                    gryo_display_raw = new ArrayList<>();
                    gryo_display_calibrated = new ArrayList<>();

                    accelGraph.removeAllSeries();

                    for (int i = 0; i < 3; i++) {
                        acc_display_raw.add(new LineGraphSeries<DataPoint>());
                        acc_display_calibrated.add(new LineGraphSeries<DataPoint>());
                        // Display filtered PPG signal
                        acc_display_raw.get(i).setColor(graphColor[i]);
                        acc_display_raw.get(i).setThickness(10);

                        Paint paint = new Paint();
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(10);
                        paint.setColor(graphColorDotted[i]);
                        paint.setPathEffect(new DashPathEffect(new float[]{10, 5}, 0));
                        acc_display_raw.get(i).setCustomPaint(paint);
                        acc_display_raw.get(i).setTitle("Acc Raw");

                        acc_display_calibrated.get(i).setColor(graphColor[i]);
                        acc_display_calibrated.get(i).setThickness(10);
                        acc_display_calibrated.get(i).setTitle("Acc Calib");
                        accelGraph.addSeries(acc_display_raw.get(i));
                        //accelGraph.addSeries(acc_display_calibrated.get(i));

                    }

                    gyroGraph.removeAllSeries();

                    for (int i = 0; i < 3; i++) {
                        gryo_display_raw.add(new LineGraphSeries<DataPoint>());
                        gryo_display_calibrated.add(new LineGraphSeries<DataPoint>());
                        // Display filtered PPG signal
                        gryo_display_raw.get(i).setColor(graphColor[i]);
                        gryo_display_raw.get(i).setThickness(10);

                        Paint paint = new Paint();
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(10);
                        paint.setColor(graphColorDotted[i]);
                        paint.setPathEffect(new DashPathEffect(new float[]{10, 5}, 0));
                        gryo_display_raw.get(i).setCustomPaint(paint);
                        gryo_display_raw.get(i).setTitle("Gyro Raw");

                        gryo_display_calibrated.get(i).setColor(graphColor[i]);
                        gryo_display_calibrated.get(i).setThickness(10);
                        gryo_display_calibrated.get(i).setTitle("Gyro Calib");
                        gyroGraph.addSeries(gryo_display_raw.get(i));
                        //gyroGraph.addSeries(gryo_display_calibrated.get(i));

                    }

                    sensorManager.registerListener(thisActivity, accelerometer, SensorManager.SENSOR_DELAY_GAME);
                    sensorManager.registerListener(thisActivity, accelerometer_raw, SensorManager.SENSOR_DELAY_GAME);
                    sensorManager.registerListener(thisActivity, gyroscope, SensorManager.SENSOR_DELAY_GAME);
                    sensorManager.registerListener(thisActivity, gyroscope_raw, SensorManager.SENSOR_DELAY_GAME);
                    startButton.setText("Stop");
                    recording = true;

                    tiltText.setText("Calibrating...");
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            calibrateAccelerometer();

                            acc_data_raw = new ArrayList<>();
                            gyro_data_raw = new ArrayList<>();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tiltText.setText("0.00°, 0.00°");
                                }
                            });

                            calibrated = true;
                        }
                    }, 10*1000);
                }
            }
        });
        tiltText = findViewById(R.id.tilt_text);
        accelGraph = findViewById(R.id.graph_acc);
        gyroGraph = findViewById(R.id.graph_gyro);

        accelGraph = findViewById(R.id.graph_acc);
        accelGraph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        accelGraph.setBackgroundColor(Color.TRANSPARENT);
        accelGraph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        accelGraph.getGridLabelRenderer().setVerticalLabelsVisible(false);

        gyroGraph = findViewById(R.id.graph_gyro);
        gyroGraph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        gyroGraph.setBackgroundColor(Color.TRANSPARENT);
        gyroGraph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        gyroGraph.getGridLabelRenderer().setVerticalLabelsVisible(false);

    }

    private void writeResults() {
        JSONObject object = new JSONObject();
        JSONArray acc_pitch = new JSONArray();
        JSONArray acc_roll = new JSONArray();
        JSONArray gyro_pitch = new JSONArray();
        JSONArray gyro_roll = new JSONArray();
        JSONArray pitch = new JSONArray();
        JSONArray roll = new JSONArray();

        try {
            for (Double d : pitch_results) {
                pitch.put(d.doubleValue());
            }

            object.put("pitch", pitch);

            for (Double d : roll_results) {
                roll.put(d.doubleValue());
            }

            object.put("roll", roll);

            for (Double d : pitch_results_acc) {
                acc_pitch.put(d.doubleValue());
            }

            object.put("pitch_acc", acc_pitch);

            for (Double d : roll_results_acc) {
                acc_roll.put(d.doubleValue());
            }

            object.put("roll_acc", acc_roll);

            for (Double d : pitch_results_gyro) {
                gyro_pitch.put(d.doubleValue());
            }

            object.put("pitch_gyro", gyro_pitch);

            for (Double d : roll_results_gyro) {
                gyro_roll.put(d.doubleValue());
            }

            object.put("roll_gyro", gyro_roll);

            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ssZ");

                File dir = new File(Environment.getExternalStorageDirectory(), "tilt");

                if (!dir.exists()) {
                    boolean r = dir.mkdir();
                    Log.d(TAG, r? "True":"False");
                }

                File file = new File(Environment.getExternalStorageDirectory(),
                        "tilt/" + simpleDateFormat.format(new Date()) + ".json");
                Writer output = new BufferedWriter(new FileWriter(file));
                output.write(object.toString());
                output.close();
                MediaScannerConnection.scanFile(this, new String[]{file.getPath()},
                        /*mimeTypes*/null, new MediaScannerConnection.MediaScannerConnectionClient() {
                            @Override
                            public void onMediaScannerConnected() {
                                // Do nothing
                            }

                            @Override
                            public void onScanCompleted(String path, Uri uri) {
                                Log.i(TAG, "Scanned " + path + ":");
                                Log.i(TAG, "-> uri=" + uri);
                            }
                        });
            } catch (Exception e) {
                Log.d(TAG, "EXCEPTION!!!");
                Log.d(TAG, e.getLocalizedMessage());
                Log.d(TAG, e.getStackTrace().toString());
            }

        } catch (Exception e) {

        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER_UNCALIBRATED) {
            acc_data_raw.add(sensorEvent.values);
            for (int i = 0; i < 3; i++) {
                if (acc_display_raw.get(i) == null) {
                    Log.d(TAG, "acc raw NULL");
                }
                DataPoint dataPoint = new DataPoint(acc_display_raw.size() > 0 ? acc_display_raw.get(i).getHighestValueX()+1: 1, sensorEvent.values[i]);
                acc_display_raw.get(i).appendData(dataPoint, true, MOTION_PREVIEW_SIZE);
            }

            if (calibrated)
                calculateAngleFromAccel();

        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION
                || sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            for (int i = 0; i < 3; i++) {
                if (acc_display_calibrated.get(i) == null) {
                    Log.d(TAG, "acc raw NULL");
                }
                DataPoint dataPoint = new DataPoint(acc_display_calibrated.size() > 0 ? acc_display_calibrated.get(i).getHighestValueX()+1: 1, sensorEvent.values[i]);
                acc_display_calibrated.get(i).appendData(dataPoint, true, MOTION_PREVIEW_SIZE);
            }
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            for (int i = 0; i < 3; i++) {
                if (gryo_display_calibrated.get(i) == null) {
                    Log.d(TAG, "acc raw NULL");
                }
                DataPoint dataPoint = new DataPoint(gryo_display_calibrated.size() > 0? gryo_display_calibrated.get(i).getHighestValueX()+1: 1, sensorEvent.values[i]);
                gryo_display_calibrated.get(i).appendData(dataPoint, true, MOTION_PREVIEW_SIZE);
            }
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE_UNCALIBRATED) {
            gyro_data_raw.add(sensorEvent.values);
            for (int i = 0; i < 3; i++) {
                if (gryo_display_raw.get(i) == null) {
                    Log.d(TAG, "acc raw NULL");
                }
                DataPoint dataPoint = new DataPoint(gryo_display_raw.size() > 0 ? gryo_display_raw.get(i).getHighestValueX()+1:1, sensorEvent.values[i]);
                gryo_display_raw.get(i).appendData(dataPoint, true, MOTION_PREVIEW_SIZE);
            }

            if (calibrated)
                calculateAngleFromGyro();
        }
    }

    private void calculateAngleFromAccel() {
        float[] last_acc_data = acc_data_raw.get(acc_data_raw.size() - 1);
        curr_pitch_acc = Math.atan2(last_acc_data[1]-accel_bias[1], last_acc_data[2]);// * 180/Math.PI;
        curr_roll_acc = Math.atan2(last_acc_data[0]-accel_bias[0], last_acc_data[2]);// * 180/Math.PI;
        //curr_pitch_acc = Math.atan2(-1*(last_acc_data[0]-accel_bias[0]),
        //        Math.sqrt((last_acc_data[1]-accel_bias[1])*(last_acc_data[1]-accel_bias[1])+(last_acc_data[2])*(last_acc_data[2])));// * 180/Math.PI;

        //Log.d(TAG, String.format("Angle from accel %.5f, %.5f", curr_pitch_acc, curr_roll_acc));

        pitch_results_acc.add(curr_pitch_acc);
        roll_results_acc.add(curr_roll_acc);
    }
    private void calculateAngleFromGyro() {
        float[] last_gyro_data = gyro_data_raw.get(gyro_data_raw.size()-1);
        curr_pitch = (1-alpha)*(curr_pitch + (last_gyro_data[0]-gyro_bias[0])*SAMPLE_INTERVAL) + alpha*curr_pitch_acc;
        curr_roll = (1-alpha)*(curr_roll + (last_gyro_data[1]-gyro_bias[1])*SAMPLE_INTERVAL) + alpha*curr_roll_acc;

        curr_pitch_gyro = curr_pitch_gyro + (last_gyro_data[0]-gyro_bias[0])*SAMPLE_INTERVAL;
        curr_roll_gyro = curr_roll_gyro + (last_gyro_data[1]-gyro_bias[1])*SAMPLE_INTERVAL;

        //Log.d(TAG, String.format("Angle from gyro %.5f, %.5f", last_gyro_data[0], last_gyro_data[1]));
        //Log.d(TAG, String.format("Angle from c.f. %.5f, %.5f", curr_pitch, curr_roll));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tiltText.setText(String.format("%.2f°, %.2f°", curr_pitch * 57.2958, curr_roll * 57.2958));
            }
        });

        pitch_results.add(curr_pitch);
        roll_results.add(curr_roll);
        pitch_results_gyro.add(curr_pitch_gyro);
        roll_results_gyro.add(curr_roll_gyro);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void calibrateAccelerometer() {
        DescriptiveStatistics acc_raw_x = new DescriptiveStatistics();
        DescriptiveStatistics acc_raw_y = new DescriptiveStatistics();
        DescriptiveStatistics acc_raw_z = new DescriptiveStatistics();

        DescriptiveStatistics gyro_raw_x = new DescriptiveStatistics();
        DescriptiveStatistics gyro_raw_y = new DescriptiveStatistics();
        DescriptiveStatistics gyro_raw_z = new DescriptiveStatistics();

        for (int i = 0; i < acc_data_raw.size(); i++) {
            acc_raw_x.addValue(acc_data_raw.get(i)[0]);
            acc_raw_y.addValue(acc_data_raw.get(i)[1]);
            acc_raw_z.addValue(acc_data_raw.get(i)[2]);
        }

        for (int i = 0; i < gyro_data_raw.size(); i++) {
            gyro_raw_x.addValue(gyro_data_raw.get(i)[0]);
            gyro_raw_y.addValue(gyro_data_raw.get(i)[1]);
            gyro_raw_z.addValue(gyro_data_raw.get(i)[2]);
        }

        Log.d(TAG, String.format("acc bias: %.5f, %.5f, %.5f, system bias: %.5f, %.5f, %.5f",
                acc_raw_x.getMean(), acc_raw_y.getMean(), acc_raw_z.getMean(),
                acc_data_raw.get(acc_data_raw.size()-1)[3], acc_data_raw.get(acc_data_raw.size()-1)[4], acc_data_raw.get(acc_data_raw.size()-1)[5]));

        accel_bias[0] = acc_raw_x.getMean();
        accel_bias[1] = acc_raw_y.getMean();
        accel_bias[2] = acc_raw_z.getMean() - GRAVITY;

        Log.d(TAG, String.format("gyro bias: %.5f, %.5f, %.5f, system bias: %.5f, %.5f, %.5f",
                gyro_raw_x.getMean(), gyro_raw_y.getMean(), gyro_raw_y.getMean(),
                gyro_data_raw.get(gyro_data_raw.size()-1)[3], gyro_data_raw.get(gyro_data_raw.size()-1)[4], gyro_data_raw.get(gyro_data_raw.size()-1)[5]));

        gyro_bias[0] = gyro_raw_x.getMean();
        gyro_bias[1] = gyro_raw_y.getMean();
        gyro_bias[2] = gyro_raw_z.getMean();
    }
}
