package edu.erau.mad.trb.flightdatarecorder;
/* AsyncMapDatabasePlotter.java
 * SE395A Final Project
 * by Thomas Bassa
 * A Java class to handle querying flight details asynchronously from loading
 * the map view. */

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

/** An asynchronous task that will read the flight data for a specific flight
 * from FlightLogDatabase and plot the markers on a GoogleMap. */
public class AsyncMapDatabasePlotter extends AsyncTask<Void, LatLng,
        ArrayList<LatLng>> {
    // https://developer.android.com/reference/android/os/AsyncTask.html

    /** The database to read from */
    private final FlightLogDatabase database;

    /** The ID of the flight to get points from */
    private final long flightID;
    /** The GoogleMap to plot points on */
    private final GoogleMap map;

    /** The set of points, in order, as returned from the database */
    private final ArrayList<LatLng> coords = new ArrayList<>();

    /** The bounds of all the points. */
    private LatLngBounds bounds;

    /** Semaphore used to ensure the GoogleMap is ready before panning with
     * animateCamera */
    private final Semaphore mapWait = new Semaphore(0);

    /** Task constructor; simply initializes fields. Call execute to start the task.
     * @param id the flight ID to plot points for
     * @param googleMap a map to plot the points on
     * @param c a Context used to retrieve the database manager */
    public AsyncMapDatabasePlotter(long id, GoogleMap googleMap, Context c) {
        flightID = id;
        map = googleMap;

        //Wait for the map to finish loading.
        //This will block the background thread until complete.
        map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                mapWait.release();
            }
        });

        database = FlightLogDatabase.getInstance(c);
    }

    //This is the background task itself, which runs on its own thread.
    @Override
    protected ArrayList<LatLng> doInBackground(Void... params) {
        Cursor flightLogPoints = database.getFlightPositionsForFlight(flightID);
        //If there are any points...
        if(flightLogPoints.moveToFirst()) {
            int latiCol = flightLogPoints.getColumnIndexOrThrow
                    (FlightLogDatabase.COL_LATI);
            int longiCol = flightLogPoints.getColumnIndexOrThrow
                    (FlightLogDatabase.COL_LONGI);
            //Iterate through them all!
            do {
                double lati, longi;
                lati = flightLogPoints.getDouble(latiCol);
                longi = flightLogPoints.getDouble(longiCol);
                LatLng point = new LatLng(lati, longi);

                coords.add(point);
                //Push this point to the map, while we're at it.
                publishProgress(point);
            } while(flightLogPoints.moveToNext());
        }
        //Close the database connection
        flightLogPoints.close();

        //Calculate the bounds of the points (to get a nice camera zoom)
        final LatLngBounds.Builder boundBuilder = new LatLngBounds.Builder();
        for(final LatLng point : coords) {
            boundBuilder.include(point);
        }
        bounds = boundBuilder.build();

        //Wait for the map to finish loading (the zoom crashes if map isn't ready)
        try {
            mapWait.acquire();
        } catch (InterruptedException ignored) {}

        return coords;
    }

    //Progress update; runs on UI thread
    //Internally invoked from publishProgress
    //This updates the provided map with a new coordinate point once loaded.
    @Override
    protected void onProgressUpdate(LatLng... values) {
        map.addMarker(new MarkerOptions().position(values[0]));
    }

    //Runs on UI thread after task completes
    //Here, a polyline is drawn, and
    //the camera is moved to encompass the bounds of all the points.
    @Override
    protected void onPostExecute(ArrayList<LatLng> latLngs) {
        //3 and 20 are pixel sizes; arbitrary polyline width and camera padding
        map.addPolyline(new PolylineOptions().addAll(latLngs).width(3.0f));
        //animateCamera crashes if the map isn't ready (hence the semaphore earlier)
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 20));
    }
}
