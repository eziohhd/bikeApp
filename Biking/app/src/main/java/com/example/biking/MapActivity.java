package com.example.biking;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener;


import java.io.IOException;
import java.util.List;
import java.io.FileOutputStream;
import java.lang.Math;
import java.io.*;
import java.util.Scanner;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;


public class MapActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        OnMyLocationButtonClickListener,
        OnMyLocationClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        SensorEventListener {

    private static final String TAG = "MapActivity";

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;


    //vars
    private Boolean mLocationPermissionsGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private SearchView searchView;
    private Location currentLocation;
    View mapView;

    Button startButton, endButton;
    Button resumeButton, pauseButton;
    KdGaugeView speedoMeterView, stopWatchView, distanceMeterView, energyMeterView;
    private int seconds = 0;
    private boolean theFirst = true;
    private float speed = 0;
    private double distance = 0;
    private double bodyWeight;//kg
    private double caloriesBurned = 0;
    private Polyline lineRoute;
    private PolylineOptions lineOptions;
    List<LatLng> points;


    SensorManager sensorManager;
    Sensor accelerometer;
    boolean collectValues = false;
    int counter; // X-axis
    int counter_yAcc;
    int counterVelocity;
    float GRAVITY_SWEDEN = 9.81666f;
    double velocity = 0;
    double tiltAngle;

    float x, y, z; // acceleration on 3 axises excluding gravity

    float x_fGravity, y_fGravity, z_fGravity;

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

    // elapsed time
    long start;
    long end;
    long elapsedTime;
    long accElapsedTime;

    // Gyroscope&gravity
    SensorManager sensorManager2;
    SensorManager sensorManager3;
    SensorManager sensorManager4;
    SensorManager sensorManager5;
    Sensor gravitySensor;
    Sensor gyroSensor;
    Sensor magneticSensor;
    Sensor rawAccSensor;

    // Common
    Common common = new Common();


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
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        // Timer starts
        start = System.currentTimeMillis();
        // Graph X-axis counter initialization so that the graph starts at zero
        counter = 0;
        counter_yAcc = 0;
        counterVelocity = 0;
        accElapsedTime = 0;

        startButton = findViewById(R.id.btnStart);
        startButton.setOnClickListener(view -> {
            collectValues = true;
            startButton.setVisibility(View.INVISIBLE);
            resumeButton.setVisibility(View.INVISIBLE);
            pauseButton.setVisibility(View.VISIBLE);
            endButton.setVisibility(View.VISIBLE);
        });

        pauseButton = findViewById(R.id.btnPause);
        resumeButton = findViewById(R.id.btnResume);
        endButton = findViewById(R.id.btnEnd);
        pauseButton.setVisibility(View.INVISIBLE);
        resumeButton.setVisibility(View.INVISIBLE);
        endButton.setVisibility(View.INVISIBLE);

        stopWatchView = findViewById(R.id.timeMeter);
        distanceMeterView = findViewById(R.id.distanceMeter);
        speedoMeterView = findViewById(R.id.speedMeter);
        energyMeterView = findViewById(R.id.energyMeter);


        pauseButton.setOnClickListener(view -> {
            if (collectValues) {
                collectValues = false;
                resumeButton.setVisibility(View.VISIBLE);
                pauseButton.setVisibility(View.INVISIBLE);
            }
        });

        resumeButton.setOnClickListener(view -> {
            if (!collectValues) {
                collectValues = true;
                pauseButton.setVisibility(View.VISIBLE);
                resumeButton.setVisibility(View.INVISIBLE);
            }
        });

        // Initializing the sensor manager with an accelerometer, and registering a listener.
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION); //
        sensorManager.registerListener((SensorEventListener) this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST); //SENSOR_DELAY_NORMAL
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

        endButton.setOnClickListener(view -> showPopupWindow(view));

        runTimer();

        searchView = findViewById(R.id.sv_location);
        getLocationPermission();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
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


            }


            if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                // Get sensor data
                x = event.values[0];
                y = event.values[1];
                z = event.values[2];


                // add data to arraylists
                accListX.add(x);
                accListY.add(y);
                accListZ.add(z);

                bGoSave = true;

                // elapsed time
                end = System.currentTimeMillis();
                elapsedTime = end - start;
                start = System.currentTimeMillis();

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
                    // velocity and distance calculation
                    if (counterZeroAcc > 9) {
                        velocity = velocity * 0.8;
                    } else {
                        for (int i = 0; i < arrayComp.length; i++) {
                            if (velocity < 0) {
                                velocity = 0;
                            } else {
                                velocity = velocity + 3.6 * arrayComp[i] / Math.cos(tiltAngle / 180 * Math.PI) * 0.005;
                            }
                            distance = distance + velocity / 3.6 * 0.005;
                            caloriesBurned = common.caloriesBurned(bodyWeight, velocity, 0.005);

                        }

                    }
                    speedoMeterView.setSpeed((float) velocity);
                    distanceMeterView.setDistance((float) distance);
                    energyMeterView.setEnergy((int) caloriesBurned);
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
//                save(DataFileRawX, array_xAccRaw);
//                save(DataFileRawY, array_yAccRaw);
//                save(DataFileRawZ, array_zAccRaw);
//                save(DataFileMadgwickX, array_xAccMadgwick);
//                save(DataFileMadgwickY, array_yAccMadgwick);
//                save(DataFileMadgwickZ, array_zAccMadgwick);
//                save(DataFileX, array_xAcc);
//                save(DataFileY, array_yAcc);
//                save(DataFileZ, array_zAcc);
//                save(DataFileGyroX, array_xGyro);
//                save(DataFileGyroY, array_yGyro);
//                save(DataFileGyroZ, array_zGyro);
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
//
//    private void save(String FILE_NAME, float[] Data) {
//        String state = Environment.getExternalStorageState();
//        if (!Environment.MEDIA_MOUNTED.equals(state)) {
//            return;
//        }
//        File file = new File(Environment.getExternalStoragePublicDirectory(
//                Environment.DIRECTORY_DOWNLOADS), FILE_NAME);
//
//        DataOutputStream fos = null;
//
//        try {
//            file.createNewFile();
//            fos = new DataOutputStream(new FileOutputStream(file, true));
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return;
//        }
//
//        for (int i = 0; i < Data.length; i++) {   //int i = 0; i < Data.length; i++)
//            String textData = String.valueOf(Data[i]) + "\n";
//
//            try {
//                //float fnumber = 9.80f;//fos.write(textData.getBytes(StandardCharsets.UTF_8)); //The vector is saved in a txt-file on the device
//                fos.writeBytes(textData);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        try {
//            fos.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
//        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: map is ready");
        mMap = googleMap;

        if (mapView != null && mapView.findViewById(Integer.parseInt("1")) != null) {
            //get the button view
            View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
            //place it on the bottom right
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                    locationButton.getLayoutParams();
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams.setMargins(0, 0, 30, 30);
        }

        if (mLocationPermissionsGranted) {
            getDeviceLocation();
            lineOptions = new PolylineOptions().width(5).color(android.R.color.holo_purple);
            lineRoute = mMap.addPolyline(lineOptions);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            mMap.setOnMyLocationButtonClickListener(this);
            mMap.setOnMyLocationClickListener(this);
            mMap.setMyLocationEnabled(true);
            init();


        }
    }

    //PopupWindow display method
    public void showPopupWindow(final View view) {

        //Create a View object yourself through inflater
        LayoutInflater inflater = (LayoutInflater) view.getContext().getSystemService(view.getContext().LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.pop_up_layout, null);

        //Specify the length and width through constants
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;

        //Make Inactive Items Outside Of PopupWindow
        boolean focusable = true;

        //Create a window with our parameters
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        //Set the location of the window on the screen
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

        //Initialize the elements of our window, install the handler
        TextView test2 = popupView.findViewById(R.id.titleText);
//        test2.setText(R.string.textTitle);

        Button cancelButton = popupView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> popupWindow.dismiss());

        Button confirmButton = popupView.findViewById(R.id.confirmButton);
        confirmButton.setOnClickListener(v -> {
            collectValues = false;
            pauseButton.setVisibility(View.INVISIBLE);
            resumeButton.setVisibility(View.INVISIBLE);
            endButton.setVisibility(View.INVISIBLE);
            startButton.setVisibility(View.VISIBLE);
            lineRoute.remove();
            seconds = 0;
            stopWatchView.setMinute(0);
            stopWatchView.setSecond(0);

            popupWindow.dismiss();
        });


    }

    private void runTimer() {
        final Handler handler = new Handler();

        handler.post(new Runnable() {
            @Override
            public void run() {
                int minutes = (seconds % 3600) / 60;
                int secs = seconds % 60;
                stopWatchView.setMinute(minutes);
                stopWatchView.setSecond(secs);
                if (collectValues) {
                    theFirst = false;
                    getDeviceLocation();
                    if (currentLocation != null && !currentLocation.equals("")) {

                        LatLng newPoint = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                        points = lineRoute.getPoints();
                        points.add(newPoint);
                        lineRoute.setPoints(points);
                    }
                }
                if (collectValues) {
                    seconds++;
                }
                handler.postDelayed(this, 1000);
            }
        });
    }


