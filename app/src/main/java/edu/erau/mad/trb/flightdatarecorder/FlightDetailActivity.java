package edu.erau.mad.trb.flightdatarecorder;
/* FlightDetailActivity.java
 * SE395A Final Project
 * by Thomas Bassa
 * A Java class to handle the display of flight details on a map. */

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;

/** An activity that displays the points of a specified flight on a Google Map */
public class FlightDetailActivity extends FragmentActivity {

    public static final String INTENT_ID_INFO = "edu.erau.mad.trb" +
            ".flightdatarecorder.INTENT_FLIGHTID";

    /** The Google Map object, used to render the flight data. */
    private GoogleMap map; // Might be null if Google Play services APK is not available.

    /** Flag to ensure that this Activity is started with a flight ID in its intent. */
    private boolean startedWithValidIntent;
    /** The Flight ID whose path coordinates will be plotted */
    private long flightID = 0;

    //Called when the activity is created. Simply initializes fields from intent.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight_detail);

        //Get the intent data to figure out which flight ID we're rendering
        startedWithValidIntent = false;
        Bundle intentExtras = getIntent().getExtras();
        if(intentExtras.containsKey(INTENT_ID_INFO)) {
            flightID = intentExtras.getLong(INTENT_ID_INFO);
            startedWithValidIntent = true;
        }

        setUpMapIfNeeded();
    }

    //Called when the activity is resumed;
    //primarily done to handle Google Play interruptions
    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /** (Auto-generated docs.)
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #map} is not null.
     * <p>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called. */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (map == null) {
            // Try to obtain the map from the SupportMapFragment.
            map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (map != null) {
                setUpMap();
            }
        }
    }

    /** This method is invoked when the activity starts,
     * Google Maps is confirmed to be installed,
     * and the map is ensured to be non-null.
     * When this is done, the points provided by the ID are plotted using an
     * {@link edu.erau.mad.trb.flightdatarecorder.AsyncMapDatabasePlotter}. */
    private void setUpMap() {
        if(startedWithValidIntent) {
            new AsyncMapDatabasePlotter(flightID, map, this).execute();
        } else {
            //TO-DO handle activity starts without data?
            Toast.makeText(this, "No flight data was present!",
                    Toast.LENGTH_LONG).show();
        }
    }

}
