package hk.ust.cse.comp4521.mapspoialert;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import hk.ust.cse.comp4521.mapspoialert.provider.POIContract;

import static android.provider.BaseColumns._ID;
import static hk.ust.cse.comp4521.mapspoialert.provider.POIContract.POIEntry.COLUMN_LATITUDE;
import static hk.ust.cse.comp4521.mapspoialert.provider.POIContract.POIEntry.COLUMN_LONGITUDE;
import static hk.ust.cse.comp4521.mapspoialert.provider.POIContract.POIEntry.COLUMN_POI;

public class AddPOI extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = "AddPOI";

    private EditText poiName, poiLatitude, poiLongitude;
    private String pointOfInterest = "";
    private double longitude = 0.0, latitude = 0.0;
    private long rowID = 0;
    private boolean add = true;
    private Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_poi);

        poiName = (EditText) findViewById(R.id.poiName);
        poiLatitude = (EditText) findViewById(R.id.poiLatitude);
        poiLongitude = (EditText) findViewById(R.id.poiLongitude);

        btnSubmit = (Button) findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(this);

        Intent in = getIntent();
        add = in.getBooleanExtra("Add",true);

        if (!add) {
            pointOfInterest = in.getStringExtra("POI Name");
            latitude = in.getDoubleExtra("Latitude",0.0);
            longitude = in.getDoubleExtra("Longitude",0.0);
            rowID = in.getLongExtra("RowID", 0);
        }

        poiName.setText(pointOfInterest);
        poiLatitude.setText(Double.toString(latitude));
        poiLongitude.setText(Double.toString(longitude));

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_poi, menu);
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
    public void onClick(View v) {

        pointOfInterest = poiName.getText().toString();
        latitude = Double.parseDouble(poiLatitude.getText().toString());
        longitude = Double.parseDouble(poiLongitude.getText().toString());

        Log.i(TAG, "POI " + pointOfInterest + " Latitude " + latitude + " Longitude " + longitude);

        if (add) {
            ContentValues initialValues = new ContentValues();
            initialValues.put(POIContract.POIEntry.COLUMN_POI, pointOfInterest);
            initialValues.put(POIContract.POIEntry.COLUMN_LATITUDE, latitude);
            initialValues.put(POIContract.POIEntry.COLUMN_LONGITUDE, longitude);

            Uri baseUri = POIContract.POIProvider.CONTENT_URI;

            Uri newUri = getContentResolver().insert(baseUri, initialValues);

            Log.i(TAG, "Uri " + newUri.toString());

            finish();

        }
        else {
            ContentValues initialValues = new ContentValues();
            initialValues.put(POIContract.POIEntry.COLUMN_POI, pointOfInterest);
            initialValues.put(POIContract.POIEntry.COLUMN_LATITUDE, latitude);
            initialValues.put(POIContract.POIEntry.COLUMN_LONGITUDE, longitude);

            // These is the projection on the POI content provider rows that we will retrieve.
            final String[] POIDb_POI_PROJECTION = new String[] {
                    _ID, // unique id to identify the row
                    COLUMN_POI, // POI name
                    COLUMN_LATITUDE, // latitude
                    COLUMN_LONGITUDE, // longitude
            };

            int column_index;

            Uri baseUri = ContentUris.withAppendedId(POIContract.POIProvider.CONTENT_URI, rowID);

            String select = "((" + COLUMN_POI + " NOTNULL) AND ("
                    + COLUMN_POI + " != '' ))";

            int retval = getContentResolver().update(baseUri, initialValues, select, null);

            Log.i(TAG, "Updated " + retval + " rows");

            finish();

        }
    }
}
