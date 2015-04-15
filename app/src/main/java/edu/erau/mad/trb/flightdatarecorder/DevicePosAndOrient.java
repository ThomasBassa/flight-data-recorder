package edu.erau.mad.trb.flightdatarecorder;
/* TODO Redocument DevicePosAndOrient
 * SE395A
 * by Thomas Bassa
 * A Java class to manage the position & orientation of an Android device. */

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

/** A class to manage the position and orientation of a device in space. */
public class DevicePosAndOrient implements SensorEventListener, LocationListener {
    private static final int US_PER_SECOND = 1000000;
    private static final long MS_PER_SECOND = 1000L;

    /** Location provider. TODO Use GPS or Google location services */
    private static final String LOC_PROVIDER = LocationManager.NETWORK_PROVIDER;

    //TODO Lat/long format needs to be ddd° mm’ ss’’
    private static final String LAT_LONG_FORMAT = "%2.4f %s°";

    /* TODO remove constants re: storage
    private static final String LATI_STORAGE = "edu.erau.mad.sensormapper.LATI";
    private static final String LONGI_STORAGE = "edu.erau.mad.sensormapper.LONGI";
    private static final float LEHMAN_LATI = 29.1893f;
    private static final float LEHMAN_LONGI = -81.0470f;
    */

    //Sensors
    private SensorManager sensorManager;
    private Sensor senseMag = null;
    private Sensor senseAccel = null;

    private float[] rotationMatrix = new float[9];
    private float[] magValues = null;
    private float[] accelValues = null;

    private float[] orientValues = new float[3];

    /** Track whether the device has the sensors required for orientation.
     * Assume false until the sensors have been retrieved from the SensorManager */
    private boolean sensorsPresent = false;

    //Location
    private LocationManager locationManager;

    private double lati;
    private double longi;

    //TODO remove Last known location handling
    // https://developer.android.com/guide/topics/data/data-storage.html
    //private SharedPreferences prefs;

    public DevicePosAndOrient(Activity hostActivity) {
        //Get the sensor manager & sensors
        sensorManager = (SensorManager) hostActivity.getSystemService(Context
                .SENSOR_SERVICE);

        senseMag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        senseAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if(senseMag != null && senseAccel != null) sensorsPresent = true;

        //Location management
        locationManager = (LocationManager) hostActivity.getSystemService(Context
                .LOCATION_SERVICE);

        /* TODO remove last known init
        //Get a handle to prefs data for last known location
        prefs = hostActivity.getPreferences(Context.MODE_PRIVATE);

        //Restore the last known loc data, or use Lehman coords.
        lati = prefs.getFloat(LATI_STORAGE, LEHMAN_LATI);
        longi = prefs.getFloat(LONGI_STORAGE, LEHMAN_LONGI);
        */
    }

    /** Begin listening to sensors & location data */
    public void startListening() {
        if(sensorsPresent) {
            sensorManager.registerListener(this, senseMag, US_PER_SECOND);
            sensorManager.registerListener(this, senseAccel, US_PER_SECOND);
        }
        locationManager.requestLocationUpdates(LOC_PROVIDER, MS_PER_SECOND, 1f, this);
    }

    /** Stop listening to sensors & location, to save battery
     * Also save the last known location (to the app) */
    public void stopListening() {
        sensorManager.unregisterListener(this);
        locationManager.removeUpdates(this);

        /* TODO remove last known save
        //Save last known loc
        SharedPreferences.Editor save = prefs.edit();
        save.putFloat(LATI_STORAGE, (float) lati);
        save.putFloat(LONGI_STORAGE, (float) longi);
        save.apply();
        */
    }

    /** Get the last known latitude of the device, signed. */
    public double getLatitude() {
        return lati;
    }

    /** Get the last known longitude of the device, signed. */
    public double getLongitude() {
        return longi;
    }

    /** Get a formatted numeric string with the latitude,
     * to four decimal places, referencing north or south rather than a sign. */
    public String getNiceLatitude() {
        String latiCompass = lati >= 0f ? "N" : "S";
        return String.format(LAT_LONG_FORMAT, Math.abs(lati), latiCompass);
    }

    /** Get a formatted numeric string with the longitude,
     * to four decimal places, referencing east or west rather than a sign. */
    public String getNiceLongitude() {
        String longiCompass = longi >= 0f ? "E" : "W";
        return String.format(LAT_LONG_FORMAT, Math.abs(longi), longiCompass);
    }

    /** Get the azimuth (bearing) of the device in degrees.
     * @see android.hardware.SensorManager#getOrientation(float[], float[]) */
    public double getAz() {
        return Math.toDegrees(orientValues[0]);
    }


    /** Get the pitch of the device in degrees.
     * @see android.hardware.SensorManager#getOrientation(float[], float[]) */
    public double getPitch() {
        return Math.toDegrees(orientValues[1]);
    }

    /** Get the roll of the device in degrees.
     * @see android.hardware.SensorManager#getOrientation(float[], float[]) */
    public double getRoll() {
        return Math.toDegrees(orientValues[2]);
    }

    /** Take a degrees value and format it nicely. */
    public static String formatDegValue(double value) {
        //TODO Roll, pitch, and yaw are all to 2 decimal place precision
        return String.format("% 3.3f°", value);
    }

    //Methods implemented from SensorEventListener
    //Fired whenever one of the sensors changes. This class only registers for
    // sensor updates when both a compass & geomagnetic sensor are present.
    @Override
    public void onSensorChanged(SensorEvent event) {
        //Useful notes & references
        // https://stackoverflow.com/questions/4819626/
        //  android-phone-orientation-overview-including-compass

// https://developer.android.com/reference/android/hardware/SensorManager.html#
// getRotationMatrix%28float%5B%5D,%20float%5B%5D,%20float%5B%5D,%20float%5B%5D%29

        //Address compare should be OK since these were specifically registered...
        if (event.sensor == senseMag) {
            magValues = event.values.clone();
        } else if (event.sensor == senseAccel) {
            accelValues = event.values.clone();
        } //no else

        //If we have data for both sensors...
        if ((magValues != null) && (accelValues != null)) {
            //Get the rotation matrix from the sensor data
            boolean gotRotation = SensorManager.getRotationMatrix
                    (rotationMatrix, null, accelValues, magValues);

            //If successful, update orientation
            if (gotRotation) {
                SensorManager.getOrientation(rotationMatrix, orientValues);
            }
        }
    }

    //Don't care about accuracy changes
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    //Methods implemented from LocationListener
    //Called when the location manager senses a change. Simple.
    @Override
    public void onLocationChanged(Location loc) {
        lati = loc.getLatitude();
        longi = loc.getLongitude();
    }

    //Don't care about the next 3 methods...
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}
}