package edu.erau.mad.trb.flightdatarecorder;

import android.content.Intent;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Chronometer;


public class MainLoggingActivity extends ActionBarActivity implements
        Chronometer.OnChronometerTickListener {

    private Chronometer chronometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_logging);

        chronometer = (Chronometer) findViewById(R.id.timer);
        chronometer.setOnChronometerTickListener(this);

        int count = 0;
        for(long x = 0; x < (1000L * 60L * 60L * 30L); x += (1000L * 60L * 60L)) {
            Log.d("ChronTest", String.format("hour %d, millis %d becomes:",
                    count, x));
            Log.d("ChronTest", DateFormat.format("HH:mm:ss", x).toString());
            Log.d("ChronTest", DateFormat.format("kk:mm:ss", x).toString());
            count++;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_logging, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        chronometer.stop();
    }

    public void goToHistory(View view) {
        Intent startHist = new Intent(this, HistoryActivity.class);
        startActivity(startHist);
    }

    public void resetTimer(View view) {
        chronometer.stop();
        chronometer.setBase(SystemClock.elapsedRealtime() - (1000L * 60L * 60L));
        onChronometerTick(chronometer);
    }

    public void startTimer(View view) {
        //chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
    }

    public void stopTimer(View view) {
        chronometer.stop();
    }

    @Override
    public void onChronometerTick(Chronometer chronometer) {
        //Via...
        // http://stackoverflow.com/questions/4897665/android-chronometer-format
        long t = SystemClock.elapsedRealtime() - chronometer.getBase();
        Log.v("Chron", "Delta t = " + t);
        chronometer.setText(DateFormat.format("HH:mm:ss", t));
    }
}
