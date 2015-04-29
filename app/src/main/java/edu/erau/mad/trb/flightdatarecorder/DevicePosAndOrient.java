package edu.erau.mad.trb.flightdatarecorder;
/* DevicePosAndOrient.java
 * SE395A Final Project
 * by Thomas Bassa
 * A Java class to manage the position & orientation of an Android device. */

import android.content.Context;
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

    /** Location provider. */
    private static final String LOC_PROVIDER = LocationManager.GPS_PROVIDER;

    //Sensors
    private final SensorManager sensorManager;
    private Sensor senseMag = null;
    private Sensor senseAccel = null;

    private final float[] rotationMatrix = new float[9];
    private float[] magValues = null;
    private float[] accelValues = null;

    /** This array holds the final orientation values, in azimuth, pitch,
     * and roll order */
    private final float[] orientValues = new float[3];

    /** Track whether the device has the sensors required for orientation.
     * Assume false until the sensors have been retrieved from the SensorManager */
    private boolean sensorsPresent = false;

    //Location
    private final LocationManager locationManager;

    private double lati = 0.0;
    private double longi = 0.0;
    private double altitude = 0.0;

    /** Create a DevicePosAndOrient object, using the Context to retrieve
     * system services (sensors, location) */
    public DevicePosAndOrient(Context hostContext) {
        //Get the sensor manager & sensors
        sensorManager = (SensorManager) hostContext.getSystemService(Context
                .SENSOR_SERVICE);

        senseMag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        senseAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if(senseMag != null && senseAccel != null) sensorsPresent = true;

        //Location management
        locationManager = (LocationManager) hostContext.getSystemService(Context
                .LOCATION_SERVICE);
    }

    /** Begin listening to sensors & location data */
    public void startListening() {
        if(sensorsPresent) {
            sensorManager.registerListener(this, senseMag, US_PER_SECOND);
            sensorManager.registerListener(this, senseAccel, US_PER_SECOND);
        }
        locationManager.requestLocationUpdates(LOC_PROVIDER, MS_PER_SECOND, 1f, this);
    }

    /** Stop listening to sensors & location, to save battery */
    public void stopListening() {
        sensorManager.unregisterListener(this);
        locationManager.removeUpdates(this);
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
     * referencing north or south rather than a sign. */
    public String getNiceLatitude() {
        return formatLatitude(lati);
    }

    /** Get a formatted numeric string with the longitude,
     * referencing east or west rather than a sign. */
    public String getNiceLongitude() {
        return formatLongitude(longi);
    }

    /** Format a latitude in ddd° mm’ ss’’ C format,
     * aka degrees, minutes, and seconds, with a compass letter. */
    public String formatLatitude(double lati) {
        String latiCompass = lati >= 0f ? "N" : "S";
        return formatDecDegToDegMinSec(Math.abs(lati), latiCompass);
    }


    /** Format a longitude in ddd° mm’ ss’’ C format,
     * aka degrees, minutes, and seconds, with a compass letter. */
    public String formatLongitude(double longi) {
        String longiCompass = longi >= 0f ? "E" : "W";
        return formatDecDegToDegMinSec(Math.abs(longi), longiCompass);
    }

    /** Format a decimal geographic coordinate in ddd° mm’ ss’’ C format,
     * aka degrees, minutes, and seconds, with a compass letter.
     * @param decDegrees a decimal geographic coordinate (positive lati/longi)
     * @param compass a letter for the compass heading of this coordinate
     * @return a formatted string in the form of ddd° mm’ ss’’ C */
    private static String formatDecDegToDegMinSec(double decDegrees,
                                                  String compass) {
        //Each of these operations is flooring, so casting works perfectly.

        /* http://geography.about.com/library/howto/htdegrees.htm
        The whole units of degrees will remain the same (i.e. in 121.135° longitude, start with 121°).
        Multiply the decimal by 60 (i.e. .135 * 60 = 8.1).
        The whole number becomes the minutes (8').
        Take the remaining decimal and multiply by 60. (i.e. .1 * 60 = 6).
        The resulting number becomes the seconds (6"). */
        int degrees = (int) decDegrees;
        decDegrees = (decDegrees - degrees) * 60.0;
        int minutes = (int) decDegrees;
        decDegrees = (decDegrees - minutes) * 60.0;
        int seconds = (int) decDegrees;

        return String.format("%03d° %02d’ %02d’’ %s", degrees, minutes, seconds,
                compass);
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

    //TODO doc getAltitude
    public double getAltitude() {
        return altitude;
    }

    public String getNiceAltitude() {
        return String.format("%2.2f ft.", altitude);
    }

    /** Take a degrees value for roll/pitch/yaw and format it nicely. */
    public static String formatDegValue(double value) {
        return String.format("% 3.2f°", value);
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
        altitude = loc.getAltitude();
    }

    //Don't care about the next 3 methods...
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    //TODO should probably account for provider enable/disable...
    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}
}