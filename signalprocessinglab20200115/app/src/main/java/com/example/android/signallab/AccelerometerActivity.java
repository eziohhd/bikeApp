package com.example.android.signallab;


import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.lang.Math;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class AccelerometerActivity extends AppCompatActivity implements SensorEventListener {

    GraphView graph;
    TextView xVal, yVal, zVal, tiltVal, vVal;
    SensorManager sensorManager;
    Sensor accelerometer;
    LineGraphSeries<DataPoint> seriesX, seriesY, seriesZ;
    Button startButton;
    boolean collectValues = false;
    int counter; // X-axis
    int counter_yAcc;
    int counterVelocity;
    float GRAVITY_SWEDEN = 9.81666f;
    double velocity = 0;
    double tiltAngle;

    float x, y, z; // acceleration on 3 axises excluding gravity
    float x_fGravity, y_fGravity, z_fGravity;
    int nBufferSize = 100; // get average of y_axix acceleration
    float array_yAcc[] = new float[nBufferSize];
    float avg_yAcc;

    // elapsed time
    long start;
    long end;
    long elapsedTime;
    long accElapsedTime;

    // Gyroscope
    SensorManager sensorManager2;
    Sensor gravitySensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accelerometer);
        // Timer starts
        start = System.currentTimeMillis();
        // Graph X-axis counter initialization so that the graph starts at zero
        counter = 0;
        counter_yAcc = 0;
        counterVelocity = 0;
        accElapsedTime = 0;

        // Initialize button and set a listener to activate measurement upon clicking on the button
        startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            // Simply sets a boolean to true, to start collect values from sensor.
            // The sensor always listens, but won't do anything unless collectValues = true in our case
            @Override
            public void onClick(View view) {
                if (!collectValues) {
                    collectValues = true;
                    startButton.setText("Stop measuring");
                } else {
                    collectValues = false;
                    startButton.setText("Start measuring");
                }
            }
        });

        // Views mapping, connecting variables to the layout
        graph = findViewById(R.id.graph);
        xVal = findViewById(R.id.xValueView);
        yVal = findViewById(R.id.yValueView);
        zVal = findViewById(R.id.zValueView);
        tiltVal = findViewById(R.id.tiltView);
        vVal = findViewById(R.id.vValueView);

        // Initializing the sensor manager with an accelerometer, and registering a listener.
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL); //SENSOR_DELAY_NORMAL
        // Gyroscope initialization
        sensorManager2 = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gravitySensor = sensorManager2.getDefaultSensor(Sensor.TYPE_GRAVITY);
        sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_NORMAL);// SENSOR_DELAY_NORMAL


        // Setting up initialized datapoints for each series of data
        // (x-values, y-values and z-values) with thier respective colors
        seriesX = new LineGraphSeries<>(new DataPoint[]{
                new DataPoint(0, 0),
        });
        seriesX.setColor(Color.RED);
        graph.addSeries(seriesX);

        seriesY = new LineGraphSeries<>(new DataPoint[]{
                new DataPoint(0, 0),
        });
        seriesY.setColor(Color.GREEN);
        graph.addSeries(seriesY);

        seriesZ = new LineGraphSeries<>(new DataPoint[]{
                new DataPoint(0, 0),
        });
        seriesZ.setColor(Color.BLUE);
        graph.addSeries(seriesZ);

    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        if (collectValues) {

            // Move along X-axis
            counter++;

            if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
                // Get sensor data
                x_fGravity = event.values[0];
                y_fGravity = event.values[1];
                z_fGravity = event.values[2];
                tiltAngle = 180 * Math.asin(y_fGravity / GRAVITY_SWEDEN) / Math.PI;
                tiltVal.setText(String.valueOf(tiltAngle));

            }

            if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                // Get sensor data
                x = event.values[0];
                y = event.values[1];
                z = event.values[2];
                // Update the text view
                xVal.setText(String.valueOf(x));
                yVal.setText(String.valueOf(y));
                zVal.setText(String.valueOf(z));
                // elapsed time
                end = System.currentTimeMillis();
                elapsedTime = end - start;
                start = System.currentTimeMillis();

                // without buffer
                counterVelocity++;
                velocity = velocity + y / Math.cos(tiltAngle) * (elapsedTime / 1000.0f);
                vVal.setText(String.valueOf(elapsedTime));
                // Add data to series
//                seriesX.appendData(new DataPoint(counterVelocity, y), true, 100, false);
//                graph.addSeries(seriesX);
                if (counterVelocity < 50) {
                    seriesX.appendData(new DataPoint(counterVelocity, y), true, 100, false);
                    graph.addSeries(seriesX);
                    //seriesX.resetData(new DataPoint[]{new DataPoint(0, 0)});
                    //counterVelocity = 0;
                }


//                // buffer y_axis acceleration data for further processing
//                if ((!(counter == 1)) && (counter_yAcc < nBufferSize)) {   // First elapsed time is not accurate
//                    accElapsedTime = accElapsedTime + elapsedTime;
//                    array_yAcc[counter_yAcc] = y;
//                    counter_yAcc++;
//                } else if (!(counter == 1)) // calculate mean value of y_axis acceleration data buffer
//                {
//                    float acc_yAcc = 0;
//                    for (int i = 0; i < nBufferSize; i++) {
//                        acc_yAcc = acc_yAcc + array_yAcc[i];
//                    }
//                    avg_yAcc = acc_yAcc / nBufferSize;
//                    velocity = velocity + avg_yAcc / Math.cos(tiltAngle) * (accElapsedTime / 1000.0f);
//                    counterVelocity++;
//                    // velocity = velocity + y / Math.cos(tiltAngle) * (elapsedTime / 1000.0f);
//                    vVal.setText(String.valueOf(accElapsedTime));
//                    // Add data to series
//                    seriesX.appendData(new DataPoint(counterVelocity, avg_yAcc), true, 100, false);
//                    // Add series to graph
//                    graph.addSeries+=+++++++++++++++++++++++++++++++++++++++////////////////////////////////////////////////////////
//                    (seriesX);
//                    if (counterVelocity > 60) {
//                        seriesX.resetData(new DataPoint[]{new DataPoint(0, 0)});
//                        counterVelocity = 0;
//                    }
//                    // reset
//                    accElapsedTime = 0;
//                    counter_yAcc = 0;
//                }


            }

        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Auto-generated method.
    }

    protected void onPause() {
        // unregister listener
        super.onPause();
        sensorManager.unregisterListener(this);
        sensorManager2.unregisterListener(this);
    }

    protected void onResume() {
        // register listener again
        super.onResume();
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                SensorManager.SENSOR_DELAY_NORMAL);   //SENSOR_DELAY_NORMAL
        sensorManager2.registerListener(this,
                sensorManager2.getDefaultSensor(Sensor.TYPE_GRAVITY),
                SensorManager.SENSOR_DELAY_NORMAL); //SENSOR_DELAY_NORMAL

    }
}