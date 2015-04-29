package edu.erau.mad.trb.flightdatarecorder;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.concurrent.TimeUnit;

//TODO MainLoggingActivity documentation pass
public class MainLoggingActivity extends ActionBarActivity
        implements CompoundButton.OnCheckedChangeListener, ServiceConnection,
        LoggingService.LogUpdateInterface {
    //TODO maybe make the MainLoggingActivity layouts less crappy

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

        startStopButton.setOnCheckedChangeListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    protected void onResume() {
        super.onResume();
        if(startStopButton.isChecked()) {
            startLogging();
        }
    }

    protected void onPause() {
        super.onPause();
        stopLogging();
    }

    //TODO override more lifecycle methods-- attach to the service, etc.

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
        bindService(serviceIntent, this, BIND_IMPORTANT);
    }

    public void stopLogging() {
        if(service != null) {
            service.unregisterListener(this);
            unbindService(this);
            stopService(serviceIntent);
            service = null;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder serviceBinder) {
        service = ((LoggingService.LoggingBinder) serviceBinder).getService();
        service.registerListener(this);
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
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(isChecked) startLogging();
        else stopLogging();
    }
}
