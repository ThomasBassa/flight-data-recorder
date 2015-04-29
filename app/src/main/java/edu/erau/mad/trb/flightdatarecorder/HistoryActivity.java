package edu.erau.mad.trb.flightdatarecorder;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


public class HistoryActivity extends ActionBarActivity implements
        HistoryFragment.ListItemClickListener {

    private FlightLogDatabase database;

    private HistoryFragment histFrag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        database = FlightLogDatabase.getInstance(this);
        histFrag = (HistoryFragment) getFragmentManager().findFragmentById(R.id.histFrag);
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
                //Reset the database
                //Pop dialog to confirm database wipe
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                dialogBuilder.setPositiveButton(R.string.dialogWipe, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //If confirmed, do it; kill the service and activity too
                        stopService(new Intent(HistoryActivity.this, LoggingService.class));
                        database.reset();
                        Toast.makeText(HistoryActivity.this, R.string.historyErased,
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .setNegativeButton(R.string.dialogCancel, null)
                .setMessage(R.string.dialogDeleteMessage)
                .setCancelable(false)
                .setTitle(R.string.dialogDeleteTitle)
                .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onFragmentListClick(long id) {
        Intent showDetails = new Intent(this, FlightDetailActivity.class);
        showDetails.putExtra(FlightDetailActivity.INTENT_ID_INFO, id);
        startActivity(showDetails);
    }
}
