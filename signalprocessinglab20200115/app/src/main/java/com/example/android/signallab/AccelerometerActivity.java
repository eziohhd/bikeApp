package com.example.android.signallab;


import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.FileOutputStream;
import java.lang.Math;
import java.io.*;
import java.util.Scanner;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class AccelerometerActivity extends AppCompatActivity implements SensorEventListener {

    GraphView graph;
    TextView xVal, yVal, zVal, tiltVal, vVal, textContent;
    TextView mxVal,myVal,mzVal,gxVal,gyVal,gzVal;
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
    double aX, aY, aZ, mX, mY, mZ;// for madgwick filter
    float x_fGravity, y_fGravity, z_fGravity;
    float x_fGyro, y_fGyro, z_fGyro; // Gyroscope
    int nBufferSize = 300; // get average of y_axix acceleration
    int nMovMeanSize = 100;
    double dArrayXacc[] = new double[nBufferSize];
    double dArrayYacc[] = new double[nBufferSize];
    double dArrayZacc[] = new double[nBufferSize];
    float array_yAcc[];
    float array_xAcc[];
    float array_zAcc[];
    float array_yAccMadgwick[];
    float array_xAccMadgwick[];
    float array_zAccMadgwick[];
    float array_yAccRaw[];
    float array_xAccRaw[];
    float array_zAccRaw[];
    float array_xGyro[];
    float array_yGyro[];
    float array_zGyro[];
    float avg_yAcc;

    // elapsed time
    long start;
    long end;
    long elapsedTime;
    long accElapsedTime;

    // Gyroscope&graavity
    SensorManager sensorManager2;
    SensorManager sensorManager3;
    SensorManager sensorManager4;
    SensorManager sensorManager5;
    Sensor gravitySensor;
    Sensor gyroSensor;
    Sensor magneticSensor;
    Sensor rawAccSensor;

    // MadgwickFilter
    MadgwickFilter madgwickFilter = new MadgwickFilter();

    // Common
    Common common = new Common();

    // txt save
    private static final String DataFileY = "BufferDataY.txt"; //Name of the file to which the data is exported
    private static final String DataFileX = "BufferDataX.txt"; //Name of the file to which the data is exported
    private static final String DataFileZ = "BufferDataZ.txt"; //Name of the file to which the data is exported
    private static final String DataFileRawY = "BufferDataRawY.txt"; //Name of the file to which the data is exported
    private static final String DataFileRawX = "BufferDataRawX.txt"; //Name of the file to which the data is exported
    private static final String DataFileRawZ = "BufferDataRawZ.txt"; //Name of the file to which the data is exported
    private static final String DataFileMadgwickY = "BufferDataMadgwickY.txt"; //Name of the file to which the data is exported
    private static final String DataFileMadgwickX = "BufferDataMadgwickX.txt"; //Name of the file to which the data is exported
    private static final String DataFileMadgwickZ = "BufferDataMadgwickZ.txt"; //Name of the file to which the data is exported
    private static final String DataFileGyroX = "BufferDataGyroX.txt"; //Name of the file to which the data is exported
    private static final String DataFileGyroY = "BufferDataGyroY.txt"; //Name of the file to which the data is exported
    private static final String DataFileGyroZ = "BufferDataGyroZ.txt"; //Name of the file to which the data is exported
    byte[] workBuffer = new byte[nBufferSize * 4];
    boolean bSave = true;

    // IIR filter removing gravity
    float gravity[] = new float[3]; // global variable in onCreate

    // arraylists
    ArrayList<Float> accListX = new ArrayList<Float>();
    ArrayList<Float> accListY = new ArrayList<Float>();
    ArrayList<Float> accListZ = new ArrayList<Float>();
    ArrayList<Float> accListMadgwickX = new ArrayList<Float>();
    ArrayList<Float> accListMadgwickY = new ArrayList<Float>();
    ArrayList<Float> accListMadgwickZ = new ArrayList<Float>();
    ArrayList<Float> accListRawX = new ArrayList<Float>();
    ArrayList<Float> accListRawY = new ArrayList<Float>();
    ArrayList<Float> accListRawZ = new ArrayList<Float>();
    ArrayList<Float> gyroListX = new ArrayList<Float>();
    ArrayList<Float> gyroListY = new ArrayList<Float>();
    ArrayList<Float> gyroListZ = new ArrayList<Float>();
    boolean bGoSave = false;


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
        textContent = findViewById(R.id.textContentView);

        mxVal=findViewById(R.id.mx);
        myVal=findViewById(R.id.my);
        mzVal=findViewById(R.id.mz);

        gxVal = findViewById(R.id.gx);
        gyVal = findViewById(R.id.gy);
        gzVal = findViewById(R.id.gz);






        // Initializing the sensor manager with an accelerometer, and registering a listener.
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION); //
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST); //SENSOR_DELAY_NORMAL
        // Gyroscope initialization
        sensorManager2 = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyroSensor = sensorManager2.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager2.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_FASTEST);// SENSOR_DELAY_NORMAL
        // Gravity initialization
        sensorManager3 = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gravitySensor = sensorManager3.getDefaultSensor(Sensor.TYPE_GRAVITY);
        sensorManager3.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_FASTEST);// SENSOR_DELAY_NORMAL
        // Gravity initialization
        sensorManager4 = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        magneticSensor = sensorManager4.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager4.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_FASTEST);// SENSOR_DELAY_NORMAL

        // Raw accelerometer sensor initialization
        sensorManager5 = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        rawAccSensor = sensorManager5.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager5.registerListener(this, rawAccSensor, SensorManager.SENSOR_DELAY_FASTEST);// SENSOR_DELAY_NORMAL


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

        // file read

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
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                x_fGyro = event.values[0];
                y_fGyro = event.values[1];
                z_fGyro = event.values[2];

                // add data to arraylists
                gyroListX.add(x_fGyro);
                gyroListY.add(y_fGyro);
                gyroListZ.add(z_fGyro);

                gxVal.setText(String.valueOf(x_fGyro));
                gyVal.setText(String.valueOf(y_fGyro));
                gzVal.setText(String.valueOf(z_fGyro));


            }
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                // Get sensor data
                mX = event.values[0];
                mY = event.values[1];
                mZ = event.values[2];

                mxVal.setText(String.valueOf(mX));
                myVal.setText(String.valueOf(mY));
                mzVal.setText(String.valueOf(mZ));

            }
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                // Get sensor data
                aX = event.values[0];
                aY = event.values[1];
                aZ = event.values[2];
                // fliter
                madgwickFilter.filterUpdatedouble(aX, aY, aZ, (double) x_fGyro, (double) y_fGyro, (double) z_fGyro, mX, mY, mZ);

                double[] compensatedGravity = GravityCompensation.CompensateGravity
                        (new double[]{aX, aY, aZ}, madgwickFilter.getQuaternions());

                // Update the text view
                xVal.setText(String.valueOf(compensatedGravity[0]));
                yVal.setText(String.valueOf(compensatedGravity[1]));
                zVal.setText(String.valueOf(compensatedGravity[2]));

                // add data to arraylists
                accListMadgwickX.add((float) compensatedGravity[0]);
                accListMadgwickY.add((float) compensatedGravity[1]);
                accListMadgwickZ.add((float) compensatedGravity[2]);
                accListRawX.add((float) aX);
                accListRawY.add((float) aY);
                accListRawZ.add((float) aZ);

                bGoSave = true;


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
                // display speed
