package edu.umbc.smartroute;

import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;


import java.util.ArrayList;
import java.util.List;

import parser.ResponseMultiPath;
import utils.ApplicationConfiguration;
import utils.Util;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener,LocationListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    private String TAG = MainActivity.class.getSimpleName();
    Marker mCurrLocationMarker;
    Marker startLocationMarker;
    Marker endLocationMarker;

    LocationRequest mLocationRequest;
    Double startLatitude , startLongitude;
    Double destnLatitude , destnLongitude;
    String startPlace="",destnPlace = "";
    private Button btnStringReq;
    private TextView msgResponse;

    // This tag will be used to cancel the request
    private String tag_string_req = "string_req";
    private ProgressDialog pDialog;
    private ArrayList<String> encodeStList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        // setup maps
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // dialog
        pDialog = new ProgressDialog(this);
        pDialog.setMessage("Loading...");
        pDialog.setCancelable(false);

        //request
        btnStringReq = (Button) findViewById(R.id.reqBtn);
        btnStringReq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeStringReq();
            }
        });
        // setup place api
        setUpPlaceAutoComplete();
    }

    private void setUpPlaceAutoComplete() {
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.startplace_autocomplete_fragment);

        (autocompleteFragment.getView().findViewById(R.id.place_autocomplete_search_input)).setBackgroundColor(getResources().getColor(R.color.colorWhite));
        (autocompleteFragment.getView().findViewById(R.id.place_autocomplete_search_button)).setBackgroundColor(getResources().getColor(R.color.colorWhite));

        ((EditText)autocompleteFragment.getView().findViewById(R.id.place_autocomplete_search_input)).setTextSize(15);

        autocompleteFragment.setHint("Select Source");
        PlaceAutocompleteFragment endcompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.endplace_autocomplete_fragment);

        ((EditText)endcompleteFragment.getView().findViewById(R.id.place_autocomplete_search_input)).setTextSize(15);
        (endcompleteFragment.getView().findViewById(R.id.place_autocomplete_search_input)).setBackgroundColor(getResources().getColor(R.color.colorWhite));
        (endcompleteFragment.getView().findViewById(R.id.place_autocomplete_search_button)).setBackgroundColor(getResources().getColor(R.color.colorWhite));

        endcompleteFragment.setHint("Select Destination");
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                Log.i("Main", "startPlace: " + place.getName());
                startPlace = place.getName().toString();
                startLatitude = place.getLatLng().latitude;
                startLongitude = place.getLatLng().longitude;
                mMap.clear();

                startDestinationPoint();
                //drawDestinationPoint();
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i("Main", "An error occurred: " + status);
            }
        });

        endcompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                Log.i("Main", "EndPlace: " + place.getName());
                destnPlace = place.getName().toString();
                destnLatitude = place.getLatLng().latitude;
                destnLongitude = place.getLatLng().longitude;
                mMap.clear();
                startDestinationPoint();
                drawDestinationPoint();
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i("Main", "An error occurred: " + status);
            }
        });
    }

    public  void startDestinationPoint()
    {
            if(startLongitude !=null && startLongitude!=null) {
                LatLng latLng = new LatLng(startLatitude, startLongitude);
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title("Source Position");
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                startLocationMarker = mMap.addMarker(markerOptions);
                //move map camera
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

            }

    }
    public  void drawDestinationPoint()
    {
        //Place current location marker
        LatLng latLng = new LatLng(destnLatitude, destnLongitude);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Destination Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
        endLocationMarker = mMap.addMarker(markerOptions);

        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
      //  mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        }
        else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        //Place current location marker
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        mCurrLocationMarker = mMap.addMarker(markerOptions);

        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted. Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request.
            // You can add here other case statements according to your requirement.
        }
    }

    private void showProgressDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideProgressDialog() {
        if (pDialog.isShowing())
            pDialog.hide();
    }

    /**
     * Making json object request
     * */
    private void makeStringReq() {
        showProgressDialog();
        String uri = Uri.parse(ApplicationConfiguration.SERVER_URL)
                .buildUpon()
                .appendQueryParameter("source", startLatitude+","+startLongitude)
                 .appendPath("&")
                .appendQueryParameter("destination",destnLatitude+","+destnLongitude)
                .build().toString();

        StringRequest strReq = new StringRequest(Request.Method.GET,
                uri, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, response.toString());
                ResponseMultiPath responseMultiPath = new ResponseMultiPath();
                encodeStList=   responseMultiPath.getresponse(response);
                displayMultiPathOnMap(encodeStList);
                hideProgressDialog();

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error: " + error.getMessage());
                hideProgressDialog();
            }
        });

        // Adding request to request queue
        Application.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * This function displays polylines on map
     */
    void displayMultiPathOnMap(ArrayList<String> encodeStList)
    {
        for(int i =0 ;i<encodeStList.size();i++) {
            List<LatLng> list = Util.decodePoly(encodeStList.get(i));
            for(int j=0;j<list.size();j++)
            {
                System.out.println("\nLatlong ->" + i +" "+list.get(j).latitude +" "+list.get(j).longitude);
            }
            PolylineOptions lineOptions = new PolylineOptions();
            lineOptions.addAll(list);
            lineOptions.width(10);
            if(encodeStList.size() == 1) {
                lineOptions.color(getResources().getColor(R.color.colorGreen));
                lineOptions.width(20);
            }else if(encodeStList.size() == 2) {
                if(i==0) {
                    lineOptions.color(getResources().getColor(R.color.colorGreen));
                    lineOptions.width(20);
                }
                else if(i==1)
                 lineOptions.color(getResources().getColor(R.color.colorRed));
            }else if(encodeStList.size() == 3) {
                if(i==0) {
                    lineOptions.color(getResources().getColor(R.color.colorGreen));
                    lineOptions.width(20);
                }else if(i==1)
                    lineOptions.color(getResources().getColor(R.color.colorOrange));
                else if(i==2)
                    lineOptions.color(getResources().getColor(R.color.colorRed));
            }else if(encodeStList.size() == 4) {
                if(i==0) {
                    lineOptions.color(getResources().getColor(R.color.colorGreen));
                    lineOptions.width(20);
                }else if(i==1)
                    lineOptions.color(getResources().getColor(R.color.colorYellow));
                else if(i==2)
                    lineOptions.color(getResources().getColor(R.color.colorOrange));
                else if(i==3) {
                    lineOptions.color(getResources().getColor(R.color.colorRed));
                }
            }
// Drawing polyline in the Google Map for the i-th route
            if (lineOptions != null) {
                mMap.addPolyline(lineOptions);
            }
        }
    }


}
