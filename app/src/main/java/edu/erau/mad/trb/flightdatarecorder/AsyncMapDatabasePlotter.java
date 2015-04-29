package edu.erau.mad.trb.flightdatarecorder;

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

//TODO document AsyncMapDatabasePlotter
public class AsyncMapDatabasePlotter extends AsyncTask<Void, LatLng,
        ArrayList<LatLng>> {
    // https://developer.android.com/reference/android/os/AsyncTask.html

    private final FlightLogDatabase database;

    private final long flightID;
    private final GoogleMap map;

    private final ArrayList<LatLng> coords = new ArrayList<>();

    private LatLngBounds bounds;

    private final Semaphore mapWait = new Semaphore(0);

    public AsyncMapDatabasePlotter(long id, GoogleMap googleMap, Context c) {
        flightID = id;
        map = googleMap;

        //Wait for the map to finish loading.
        map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                mapWait.release();
            }
        });

        database = FlightLogDatabase.getInstance(c);
    }

    //The actual background task itself, runs on its own thread
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
                publishProgress(point);
            } while(flightLogPoints.moveToNext());
        }
        flightLogPoints.close();

        final LatLngBounds.Builder boundBuilder = new LatLngBounds.Builder();
        for(final LatLng point : coords) {
            boundBuilder.include(point);
        }
        bounds = boundBuilder.build();

        //Wait for the map to finish loading...
        try {
            mapWait.acquire();
        } catch (InterruptedException ignored) {}

        return coords;
    }

    //Progress update; runs on UI thread
    //Internally invoked from publishProgress
    @Override
    protected void onProgressUpdate(LatLng... values) {
        map.addMarker(new MarkerOptions().position(values[0]));
    }

    //Runs on UI thread after task completes
    @Override
    protected void onPostExecute(ArrayList<LatLng> latLngs) {
        //TODO consider centering on the bounds prior to this somehow
        map.addPolyline(new PolylineOptions().addAll(latLngs).width(3.0f));
        //20 is the # pixels padding; arbitrary
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 20));
    }
}
