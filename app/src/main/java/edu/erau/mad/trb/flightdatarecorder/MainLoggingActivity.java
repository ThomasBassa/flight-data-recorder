package edu.erau.mad.trb.flightdatarecorder;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
//TODO MainLoggingActivity documentation pass
public class MainLoggingActivity extends ActionBarActivity {
    //TODO Improve the UI of MainLoggingActivity; involve a fragment maybe
    //TODO account for landscape orientation in MainLoggingActivity


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_logging);
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    //TODO override more lifecycle methods-- attach to the service, etc.

    //TODO does main actually need a menu?
    /*
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
    */

    public void goToHistory(View view) {
        Intent startHist = new Intent(this, HistoryActivity.class);
        startActivity(startHist);
    }
}