//    public void onTaskDone(Object... values) {
//        if (currentPolyline != null)
//            currentPolyline.remove();
//        currentPolyline = mMap.addPolyline((PolylineOptions) values[0]);
//    }


    private void init() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                String location = searchView.getQuery().toString();
                List<Address> addressList = null;

                if (location != null && !location.equals("")) {
                    Geocoder geocoder = new Geocoder(MapActivity.this);
                    try {
                        addressList = geocoder.getFromLocationName(location, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (addressList.size() > 0) {
                        Address address = addressList.get(0);
                        LatLng latLng1 = new LatLng(address.getLatitude(), address.getLongitude());

                        mMap.clear();
                        mMap.addMarker(new MarkerOptions().position(latLng1).title(location));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng1, 15));

                        Log.d(TAG, "geoLocate: found a location: " + address.toString());
                        //Toast.makeText(this, address.toString(), Toast.LENGTH_SHORT).show();

                    } else {
                        Toast.makeText(MapActivity.this, "Address not found", Toast.LENGTH_SHORT).show();
                    }
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
    }

    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the devices current location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionsGranted) {
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: found location!");
                            currentLocation = (Location) task.getResult();
                            if (theFirst) {
                                moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                                        DEFAULT_ZOOM);
                            }
                        } else {
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MapActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }


    private void moveCamera(LatLng latLng, float zoom) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }


    private void initMap() {
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapActivity.this);
    }

    private void getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;
                initMap();
            } else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
//        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: called.");
        mLocationPermissionsGranted = false;

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionsGranted = true;
                    //initialize our map
                    initMap();
                }
            }
        }
    }


}
