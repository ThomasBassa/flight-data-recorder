package edu.erau.mad.trb.flightdatarecorder;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.concurrent.TimeUnit;

//TODO MainLoggingActivity documentation pass
public class MainLoggingActivity extends ActionBarActivity
        implements View.OnClickListener, ServiceConnection,
        LoggingService.LogUpdateInterface {

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

    private LoggingService service;

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

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("LogActivity", "Resume. Attempt bind.");
        bindService(serviceIntent, this, BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("LogActivity", "Pause. Attempt disconnect.");
        if(service != null) {
            Log.e("LogActivity", "> Disconnecting...");
            service.unregisterListener(this);
            unbindService(this);
        } else {
            Log.e("LogActivity", "> Service not alive.");
        }
    }

    @Override
    public void updateFieldData(DevicePosAndOrient data) {
        dispRoll.setText(DevicePosAndOrient.formatDegValue(data.getRoll()));
        dispPitch.setText(DevicePosAndOrient.formatDegValue(data.getPitch()));
        dispYaw.setText(DevicePosAndOrient.formatDegValue(data.getAz()));

        dispLati.setText(data.getNiceLatitude());
        dispLongi.setText(data.getNiceLongitude());
        dispAlt.setText(data.getNiceAltitude());
    }

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

    public void startLogging() {
        startService(serviceIntent);
        bindService(serviceIntent, this, BIND_AUTO_CREATE);
    }

    public void stopLogging() {
        if(service != null) {
            unbindService(this);
            stopService(serviceIntent);
            service = null;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder serviceBinder) {
        Log.e("LogActivity", "Trying to bind to service.");
        service = ((LoggingService.LoggingBinder) serviceBinder).getService();
        if(service != null) {
            Log.e("LogActivity", "> Found. Registering...");
            service.registerListener(this);
            startStopButton.setChecked(true);
        } else {
            unbindService(this);
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

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /** Open the HistoryActivity. Attached to a button via XML. */
    public void goToHistory(View view) {
        Intent startHist = new Intent(this, HistoryActivity.class);
        startActivity(startHist);
    }

    @Override
    public void onClick(View v) {
        if(startStopButton.isChecked()) startLogging();
        else stopLogging();
    }
}
