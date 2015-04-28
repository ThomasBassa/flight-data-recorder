package edu.erau.mad.trb.flightdatarecorder;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


public class HistoryActivity extends ActionBarActivity implements
        HistoryFragment.ListItemClickListener {

    private FlightLogDatabase database;

    //TODO handle to its contained HistoryFragment

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        database = FlightLogDatabase.getInstance(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()) {
            case R.id.action_export:
                //TODO switch HistoryFragment to multiple-select mode...
                Toast.makeText(this, "Export will be implemented.",
                        Toast.LENGTH_SHORT).show();
                return true;
            //TODO case for "delete some"
            case R.id.action_delete_all:
                //TODO Reset the database
                //Pop dialog to confirm database wipe
                //If confirmed, do it
                Toast.makeText(this, "Delete all will be implemented.",
                        Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onFragmentListClick(long id) {
        //TODO Launch the details view instead.
        Toast.makeText(this, String.format("This flight started at %tD %<tT",
                        database.getFlightStart(id)),
                Toast.LENGTH_SHORT).show();
    }
}
