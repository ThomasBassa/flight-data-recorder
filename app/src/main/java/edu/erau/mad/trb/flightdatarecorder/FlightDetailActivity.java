package edu.erau.mad.trb.flightdatarecorder;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;

//TODO Document FlightDetailActivity
public class FlightDetailActivity extends FragmentActivity {

    //TODO Maybe add an action bar + menu items...
    public static final String INTENT_ID_INFO = "edu.erau.mad.trb" +
            ".flightdatarecorder.INTENT_FLIGHTID";

    private GoogleMap map; // Might be null if Google Play services APK is not available.

    private boolean startedWithValidIntent;
    private long flightID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight_detail);

        startedWithValidIntent = false;
        Bundle intentExtras = getIntent().getExtras();
        if(intentExtras.containsKey(INTENT_ID_INFO)) {
            flightID = intentExtras.getLong(INTENT_ID_INFO);
            startedWithValidIntent = true;
        }

        setUpMapIfNeeded();
    }

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
     * method in {@link #onResume()} to guarantee that it will be called.
     */
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

    /** (Auto-generated docs.)
     * This is where we can add markers or lines, add listeners or move the camera.
     * <p>
     * This should only be called once and when we are sure that {@link #map} is not null.
     */
    private void setUpMap() {
        if(startedWithValidIntent) {
            new AsyncMapDatabasePlotter(flightID, map, this).execute();
        } else {
            //TODO handle activity starts without data?
            Toast.makeText(this, "No flight data was provided!",
                    Toast.LENGTH_LONG).show();
        }
    }

}
