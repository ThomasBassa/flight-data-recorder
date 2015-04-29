package edu.erau.mad.trb.flightdatarecorder;
/* MainLoggingActivity.java
 * SE395A Final Project
 * by Thomas Bassa
 * A Java class to handle entry point of the app, and logging display. */

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.concurrent.TimeUnit;

/** An Activity to serve as the entry point of the Flight Data Recorder and
 * show its main logging functionality. */
public class MainLoggingActivity extends ActionBarActivity
        implements View.OnClickListener, ServiceConnection,
        LoggingService.LogUpdateInterface {

    //Maps to start a LoggingService (to avoid a bunch of redundant new Intent())
    private Intent serviceIntent;

    //Views
    private TextView clock;

    private ToggleButton startStopButton;

    private TextView dispRoll;
    private TextView dispPitch;
    private TextView dispYaw;
    private TextView dispLati;
    private TextView dispLongi;
    private TextView dispAlt;

    /** Direct connection to a LoggingService; non-null after successful
     * starting AND binding. */
    private LoggingService service;

    //Called when activity created. Connects XML views to this class.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_logging);

        serviceIntent = new Intent(this, LoggingService.class);

        clock = (TextView) findViewById(R.id.clock);

        startStopButton = (ToggleButton) findViewById(R.id.startStopButton);

        dispRoll = (TextView) findViewById(R.id.dispRoll);
        dispPitch = (TextView) findViewById(R.id.dispPitch);
        dispYaw = (TextView) findViewById(R.id.dispYaw);
        dispLati = (TextView) findViewById(R.id.dispLati);
        dispLongi = (TextView) findViewById(R.id.dispLongi);
        dispAlt = (TextView) findViewById(R.id.dispAlt);

        startStopButton.setOnClickListener(this);
    }

    //On activity resume, attempt to bind with the LoggingService for UI updates
    @Override
    protected void onResume() {
        super.onResume();
        bindService(serviceIntent, this, BIND_AUTO_CREATE);
    }

    //On activity pause, unbind from the LoggingService if bound
    @Override
    protected void onPause() {
        super.onPause();
        if(service != null) {
            service.unregisterListener(this);
            unbindService(this);
        }
    }

    //Implementation of LogUpdateInterface
    //Update the position and orientation fields
    @Override
    public void updateFieldData(DevicePosAndOrient data) {
        dispRoll.setText(DevicePosAndOrient.formatDegValue(data.getRoll()));
        dispPitch.setText(DevicePosAndOrient.formatDegValue(data.getPitch()));
        dispYaw.setText(DevicePosAndOrient.formatDegValue(data.getAz()));

        dispLati.setText(data.getNiceLatitude());
        dispLongi.setText(data.getNiceLongitude());
        dispAlt.setText(data.getNiceAltitude());
    }

    //Update the clock in HH:MM:SS format from a delta-t
    // e.g. a counter/stopwatch time, not clock time
    @Override
    public void updateClock(long deltaT) {
        // http://stackoverflow.com/questions/625433/how-to-convert-milliseconds-to-x-mins-x-seconds-in-java
        long hours = TimeUnit.MILLISECONDS.toHours(deltaT);
        deltaT -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(deltaT);
        deltaT -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(deltaT);

        clock.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

    /** Initiate a new LoggingService and bind to it (called on toggle button press) */
    public void startLogging() {
        startService(serviceIntent);
        bindService(serviceIntent, this, BIND_AUTO_CREATE);
    }

    /** Unbind from the LoggingService and stop it (called on toggle button press) */
    public void stopLogging() {
        if(service != null) {
            unbindService(this);
            stopService(serviceIntent);
            service = null;
        }
    }

    //This is called upon attempting to bind with bindService.
    @Override
    public void onServiceConnected(ComponentName name, IBinder serviceBinder) {
        //Get the LoggingService out of the binder
        service = ((LoggingService.LoggingBinder) serviceBinder).getService();

        //If it was null, the service was NOT previously started.
        if(service != null) {
            //We bound to the LoggingService after it started; get updates
            service.registerListener(this);
            //Ensure the toggle button is turned on (logging is active)
            startStopButton.setChecked(true);
        } else {
            //Service was brought to life just for binding; kill it
            unbindService(this);

            //Reset the state of the UI (since no logging is happening)
            startStopButton.setChecked(false);

            clock.setText(R.string.zeroTime);
            dispRoll.setText(R.string.zeroDeg);
            dispPitch.setText(R.string.zeroDeg);
            dispYaw.setText(R.string.zeroDeg);
            dispLati.setText(R.string.zeroCoord);
            dispLongi.setText(R.string.zeroCoord);
            dispAlt.setText(R.string.zeroFt);
        }
    }

    //The service was accidentally killed. onServiceConnected will rebind it.
    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /** Open the HistoryActivity. Attached to a button via XML. */
    public void goToHistory(View view) {
        Intent startHist = new Intent(this, HistoryActivity.class);
        startActivity(startHist);
    }

    //The onClickListener for the ToggleButton.
    //Used instead of onCheckedChangedListener because that reacted to setChecked.
    //This is only called in response to user clicks.
    @Override
    public void onClick(View v) {
        //Start or stop logging depending on the button state.
        if(startStopButton.isChecked()) startLogging();
        else stopLogging();
    }
}