//                vVal.setText(String.valueOf(velocity));

                // add data to arraylists
                accListX.add(x);
                accListY.add(y);
                accListZ.add(z);

                bGoSave = true;

                // elapsed time
                end = System.currentTimeMillis();
                elapsedTime = end - start;
                start = System.currentTimeMillis();
                vVal.setText(String.valueOf(elapsedTime));

                counter_yAcc++;

                // buffer y_axis acceleration data for further processing
                if ((!(counter == 1)) && (counter_yAcc < nBufferSize)) {   // First elapsed time is not accurate
                    //accElapsedTime = accElapsedTime + elapsedTime;
                    dArrayXacc[counter_yAcc] = x;
                    dArrayYacc[counter_yAcc] = y;
                    dArrayZacc[counter_yAcc] = z;

                } else if (!(counter == 1)) // The data size has been reached

                {
                    int counterZeroAcc = 0;
                    double[] arrayComp;
                    double[] movMeanOut = common.calculateMovingAverage(dArrayYacc, nMovMeanSize); // nMovMeanSize
                    arrayComp = common.accCompensation(movMeanOut);

                    for (int i = 0; i < arrayComp.length; i++) {
                        if (i < arrayComp.length - 10) {
                            counterZeroAcc = 0;
                            for (int k = i; k < i + 10; k++) {
                                if (Math.abs(movMeanOut[k] - movMeanOut[k + 1]) < 0.02) {
                                    counterZeroAcc++;
                                }
                            }
                        }
                    }

                    if (counterZeroAcc > 9) {
                        velocity = velocity * 0.8;
                    } else {
                        for (int i = 0; i < arrayComp.length; i++) {
                            velocity = velocity + 3.6 * arrayComp[i] / Math.cos(tiltAngle/180*Math.PI) * 0.005;
                        }
                    }
                    //counter clear
                    counter_yAcc = 0;

                }



            }

        } else if (bGoSave) {
            if (bSave) {
                array_xAccMadgwick = new float[accListMadgwickX.size()];
                array_yAccMadgwick = new float[accListMadgwickY.size()];
                array_zAccMadgwick = new float[accListMadgwickZ.size()];
                array_yAccRaw = new float[accListRawY.size()];
                array_zAccRaw = new float[accListRawZ.size()];
                array_xAccRaw = new float[accListRawX.size()];
                array_yAccRaw = new float[accListRawY.size()];
                array_zAccRaw = new float[accListRawZ.size()];
                array_xAcc = new float[accListX.size()];
                array_yAcc = new float[accListY.size()];
                array_zAcc = new float[accListZ.size()];
                array_xGyro = new float[gyroListX.size()];
                array_yGyro = new float[gyroListY.size()];
                array_zGyro = new float[gyroListZ.size()];

                for (int i = 0; i < accListMadgwickX.size(); i++) {
                    array_xAccMadgwick[i] = accListMadgwickX.get(i);
                    array_yAccMadgwick[i] = accListMadgwickY.get(i);
                    array_zAccMadgwick[i] = accListMadgwickZ.get(i);
                }

                for (int i = 0; i < accListRawX.size(); i++) {
                    array_xAccRaw[i] = accListRawX.get(i);
                    array_yAccRaw[i] = accListRawY.get(i);
                    array_zAccRaw[i] = accListRawZ.get(i);
                }

                for (int i = 0; i < accListX.size(); i++) {
                    array_xAcc[i] = accListX.get(i);
                    array_yAcc[i] = accListY.get(i);
                    array_zAcc[i] = accListZ.get(i);
                }

                for (int i = 0; i < gyroListX.size(); i++) {
                    array_xGyro[i] = gyroListX.get(i);
                    array_yGyro[i] = gyroListY.get(i);
                    array_zGyro[i] = gyroListZ.get(i);
                }
                // save data to txt
                save(DataFileRawX, array_xAccRaw);
                save(DataFileRawY, array_yAccRaw);
                save(DataFileRawZ, array_zAccRaw);
                save(DataFileMadgwickX, array_xAccMadgwick);
                save(DataFileMadgwickY, array_yAccMadgwick);
                save(DataFileMadgwickZ, array_zAccMadgwick);
                save(DataFileX, array_xAcc);
                save(DataFileY, array_yAcc);
                save(DataFileZ, array_zAcc);
                save(DataFileGyroX, array_xGyro);
                save(DataFileGyroY, array_yGyro);
                save(DataFileGyroZ, array_zGyro);
                bSave = false;
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
        sensorManager3.unregisterListener(this);
    }

    protected void onResume() {
        // register listener again
        super.onResume();
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                SensorManager.SENSOR_DELAY_FASTEST);   //SENSOR_DELAY_NORMAL
        sensorManager2.registerListener(this,
                sensorManager2.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST); //SENSOR_DELAY_NORMAL
        sensorManager3.registerListener(this,
                sensorManager3.getDefaultSensor(Sensor.TYPE_GRAVITY),
                SensorManager.SENSOR_DELAY_FASTEST); //SENSOR_DELAY_NORMAL
        sensorManager4.registerListener(this,
                sensorManager4.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST); //SENSOR_DELAY_NORMAL

    }

    private void save(String FILE_NAME, float[] Data) {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return;
        }
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), FILE_NAME);

        DataOutputStream fos = null;

        try {
            file.createNewFile();
            fos = new DataOutputStream(new FileOutputStream(file, true));

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        for (int i = 0; i < Data.length; i++) {   //int i = 0; i < Data.length; i++)
            String textData = String.valueOf(Data[i]) + "\n";

            try {
                //float fnumber = 9.80f;//fos.write(textData.getBytes(StandardCharsets.UTF_8)); //The vector is saved in a txt-file on the device
                fos.writeBytes(textData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static byte[] getByteArrayFromByteBuffer(ByteBuffer byteBuffer) {
        byte[] bytesArray = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytesArray, 0, bytesArray.length);
        return bytesArray;
    }

    public static byte[] float2ByteArray(float value) {
        return ByteBuffer.allocate(4).putFloat(value).array();
    }


}

