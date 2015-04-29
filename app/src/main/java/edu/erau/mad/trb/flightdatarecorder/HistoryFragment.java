package edu.erau.mad.trb.flightdatarecorder;
/* HistoryActivity.java
 * SE395A Final Project
 * by Thomas Bassa
 * A Java class to handle the list of flights. */

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


/** A Fragment that contains the list of flights */
public class HistoryFragment extends ListFragment implements
        SimpleCursorAdapter.ViewBinder {

    /** A listener for list item clicks (invoked on container Activity) */
    private ListItemClickListener mListener;

    /** A database connection */
    private FlightLogDatabase database;

    /** Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes). */
    public HistoryFragment() { }

    //TO-DO implement multiple selection lists for delete & export
    //TO-DO close the cursors you're using, with lifecycle methods

    //Fragment lifecycle methods
    //Called when fragment first binds to activity; casts to interface, gets DB
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        //Ensure that the containing activity implements the Fragment interface
        if(activity instanceof ListItemClickListener) {
            mListener = (ListItemClickListener) activity;
        } else {
            throw new ClassCastException(activity.toString()
                    + " must implement ListItemClickListener");
        }
        database = FlightLogDatabase.getInstance(activity);
    }

    //Called when fragment is first created, used to initialize list
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Get list of flights in cursor form
        Cursor flightsCursor = database.getAllFlights();

        //Map columns to list row Views
        final String[] fromCols = {FlightLogDatabase.COL_START_REAL,
                FlightLogDatabase.COL_END_REAL};
        //TO-DO Implement a better layout, multiple selection layout
        final int[] toViews = {android.R.id.text1, android.R.id.text2};

        //Build an adapter using the above
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.two_line_list_item, flightsCursor, fromCols, toViews, 0);
        adapter.setViewBinder(this);

        //Give it to our list
        setListAdapter(adapter);
    }

    //Called to initialize the view of this Fragment
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    //Called when the fragment is removed from its activity
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    //ListFragment overrides
    //Called when a list item is clicked. Used to invoke action from the parent activity.
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if(mListener != null) {
            mListener.onFragmentListClick(id);
        }
    }

    //Implementation of ViewBinder interface, used by SimpleCursorAdapter
    // Used to populate the ListView sub-elements with human-readable times.
    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        boolean handled = false;
        //If the cursor is pointed at a time...
        if(columnIndex == cursor.getColumnIndex(FlightLogDatabase.COL_START_REAL)
                || columnIndex == cursor.getColumnIndex(FlightLogDatabase.COL_END_REAL)) {
            //And we want to change a textview...
            if(view instanceof TextView) {
                //Convert it to a human-readable time.
                final TextView text = (TextView) view;
                text.setText(String.format("%ta %<tb %<te %<tY - %<tr",
                        cursor.getLong(columnIndex)));
                handled = true;
            }
        }
        //If handled is false, the Adapter will do default work for us.
        return handled;
    }

    /** (Auto-generated docs.)
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information. */
    public interface ListItemClickListener {
        /** Called when a list item is clicked. Only the ID of the item is provided. */
        public void onFragmentListClick(long flightID);
    }

}
