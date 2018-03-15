package hk.ust.cse.comp4521.mapspoialert;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import hk.ust.cse.comp4521.mapspoialert.provider.POIContract;

import static android.provider.BaseColumns._ID;
import static hk.ust.cse.comp4521.mapspoialert.provider.POIContract.POIEntry.*;

public class POIAlertActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        GoogleMap.OnInfoWindowClickListener,
        OnMapReadyCallback, GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMarkerClickListener, GoogleMap.OnMarkerDragListener  {

    private String TAG = "POIAlertActivity";

    // Spinner handle
    private Spinner poispinner;
    SimpleCursorAdapter mAdapter;

    private String pointOfInterest = "";
    private double latitude = 0.0;
    private double longitude = 0.0;
    private long rowID = 0;

    /**
     * Provides the entry point to Google Play services.
     */
    protected FusedLocationProviderClient mFusedLocationClient;

    /**
     * Represents a geographical location.
     */
    protected Location mLastLocation, mCurrentLocation;


    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    protected Boolean mRequestingLocationUpdates = false;

    /**
     * Time when the location was updated represented as a String.
     */
    protected String mLastUpdateTime;

    private LocationCallback mLocationCallback;

    protected static final String ADDRESS_REQUESTED_KEY = "address-request-pending";
    protected static final String LOCATION_ADDRESS_KEY = "location-address";

    /**
     * Tracks whether the user has requested an address. Becomes true when the user requests an
     * address and false when the address (or an error message) is delivered.
     * The user requests an address by pressing the Fetch Address button. This may happen
     * before GoogleApiClient connects. This activity uses this boolean to keep track of the
     * user's intent. If the value is true, the activity tries to fetch the address as soon as
     * GoogleApiClient connects.
     */
    protected boolean mAddressRequested;

    /**
     * The formatted location address.
     */
    protected String mPOIOutput, mLocationOutput, mAddressOutput;

    /**
     * Receiver registered with this activity to get the response from FetchAddressIntentService.
     */
    private AddressResultReceiver mResultReceiver;

    /**
     * The list of geofences used in this sample.
     */
    protected ArrayList<Geofence> mGeofenceList;

    /**
     * Used to keep track of whether geofences were added.
     */
    private boolean mGeofencesAdded = false;

    /**
     * Used when requesting to add or remove geofences.
     */
    private PendingIntent mGeofencePendingIntent;

    /**
     * Used to persist application state about whether geofences were added.
     */
    private SharedPreferences mSharedPreferences;

    private GeofencingClient mGeofencingClient;

    TextView poiView;

    private GoogleMap mMap;
    private boolean mapReady = false;
    private Marker meMarker = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poialert);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        poiView = (TextView) findViewById(R.id.pointOfInterest);


        // Empty list for storing geofences.
        mGeofenceList = new ArrayList<Geofence>();

        // Initially set the PendingIntent used in addGeofences() and removeGeofences() to null.
        mGeofencePendingIntent = null;

        // Retrieve an instance of the SharedPreferences object.
        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME,
                MODE_PRIVATE);

        // Get the value of mGeofencesAdded from SharedPreferences. Set to false as a default.
        mGeofencesAdded = mSharedPreferences.getBoolean(Constants.GEOFENCES_ADDED_KEY, false);

        mGeofencingClient = LocationServices.getGeofencingClient(this);

        Log.d("Maps", "After Setting up View");

        // We need to ensure that we get access to the map object. We need to wait
        // for the map object to be setup. This requires the implementation of an
        // async callback method onMapReady() where we get the reference to the map.
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapview);
        mapFragment.getMapAsync(this);

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, (LoaderCallbacks<Cursor>) this);

        // Build the Google API Fused Location Services client so that connections can be established
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            Log.i(TAG, "No Permission Granted!");
            return;
        }

        Log.i(TAG, "Location can be determined!");

        createLocationRequest();

        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            mLastLocation = location;
                            mCurrentLocation = mLastLocation;
                            String message = "Last Location is: " +
                                    "  Latitude = " + String.valueOf(mLastLocation.getLatitude()) +
                                    "  Longitude = " + String.valueOf(mLastLocation.getLongitude());
                            Log.i(TAG, message);
                            Snackbar.make(poiView, message, Snackbar.LENGTH_LONG).show();

                            // Determine whether a Geocoder is available.
                            if (!Geocoder.isPresent()) {
                                Snackbar.make(poiView, getString(R.string.no_geocoder_available), Snackbar.LENGTH_LONG).show();
                                return;
                            }
                            mAddressRequested = true;

                            // It is possible that the user presses the button to get the address before the
                            // GoogleApiClient object successfully connects. In such a case, mAddressRequested
                            // is set to true, but no attempt is made to fetch the address (see
                            // fetchAddressButtonHandler()) . Instead, we start the intent service here if the
                            // user has requested an address, since we now have a connection to GoogleApiClient.
                            if (mAddressRequested) {
                                startIntentService(mLastLocation);
                            }
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, getString(R.string.no_location_detected));
                        Snackbar.make(poiView, R.string.no_location_detected, Snackbar.LENGTH_LONG).show();
                    }
                });

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    mCurrentLocation = location;
                    mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                    String message = "Current Location is: " +
                            "  Latitude = " + String.valueOf(mCurrentLocation.getLatitude()) +
                            "  Longitude = " + String.valueOf(mCurrentLocation.getLongitude() +
                            "\nLast Updated = " + mLastUpdateTime);
                    Log.i(TAG, message);
                    Snackbar.make(poiView, message, Snackbar.LENGTH_LONG).show();

                    mAddressRequested = true;

                    if (meMarker != null)
                        meMarker.remove();

                    MarkerOptions markCur = new MarkerOptions()
                            .position(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.pink_stick_man))
                            .title("Me");

                    meMarker = mMap.addMarker(markCur);

                    if (mAddressRequested) {
                        startIntentService(mCurrentLocation);
                    }
                };
            };


        };


        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";

        // Set defaults, then update using values stored in the Bundle.
        mResultReceiver = new AddressResultReceiver(new Handler());
        mAddressRequested = false;
        mAddressOutput = "";

        updateValuesFromBundle(savedInstanceState);

    }

    /**
     * Updates fields based on data stored in the bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Check savedInstanceState to see if the address was previously requested.
            if (savedInstanceState.keySet().contains(ADDRESS_REQUESTED_KEY)) {
                mAddressRequested = savedInstanceState.getBoolean(ADDRESS_REQUESTED_KEY);
            }
            // Check savedInstanceState to see if the location address string was previously found
            // and stored in the Bundle. If it was found, display the address string in the UI.
            if (savedInstanceState.keySet().contains(LOCATION_ADDRESS_KEY)) {
                mAddressOutput = savedInstanceState.getString(LOCATION_ADDRESS_KEY);
            }
        }
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null /* Looper */);
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }

    private void addGeoFence() {
        mGeofenceList.add(new Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId(pointOfInterest)

                // Set the circular region of this geofence.
                .setCircularRegion(
                        latitude,
                        longitude,
                        Constants.GEOFENCE_RADIUS_IN_METERS
                )

                // Set the expiration duration of the geofence. This geofence gets automatically
                // removed after this period of time.
                .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)

                // Set the transition types of interest. Alerts are only generated for these
                // transition. We track entry and exit transitions in this sample.
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)

                // Create the geofence.
                .build());

        try {
            mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                    .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Update state and save in shared preferences.
                            SharedPreferences.Editor editor = mSharedPreferences.edit();
                            editor.putBoolean(Constants.GEOFENCES_ADDED_KEY, mGeofencesAdded);
                            editor.commit();

                            Toast.makeText(getApplicationContext(),
                                    getString(R.string.geofences_added),
                                    Toast.LENGTH_SHORT
                            ).show();
                            mGeofencesAdded = true;
                        }
                    })
                    .addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Get the status code for the error and log it using a user-friendly message.
                            Log.e(TAG, e.getMessage());
                        }
                    });

        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }

    private void logSecurityException(SecurityException securityException) {
        Log.e(TAG, "Invalid location permission. " +
                "You need to use ACCESS_FINE_LOCATION with geofences", securityException);
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    protected void onStart() {

        super.onStart();
    }

    @Override
    protected void onRestart() {

        super.onRestart();
    }

    @Override
    protected void onResume() {

        super.onResume();
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {

        if (mRequestingLocationUpdates) {
            mRequestingLocationUpdates = false;
            stopLocationUpdates();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {

        super.onStop();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    /**
     * Creates an intent, adds location data to it as an extra, and starts the intent service for
     * fetching an address.
     */
    protected void startIntentService(Location location) {
        // Create an intent for passing to the intent service responsible for fetching the address.
        Intent intent = new Intent(this, FetchAddressIntentService.class);

        // Pass the result receiver as an extra to the service.
        intent.putExtra(Constants.RECEIVER, (Parcelable) mResultReceiver);

        // Pass the location data as an extra to the service.
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, location);

        // Start the service. If the service isn't already running, it is instantiated and started
        // (creating a process for it if needed); if it is running then it remains running. The
        // service kills itself automatically once all intents are processed.
        startService(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save whether the address has been requested.
        savedInstanceState.putBoolean(ADDRESS_REQUESTED_KEY, mAddressRequested);

        // Save the address string.
        savedInstanceState.putString(LOCATION_ADDRESS_KEY, mAddressOutput);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Receiver for data sent from FetchAddressIntentService.
     */
    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        /**
         *  Receives data sent from FetchAddressIntentService and updates the UI in MainActivity.
         */
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string or an error message sent from the intent service.
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);

            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
                Toast.makeText(getApplicationContext(), getString(R.string.address_found), Toast.LENGTH_LONG).show();
            }

            // Reset. Enable the Fetch Address button and stop showing the progress bar.
            mAddressRequested = false;
        }
    }

    // These is the projection on the POI content provider rows that we will retrieve.
    static final String[] POIDb_PROJECTION = new String[] {
            _ID, // unique id to identify the row
            COLUMN_POI, // file handle
            COLUMN_LATITUDE,
            COLUMN_LONGITUDE
    };

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader, so we don't care about the ID.

        // This is the URI of the Android Media Store that enable's
        // access to all the music files on the device

        Uri baseUri = POIContract.POIProvider.CONTENT_URI;

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        String select = "((" + COLUMN_POI + " NOTNULL) AND ("
                + COLUMN_POI + " != '' ))";

        CursorLoader curloader = new CursorLoader(this, baseUri,
                POIDb_PROJECTION, select, null,
                null);

        return curloader;
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)

        setUpMarkers(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_poialert, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        Intent intent = new Intent(this,AddPOI.class);

        //noinspection SimplifiableIfStatement
        switch (item.getItemId()) {
            case R.id.action_add_poi:
                intent.putExtra("Add",true);
                startActivity(intent);

                break;
            case R.id.action_update_poi:
                intent.putExtra("Add",false);
                intent.putExtra("POI Name", pointOfInterest);
                intent.putExtra("Latitude", latitude);
                intent.putExtra("Longitude", longitude);
                intent.putExtra("RowID", rowID);
                startActivity(intent);

                break;

            case R.id.action_delete_poi:

                Uri baseUri = ContentUris.withAppendedId(POIContract.POIProvider.CONTENT_URI, rowID);

                String select = "((" + COLUMN_POI + " NOTNULL) AND ("
                        + COLUMN_POI + " != '' ))";

                int retval = getContentResolver().delete(baseUri, select, null);
                Log.i(TAG, "Updated " + retval + " rows");

                break;
            case R.id.action_settings:
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mapReady = true;
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMarkerDragListener(this);

    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMarkers(Cursor markerCursor) {

        int column_index;
        double latitude, longitude;
        String poiName;
        long id;

        while (!mapReady) {}

        // Move to the first row in the cursor
        markerCursor.moveToFirst();

        do{ // for all the rows in the cursor


            // Get the poiName's name, latitude and longitude
            id = markerCursor.getLong(markerCursor.getColumnIndexOrThrow(_ID));
            poiName = markerCursor.getString(markerCursor.getColumnIndexOrThrow(COLUMN_POI));
            latitude = markerCursor.getDouble(markerCursor.getColumnIndexOrThrow(COLUMN_LATITUDE));
            longitude = markerCursor.getDouble(markerCursor.getColumnIndexOrThrow(COLUMN_LONGITUDE));

            Log.i(TAG, "Marker at: "+id+poiName+latitude+longitude);

            mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title(poiName).snippet(Long.toString(id)));

        } while (markerCursor.moveToNext());  // until you exhaust all the rows. returns false when we reach the end of the cursor

        markerCursor.close();

    }

    @Override
    public void onInfoWindowClick(Marker marker) {

        mGeofencingClient.removeGeofences(getGeofencePendingIntent());

        marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

//        Log.i(TAG,"Marker Snippet is "+marker.getSnippet());
//        long rowIndex = Long.valueOf(marker.getSnippet()).longValue();
//        Log.i(TAG,"Marker Long is "+rowIndex);
//        Uri baseUri = ContentUris.withAppendedId(POIContract.POIProvider.CONTENT_URI, rowIndex);
//
//        String select = "((" + COLUMN_POI + " NOTNULL) AND ("
//                + COLUMN_POI + " != '' ))";
//
//        int retval = getContentResolver().delete(baseUri, select, null);
//        Log.i(TAG, "Updated " + retval + " rows");

    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        LatLng ll = marker.getPosition();

        // Get the pointOfInterest's name, latitude and longitude
        pointOfInterest = marker.getTitle();
        latitude = ll.latitude;
        longitude = ll.longitude;

        String message = String.format(
                "%1$s\n Longitude: %2$s \n Latitude: %3$s",
                pointOfInterest, longitude, latitude
        );

        mPOIOutput = message;

        Log.d(TAG, "Proximity Alert for: "+message);

        Toast.makeText(getBaseContext(), "Proximity Alert for: "+message, Toast.LENGTH_LONG).show();

        addGeoFence();

        marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

        return false;
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {


    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        marker.setDraggable(false);

    }

    @Override
    public void onMapLongClick(LatLng latLng) {

        Log.i(TAG, "Map clicked at Latitude " + latLng.latitude + " and Longitude " + latLng.longitude);

        AddMapPOIFragment addpoiFrag = new AddMapPOIFragment(latLng);

        addpoiFrag.show(getSupportFragmentManager(),"addpoi");

    }

    public static class AddMapPOIFragment extends DialogFragment {

        String poiName;

        EditText mName;
        TextView mLat, mLong;

        LatLng ll;

        @SuppressLint("ValidFragment")
        public AddMapPOIFragment(LatLng latLng) {
            ll = latLng;
        }

        public AddMapPOIFragment() {

        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Add a new Point of Interest")
                    .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ContentValues initialValues = new ContentValues();

                            poiName = mName.getText().toString();

                            Log.i("Dialog Fragment", poiName + " " + ll.latitude + " " + ll.longitude);

                            initialValues.put(POIContract.POIEntry.COLUMN_POI, poiName);
                            initialValues.put(POIContract.POIEntry.COLUMN_LATITUDE, ll.latitude);
                            initialValues.put(POIContract.POIEntry.COLUMN_LONGITUDE, ll.longitude);

                            Uri baseUri = POIContract.POIProvider.CONTENT_URI;

                            Uri newUri = getActivity().getContentResolver().insert(baseUri, initialValues);

                            dialog.dismiss();

                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    });

            LayoutInflater inflater = getActivity().getLayoutInflater();

            View view = inflater.inflate(R.layout.dialog_add_poi, null);

            mName = (EditText) view.findViewById(R.id.poidiaName);
            mLat = (TextView) view.findViewById(R.id.txtdiaLat);
            mLong = (TextView) view.findViewById(R.id.txtdiaLong);

            mLat.setText("Latitude: "+Double.toString(ll.latitude));
            mLong.setText("Longitude: "+Double.toString(ll.longitude));

            builder.setView(view);

            // Create the AlertDialog object and return it
            return builder.create();
        }


        @Override
        public void onDismiss(DialogInterface dialog) {
        }
    }

}
