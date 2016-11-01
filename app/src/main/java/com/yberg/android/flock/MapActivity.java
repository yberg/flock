package com.yberg.android.flock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.MonthDisplayHelper;
import android.util.TypedValue;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MapActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMarkerClickListener,
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "FLOCK/MapActivity";

    private static final String BASE_URL = "http://192.168.0.105:3000/";
    private static final String USER_URL = BASE_URL + "user/";
    private static final String FAMILY_URL = BASE_URL + "family/";
    private static final String SOCKET_URL = BASE_URL;

    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    public static final int ANIMATE_SPEED = 1000;

    public static final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 1;
    public static final int MY_PERMISSIONS_ACCESS_COARSE_LOCATION = 2;

    public static boolean appIsActive = true;

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    private String mLastUpdateTime;
    private double mDistance;
    private String mLocationString;

    private List<Favorite> mFavorites = new ArrayList<>();
    private List<Favorite> mOldFavorites;
    private List<Member> mFamily = new ArrayList<>();
    private List<Member> mOldFamily;
    private Member mMe;
    private MapLocation mMarked;

    private RelativeLayout mRoot;
    private SlidingUpPanelLayout mSlidingPanel;
    private TextView mLabel, mAddress, mTime;
    private ImageView mLabelImage;
    private SubMenu mFamilyMenu;
    private SharedPreferences mPrefs;
    private TextView mInfo;

    private Socket mSocket;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        // Get user id and name
        mPrefs = getSharedPreferences("com.yberg.android.life", Context.MODE_PRIVATE);
        String _id = mPrefs.getString("_id", "");
        String name = mPrefs.getString("name", "");
        String familyId = mPrefs.getString("familyId", "");
        if (!_id.equals("") && !name.equals("")) {
            mMe = new Member(name, _id, familyId);
        }
        mMarked = new Member();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initialize DrawerLayout
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Get the mFamily submenu
        mFamilyMenu = navigationView.getMenu().getItem(0).getSubMenu();

        // Request permissions
        requestPermissions();

        mRoot = (RelativeLayout) findViewById(R.id.root);
        mInfo = (TextView) findViewById(R.id.info);

        // Initialize sliding panel
        mSlidingPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mSlidingPanel.setPanelState(PanelState.HIDDEN);
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mSlidingPanel.setParallaxOffset((int) (size.y / 2 - dpToPixels(68, mRoot)));
        mSlidingPanel.setAnchorPoint(0.5f);

        mLabel = (TextView) mSlidingPanel.findViewById(R.id.label);
        mLabelImage = (ImageView) findViewById(R.id.label_image);
        mTime = (TextView) findViewById(R.id.time);
        mAddress = (TextView) mSlidingPanel.findViewById(R.id.address);

        // Volley setup
        requestQueue = Volley.newRequestQueue(this);

        // Connect to socket
        IO.Options options = new IO.Options();
        options.query = "_id=" + mMe.getId();
        try {
            mSocket = IO.socket(SOCKET_URL, options);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        mSocket.on("updateRequest", onUpdateRequest);
        mSocket.on("updatedOne", onUpdatedOne);
        mSocket.on("socketError", onSocketError);
        mSocket.connect();

        // Kick off the process of building a GoogleApiClient and requesting the LocationServices
        // API.
        buildGoogleApiClient();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        appIsActive = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Within {@code onPause()}, we pause location updates, but leave the
        // connection to GoogleApiClient intact.  Here, we resume receiving
        // location updates if the user has requested them.

        if (mGoogleApiClient.isConnected()/* && mRequestingLocationUpdates*/) {
            startLocationUpdates();
        }
        appIsActive = true;

        mSocket.on("updatedOne", onUpdatedOne);
        mSocket.on("socketError", onSocketError);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
        appIsActive = false;
    }

    @Override
    protected void onStop() {
        //mGoogleApiClient.disconnect();
        appIsActive = false;

        // Do not handle other members' updates when not active
        mSocket.off("updatedOne", onUpdatedOne);
        mSocket.off("socketError", onSocketError);

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mSocket.disconnect();
        mSocket.off("updateRequest", onUpdateRequest);
        mSocket.off("updatedOne", onUpdatedOne);
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

        // Set listeners for map & marker click.
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        // Hide panel and reset toolbar title
        mSlidingPanel.setPanelState(PanelState.HIDDEN);
        getSupportActionBar().setTitle("");
        resetMarkerColors();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Log.d(TAG, "Long click on " + latLng);
        FavoriteDialog dialog = new FavoriteDialog();
        dialog.show(getFragmentManager(), "Test");
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        // Move camera to marker position
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(
                marker.getPosition(), Math.max(15, mMap.getCameraPosition().zoom));
        mMap.animateCamera(update, ANIMATE_SPEED, null);

        Tag tag = ((Tag) marker.getTag());
        mMarked = tag.getAssociated();

        mLocationString = null;

        // If marker is a family member
        if (mMarked.getType() == MapLocation.MEMBER) {
            // Send an update request to the marked user
            if (!mMarked.getId().equals(mMe.getId())) {
                JSONObject json = new JSONObject();
                try {
                    json.put("src", mMe.getId());
                    json.put("dest", mMarked.getId());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mSocket.emit("requestOne", json);
            }

            // TODO: put in function? Then update panel when receive update from server.
            // Get distance from marker to nearest favorite
            mDistance = Float.MAX_VALUE;
            for (Favorite favorite : mFavorites) {
                LatLng favPos = favorite.getMarker().getPosition();
                LatLng markerPos = marker.getPosition();
                double distance = calculateDistance(favPos.latitude, favPos.longitude, markerPos.latitude, markerPos.longitude);
                if (distance < mDistance && distance < 15.0) {
                    mDistance = distance;
                    mLocationString = favorite.getName();
                }
            }
        }

        updateUI();

        return true;
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");

        // If the initial location was never previously requested, we use
        // FusedLocationApi.getLastLocation() to get it. If it was previously requested, we store
        // its value in the Bundle and check for it in onCreate(). We
        // do not request it again unless the user specifically requests location updates by pressing
        // the Start Updates button.
        //
        // Because we cache the value of the initial location in the Bundle, it means that if the
        // user launches the activity,
        // moves to a new location, and then changes the device orientation, the original location
        // is displayed as the activity is re-created.
        if (mCurrentLocation == null) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions();
                return;
            }
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            // TODO remove, only for emulator
            /*mCurrentLocation = new Location("nan");
            mCurrentLocation.setLatitude(59.508594);
            mCurrentLocation.setLongitude(17.755809);*/

            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

            // Move camera to current location
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()),
                    Math.max(15, mMap.getCameraPosition().zoom));
            mMap.animateCamera(update, ANIMATE_SPEED, null);

            // Get data from server
            refresh();
        }

        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        Log.d(TAG, location.toString());

        // Update marker
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (mMe.getMarker() == null) {
            mMe.addMarker(mMap.addMarker(new MarkerOptions().position(latLng)));
        }
        mMe.getMarker().setPosition(latLng);
        mMe.setLastUpdated(new DateTime());

        // Prevent active emitting when app is closed
        if (appIsActive) {
            emitUserLocation();
        }
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
        getMenuInflater().inflate(R.menu.refresh, menu);

        for (int i = 0; i < menu.size(); i++) {
            Drawable drawable = menu.getItem(i).getIcon();
            if (drawable != null) {
                drawable.mutate();
                drawable.setColorFilter(ContextCompat.getColor(this, R.color.textColorSecondary), PorterDuff.Mode.SRC_ATOP);
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.refresh) {
            refresh();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        switch (item.getItemId()) {
            case R.id.nav_settings:

                break;
            case R.id.nav_sign_out:
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean("authenticated", false).apply();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                break;
            case 0:
                onMarkerClick(mMe.getMarker());
                break;
        }
        for (int i = 0; i < mFamily.size(); i++) {
            if (id == (i+1)) {
                onMarkerClick(mFamily.get(i).getMarker());
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    /**
     * Resets all markers to their default color.
     */
    public void resetMarkerColors() {
        for (Member m : mFamily) {
            m.setDefaultMarkerColor();
        }
        for (Favorite f : mFavorites) {
            f.setDefaultMarkerColor();
        }
        mMe.setDefaultMarkerColor();
    }

    /**
     * Emit user location without a specific destination mAddress.
     */
    public void emitUserLocation() {
        emitUserLocation(null);
    }

    /**
     * Emits the user's location to the server.
     */
    public void emitUserLocation(String dest) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();
            return;
        }
        if (!appIsActive) {
            startLocationUpdates();
        }
        Location myLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        JSONObject json = new JSONObject();
        try {
            json.put("_id", mMe.getId());
            json.put("lat", myLocation.getLatitude());
            json.put("long", myLocation.getLongitude());
            if (dest != null) {
                json.put("dest", dest);
            }
        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
        }
        mSocket.emit("updateSelf", json);
        if (!appIsActive) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            stopLocationUpdates();
        }
    }

    public void updateUI() {
        // Show panel if hidden
        if (mSlidingPanel.getPanelState() == PanelState.HIDDEN) {
            mSlidingPanel.setPanelState(PanelState.COLLAPSED);
        }

        // Edit toolbar title
        getSupportActionBar().setTitle(mMarked.getName());

        // Set panel mLabel
        mLabel.setText(mMarked.getName());

        Tag tag = (Tag) mMarked.getMarker().getTag();
        Marker marker = mMarked.getMarker();

        // Update panel
        mLabelImage.setImageDrawable(((Tag) marker.getTag()).getAssociated().getIcon(MapActivity.this));
        if (tag.getType() == MapLocation.FAVORITE) {
            mLabelImage.setColorFilter(ContextCompat.getColor(this, R.color.yellow));
            mTime.setText("");
        } else if (tag.getType() == MapLocation.MEMBER) {
            mLabelImage.setColorFilter(ContextCompat.getColor(this, R.color.white));
            mTime.setText(((Member) mMarked).getLastUpdated());
        }

        // Set marker color
        resetMarkerColors();
        marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

        // Set location info
        if (mLocationString != null) {
            mAddress.setText("Vid " + mLocationString);
        }
        else {
            // Get address from location
            try {
                Geocoder geo = new Geocoder(MapActivity.this, Locale.getDefault());
                List<Address> addresses = geo.getFromLocation(marker.getPosition().latitude, marker.getPosition().longitude, 1);
                if (addresses.size() > 0) {
                    mAddress.setText(addresses.get(0).getThoroughfare() + " " +
                            addresses.get(0).getFeatureName() + ", " +
                            addresses.get(0).getLocality());
                }
                else {
                    mAddress.setText("Waiting for Location");
                }
            } catch (Exception e) {
                //e.printStackTrace(); // getFromLocation() may sometimes fail
                Log.d(TAG, "Couldn't get mAddress data from location");
                mAddress.setText("Ingen adress");
            }
        }
    }

    /**
     * Gets all data from the server and update all members and places.
     */
    public void refresh() {

        // Emit user location
        emitUserLocation();

        String query = "?familyId=" + mMe.getFamilyId();

        // Request a JSON response from the provided URL
        JsonObjectRequest usersRequest = new JsonObjectRequest(Request.Method.GET, USER_URL + query, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Log.d(TAG, "users response: " + response);
                    if (response.getBoolean("success")) {
                        // Copy mFamily array to later remove lost markers
                        mOldFamily = new ArrayList<>(mFamily);

                        // Update members
                        JSONArray users = response.getJSONArray("users");
                        for (int i = 0; i < users.length(); i++) {
                            JSONObject user = users.getJSONObject(i);
                            String _id = user.getString("_id");
                            String name = user.getString("name");
                            String familyId = user.getString("familyId");
                            LatLng location = new LatLng(user.getDouble("lat"), user.getDouble("long"));
                            DateTime lastUpdated = DateTime.parse(user.getString("lastUpdated"));
                            boolean found = false;
                            for (Member m : mFamily) {
                                if (name.equals(m.getName())) {
                                    mOldFamily.remove(m);
                                    m.getMarker().setPosition(location);
                                    m.setLastUpdated(lastUpdated);
                                    found = true;
                                }
                            }
                            // Add new mFamily member
                            if (!found && !name.equals(mMe.getName())) {
                                Marker newMarker = mMap.addMarker(new MarkerOptions().position(location));
                                Member newMember = new Member(name, _id, familyId, newMarker);
                                mFamily.add(newMember);
                                newMember.setLastUpdated(lastUpdated);
                            }
                        }
                        // Remove lost mFamily members
                        for (Member m : mOldFamily) {
                            if (m != null) {
                                m.getMarker().remove();
                                mFamily.remove(m);
                            }
                        }
                        // Add members to navigation drawer menu
                        mFamilyMenu.clear();
                        int i = 0;
                        mFamilyMenu.add(Menu.NONE, i++, Menu.NONE, mMe.getName()).setIcon(mMe.getIcon(MapActivity.this));
                        for (Member m : mFamily) {
                            mFamilyMenu.add(Menu.NONE, i++, Menu.NONE, m.getName()).setIcon(m.getIcon(MapActivity.this));
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Snackbar.make(mRoot, "Request timed out", Snackbar.LENGTH_LONG).show();
                //Toast.makeText(MapActivity.this, "Request timed out", Toast.LENGTH_LONG).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("_id", mMe.getId());
                return headers;
            }
        };
        requestQueue.add(usersRequest);

        // Request family information
        JsonObjectRequest favoritesRequest = new JsonObjectRequest(Request.Method.GET, FAMILY_URL + mMe.getFamilyId(), null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Log.d(TAG, "favorites response: " + response);
                    if (response.getBoolean("success")) {
                        // Copy mFavorites array to later remove lost markers
                        mOldFavorites = new ArrayList<>(mFavorites);

                        // Update favorite places
                        JSONArray favorites = response.getJSONArray("favorites");
                        for (int i = 0; i < favorites.length(); i++) {
                            JSONObject favorite = favorites.getJSONObject(i);
                            String _id = favorite.getString("_id");
                            String name = favorite.getString("name");
                            LatLng location = new LatLng(favorite.getDouble("lat"), favorite.getDouble("long"));
                            boolean found = false;
                            for (Favorite f : mFavorites) {
                                if (favorite.getString("name").equals(f.getName())) {
                                    mOldFavorites.remove(f);
                                    f.getMarker().setPosition(location);
                                    found = true;
                                }
                            }
                            // Add new favorite place
                            if (!found) {
                                Marker newMarker = mMap.addMarker(new MarkerOptions().position(location));
                                Favorite newFavorite = new Favorite(name, _id, newMarker);
                                mFavorites.add(newFavorite);
                            }
                        }
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Snackbar.make(mRoot, "Request timed out", Snackbar.LENGTH_LONG).show();
                //Toast.makeText(MapActivity.this, "Request timed out", Toast.LENGTH_LONG).show();
            }
        });
        requestQueue.add(favoritesRequest);
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    private synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

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
    private void startLocationUpdates() {
        Log.d(TAG, "Start location updates");
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        Log.d(TAG, "Stop location updates");
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    private Emitter.Listener onUpdateRequest = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            MapActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String src = "";
                    try {
                        src = data.getString("src");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "src: " + src);

                    Snackbar.make(mRoot, "updateRequest received", Snackbar.LENGTH_LONG).show();
                    //Toast.makeText(MapActivity.this, "updateRequest received", Toast.LENGTH_LONG).show();

                    // Emit user location
                    emitUserLocation(src);
                }
            });
        }
    };

    private Emitter.Listener onUpdatedOne = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            MapActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    System.out.println("onUpdatedOne: " + data.toString());
                    String name;
                    LatLng location;
                    DateTime lastUpdated;
                    try {
                        name = data.getString("name");
                        location = new LatLng(data.getDouble("lat"), data.getDouble("long"));
                        lastUpdated = DateTime.parse(data.getString("lastUpdated"));
                    } catch (JSONException e) {
                        return;
                    }

                    // Update mFamily member
                    for (Member m : mFamily) {
                        if (m.getName().equals(name)) {
                            m.getMarker().setPosition(location);
                            m.setLastUpdated(lastUpdated);
                            break;
                        }
                    }
                }
            });
        }
    };

    private Emitter.Listener onSocketError = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            MapActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String error = "";
                    try {
                        error = data.getString("error");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "error: " + error);

                    Snackbar.make(mRoot, "error: " + error, Snackbar.LENGTH_LONG).show();
                    //Toast.makeText(MapActivity.this, "error: " + error, Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    /**
     * Requests permissions from the user at run-mTime.
     */
    public void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_ACCESS_COARSE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Callback for the result from requesting permissions.
     * This method is invoked for every call on
     * requestPermissions(android.app.Activity, String[], int).
     * @param requestCode The request code passed in requestPermissions(android.app.Activity, String[], int)
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_ACCESS_COARSE_LOCATION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                break;
            case MY_PERMISSIONS_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {

                }
                break;
        }
    }

    /**
     * Converts density pixels to pixels.
     * @param dp Density pixels
     * @param view The view
     * @return Number of pixels
     */
    public static float dpToPixels(int dp, View view) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, view.getResources().getDisplayMetrics());
    }

    /**
     * Calculates the distance in meters between two points on the map.
     * @param lat1
     * @param lng1
     * @param lat2
     * @param lng2
     * @return The distance in meters
     */
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000;
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return (float) (earthRadius * c);
    }

}
