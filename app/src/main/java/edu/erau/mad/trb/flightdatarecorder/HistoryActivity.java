package edu.erau.mad.trb.flightdatarecorder;
/* HistoryActivity.java
 * SE395A Final Project
 * by Thomas Bassa
 * A Java class to handle the list of flights and its manipulation. */
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

/** An activity to contain a HistoryFragment, and manage global actions on the
 * list of flights contained in said fragment. */
public class HistoryActivity extends ActionBarActivity implements
        HistoryFragment.ListItemClickListener {

    /** The database, which is reset as a result of an action bar... action */
    private FlightLogDatabase database;

    //private HistoryFragment histFrag;

    //Called when the activity is created. Simply grabs a database instance.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        database = FlightLogDatabase.getInstance(this);
        //histFrag = (HistoryFragment) getFragmentManager().findFragmentById(R.id.histFrag);
    }

    //Initialize the action bar menu items.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_history, menu);
        return true;
    }

    //Handles action bar item clicks.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            /*case R.id.action_export:
                //TO-DO switch HistoryFragment to multiple-select mode...
                Toast.makeText(this, "Export will be implemented.",
                        Toast.LENGTH_SHORT).show();
                return true;
            */
            case R.id.action_delete_all:
                //Reset the database
                //Pop dialog to confirm database wipe
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                dialogBuilder.setPositiveButton(R.string.dialogWipe, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //This is executed on confirm!
                        //Stop the LoggingService so it doesn't run into DB troubles
                        stopService(new Intent(HistoryActivity.this, LoggingService.class));
                        //Reset the database
                        database.reset();
                        //Inform the user it's done
                        Toast.makeText(HistoryActivity.this, R.string.historyErased,
                                Toast.LENGTH_LONG).show();
                        //Kill this activity since its data is invalid and not needed
                        finish();
                    }
                })
                .setNegativeButton(R.string.dialogCancel, null) //Do nothing on cancel
                .setMessage(R.string.dialogDeleteMessage)
                .setCancelable(false) //Don't allow back button/tap outside dialog to kill
                .setTitle(R.string.dialogDeleteTitle)
                .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //Override from the HistoryFragment interface;
    //reacts to list clicks by opening the DetailActivity with associated ID
    @Override
    public void onFragmentListClick(long id) {
        Intent showDetails = new Intent(this, FlightDetailActivity.class);
        showDetails.putExtra(FlightDetailActivity.INTENT_ID_INFO, id);
        startActivity(showDetails);
    }
}
