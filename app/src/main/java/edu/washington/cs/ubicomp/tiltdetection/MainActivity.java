package edu.washington.cs.ubicomp.tiltdetection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    Button startButton;
    TextView tiltText;
    GraphView accelGraph;
    GraphView gyroGraph;
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private Sensor accelerometer;

    private ArrayList<LineGraphSeries<DataPoint>> acc_display_raw = new ArrayList<>();
    private ArrayList<LineGraphSeries<DataPoint>> acc_display_calibrated = new ArrayList<>();
    private ArrayList<ArrayList<DataPoint>> acc_result_raw = new ArrayList<>();
    private ArrayList<ArrayList<DataPoint>> acc_result_calibrated = new ArrayList<>();

    private ArrayList<LineGraphSeries<DataPoint>> gryo_display_raw = new ArrayList<>();
    private ArrayList<LineGraphSeries<DataPoint>> gryo_display_calibrated = new ArrayList<>();
    private ArrayList<ArrayList<DataPoint>> gryo_result_raw = new ArrayList<>();
    private ArrayList<ArrayList<DataPoint>> gryo_result_calibrated = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        startButton = findViewById(R.id.start_button);
        tiltText = findViewById(R.id.tilt_text);
        accelGraph = findViewById(R.id.graph_acc);
        gyroGraph = findViewById(R.id.graph_gyro);

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
