package edu.erau.mad.trb.flightdatarecorder;

import android.app.Activity;
import android.app.ListFragment;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;


//TODO Document HistoryFragment fully
public class HistoryFragment extends ListFragment implements
        SimpleCursorAdapter.ViewBinder {

    private ListItemClickListener mListener;

    private FlightLogDatabase database;


    /** Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes). */
    public HistoryFragment() { }

    /* Code snippet: Multiple selection adapter (alerts is a ListView obj)
    //Populate the list
    //Per Stansbury
    ArrayAdapter<String> alertAdapter = new ArrayAdapter<String>(
            this, android.R.layout.simple_list_item_multiple_choice, alertChoices);

    alerts.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    alerts.setAdapter(alertAdapter);
    */

    /* Code snippet: Read multiple selection (alerts is a ListView obj)
    //Get the selected alert options as CSV, sort of
    //Almost Stansbury's list concatenation, except with StringBuilder
    final SparseBooleanArray alertSelections = alerts.getCheckedItemPositions();

    final StringBuilder selectedAlerts = new StringBuilder();
    for (int i=0; i < alertSelections.size(); i++) {
        if (alertSelections.valueAt(i)) {
            //get selected item’s string
            selectedAlerts.append(
                    alerts.getAdapter().getItem(alertSelections.keyAt(i)).toString());

            selectedAlerts.append(", ");
        }
    }
    //Delete trailing comma separator (2 chars long)
    if(selectedAlerts.length() >= 2) //Make sure we have something to delete...
        selectedAlerts.delete(selectedAlerts.length() - 2, selectedAlerts.length());

    */

    //TO-DO implement multiple selection lists for delete & export

    //Fragment lifecycle methods
    //TO-DO close the cursors you're using, with lifecycle methods
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if(activity instanceof ListItemClickListener) {
            mListener = (ListItemClickListener) activity;
        } else {
            throw new ClassCastException(activity.toString()
                    + " must implement ListItemClickListener");
        }
        database = FlightLogDatabase.getInstance(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Cursor flightsCursor = database.getAllFlights();
        final String[] fromCols = {FlightLogDatabase.COL_START_REAL,
                FlightLogDatabase.COL_END_REAL};
        //TO-DO Implement a better layout, multiple selection layout
        final int[] toViews = {android.R.id.text1, android.R.id.text2};
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.two_line_list_item, flightsCursor, fromCols, toViews, 0);
        adapter.setViewBinder(this);
        setListAdapter(adapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    //ListFragment overrides

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if(mListener != null) {
            mListener.onFragmentListClick(id);
        }
    }

    //Implementation of ViewBinder interface. Used to populate the ListView
    // sub-elements with human-readable times.
    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        boolean handled = false;
        if(columnIndex == cursor.getColumnIndex(FlightLogDatabase.COL_START_REAL)
                || columnIndex == cursor.getColumnIndex(FlightLogDatabase.COL_END_REAL)) {
            if(view instanceof TextView) {
                final TextView text = (TextView) view;
                text.setText(String.format("%ta %<tb %<te %<tY - %<tr",
                        cursor.getLong(columnIndex)));
                handled = true;
            }
        }
        return handled;
    }

    /** (Auto-generated docs.)
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface ListItemClickListener {
        public void onFragmentListClick(long flightID);
    }

}
