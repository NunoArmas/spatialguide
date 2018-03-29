package com.paydayme.spatialguide.ui;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.paydayme.spatialguide.R;
import com.paydayme.spatialguide.core.Constant;
import com.paydayme.spatialguide.core.api.SGApiClient;
import com.paydayme.spatialguide.model.Point;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.paydayme.spatialguide.core.Constant.BASE_URL;

public class MapActivity extends AppCompatActivity
        implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private static final String TAG = "MapActivity";

    private GoogleMap googleMap;
    private boolean isMapReady = false;
    private boolean firstTime = true;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private Marker currentLocationMarker;
    private long startTime = SystemClock.elapsedRealtime(), endTime, deltaTime;
    private AlertDialog dialog;
    private int routeSelected;

    private SGApiClient sgApiClient;
    private List<Point> points = new ArrayList<>();
    private Point lastPoint = new Point();

    /**
     * For dummy purposes
     *
     * - dummyLocationMarker (the fixed marker on map)
     * - dummyLocation (the location associated with the marker)
     * - mp (the media player that will play the audio file)
     */
    private MarkerOptions dummyLocationMarker;
    private Location dummyLocation;
    private MediaPlayer mp;

    // Reference the views
    @BindView(R.id.tvLatitude) TextView tvLatitude;
    @BindView(R.id.tvLongitude) TextView tvLongitude;
    @BindView(R.id.tvDistance) TextView tvDistance;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.drawer_layout) DrawerLayout drawer;
    @BindView(R.id.nav_view) NavigationView navigationView;
    @BindView(R.id.btn_info) Button btnInfo;
    @BindView(R.id.btn_play) Button btnPlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Binding the views
        ButterKnife.bind(this);

        // Get route selected on the previous screen
        Intent intent = getIntent();
        routeSelected = intent.getIntExtra("route", -1);
        Log.d(TAG, "route selected: " + routeSelected);

        init();

        /**
         * For dummy purposes
         * - creating the media player that will play the audio file on "raw" folder on "res"
         */
        mp = MediaPlayer.create(this, R.raw.tadahh);
    }

    private void init() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Get the google maps client
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Setting action bar to the toolbar, removing text
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);

        // Add listener to the hamburger icon on left
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // Set listener of navigation view to this class
        navigationView.setNavigationItemSelectedListener(this);

        // Set button info click listener
        btnInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInfoDialog(false, lastPoint);
            }
        });

        // Set button play click listener
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mp.isPlaying()) mp.start();
            }
        });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        sgApiClient = retrofit.create(SGApiClient.class);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(isMapReady) {
            doLocationRequests();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //stop location updates when Activity is no longer active
        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
        }
    }

    /**
     * What to do when the back button is pressed
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            //Ask the user if they want to quit
            AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                    .setTitle(getString(R.string.exit))
                    .setMessage(getString(R.string.exit_prompt))
                    .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            //Stop the activity
                            finish();
                        }

                    })
                    .setNegativeButton(getString(android.R.string.no), null)
                    .setCancelable(false)
                    .show();
            TextView textView = (TextView) dialog.findViewById(android.R.id.message);
            Typeface tf = ResourcesCompat.getFont(getApplicationContext(), R.font.catamaran);
            textView.setTypeface(tf);
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

        if (id == R.id.nav_website) {
            String url = "http://xcoa.av.it.pt/~pei2017-2018_g09/";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        } else if(id == R.id.nav_route){
            startActivity(new Intent(MapActivity.this, RouteActivity.class));
            finish();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * onMapReady
     *
     * What to do when the google map is ready when it loads.
     * We set a location request with the minimum and maximum refresh rate, as well as a
     * minimum displacement (the least distance for location to refresh), and set priority of
     * processing/polling to save some battery.
     *
     * Then we check if we have location services permissions, if we do not have them
     * we have to ask user if they give permission. Then we handle the response.
     *
     * Dummy purposes:
     * Add the dummy location marker on a fixed position. The code explains by itself.
     *
     * @param googMap (the GoogleMap declared)
     */
    @Override
    public void onMapReady(GoogleMap googMap) {
        isMapReady = true;
        googleMap = googMap;
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Log.d(TAG, "onMarkerClick: marker " + marker.getTag());

                /**
                 * TODO check if user clicked on user marker or on location marker
                 * Idea: if clicked on location marker display the info about it
                 */

                return false;
            }
        });

        doLocationRequests();
    }

    private void doLocationRequests() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(60000); // one minute interval
        locationRequest.setFastestInterval(1500); // 15 seconds
        locationRequest.setSmallestDisplacement(2); // 2 meters
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY); // for battery efficiency

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
                googleMap.setMyLocationEnabled(true);
            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        }
        else {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
            googleMap.setMyLocationEnabled(true);
        }

        /**
         * DUMMY LOCATION MARKER
         */
//        dummyLocationMarker = new MarkerOptions();
//        //FANEPAO
////        dummyLocationMarker.position(new LatLng(40.635625, -8.647693));
//
//        //DETI
//        dummyLocationMarker.position(new LatLng(40.633135, -8.659483));
//        dummyLocationMarker.title("DETI");
//        dummyLocationMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
//        Marker dummyMarker = googleMap.addMarker(dummyLocationMarker);
//        dummyMarker.setTag("location");
//
//        dummyLocation = new Location("");
////        // Fanepao
////        dummyLocation.setLatitude(40.635625);
////        dummyLocation.setLongitude(-8.647693);
//
////         DETI
//        dummyLocation.setLatitude(40.633135);
//        dummyLocation.setLongitude(-8.659483);
    }

    /**
     * check for location permissions and handle them
     */
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.location_permission_title))
                        .setMessage(getString(R.string.location_permission_prompt))
                        .setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION );
                            }
                        })
                        .create()
                        .show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION );
            }
        }
    }

    /**
     * handle the asked permissions result
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
                        googleMap.setMyLocationEnabled(true);
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
//                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }

    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                Log.d(TAG, "Location: " + location.getLatitude() + " " + location.getLongitude());
                if (currentLocationMarker != null) {
                    currentLocationMarker.remove();
                }
                Point minPoint = new Point();
                Location minLocation = new Location("");
                float minDistance = Float.MAX_VALUE;

                // Place current location marker
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title("Current Position");
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.mipmap.logo_launcher));
                currentLocationMarker = googleMap.addMarker(markerOptions);
                currentLocationMarker.setTag("me");

                // move map camera
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));

                for(Point point : points) {
                    Location loc = new Location("");
                    loc.setLongitude(point.getPointLongitude());
                    loc.setLatitude(point.getPointLatitude());

                    if(loc.distanceTo(location) < minDistance) {
                        minDistance = loc.distanceTo(location);
                        minLocation = loc;
                        minPoint = point;
                    }
                }

                // update the textviews
                tvLatitude.setText("Latitude: " + String.valueOf(location.getLatitude()));
                tvLongitude.setText("Longitude: " + String.valueOf(location.getLongitude()));
                tvDistance.setText("Distance: " + String.valueOf(location.distanceTo(minLocation)) + "m");

                // if the current location is within the trigger area, we play the audio file
                // to avoid playing multiple times, only play if the time elapsed greater than 1 minute
                if(location.distanceTo(minLocation) <= Constant.TRIGGER_AREA_DISTANCE) {
                    lastPoint = minPoint;
                    currentLocationMarker.remove();
                    markerOptions.title("You are at " + minPoint.getPointName());
                    markerOptions.snippet(minPoint.getPointName());
//                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.mipmap.logo_launcher));
                    currentLocationMarker = googleMap.addMarker(markerOptions);
                    currentLocationMarker.setTag("me");

                    endTime = SystemClock.elapsedRealtime();
                    deltaTime = endTime - startTime;

                    double secondsElapsed = deltaTime / 1000.0;

                    Log.d(TAG, "startTime: " + startTime);
                    Log.d(TAG, "endTime: " + endTime);
                    Log.d(TAG, "secondsElapsed: " + secondsElapsed);

                    if(firstTime || secondsElapsed >= (60 * 1)) {
                        // play the sound
                        mp.start();

                        // show the dialog with info of location
                        showInfoDialog(true, minPoint);

                        /**
                         * TODO: Change this firsttime mechanism. Its dumb.
                         * Idea: check if we already visited the point, so we need to show the
                         * dialog info, otherwise check the time difference
                         */
                        firstTime = false;

                        startTime = endTime;
                    }
                }
            }
        }
    };

    // API STUFF
    private void getRoutePoints(int id) {
        Call<List<Point>> call = sgApiClient.getRoutePoints(id);

        call.enqueue(new Callback<List<Point>>() {
            @Override
            public void onResponse(Call<List<Point>> call, Response<List<Point>> response) {
                Log.d(TAG, "onResponse - getRoutePoints");

            }

            @Override
            public void onFailure(Call<List<Point>> call, Throwable t) {

            }
        });
    }

    private void getPoints() {
        Call<List<Point>> call = sgApiClient.getPoints();

        call.enqueue(new Callback<List<Point>>() {
            @Override
            public void onResponse(Call<List<Point>> call, Response<List<Point>> response) {
                Log.d(TAG, "onResponse");

                for(int i = 0; i < response.body().size(); i++) {
                    points.add(response.body().get(i));
                }

                /**
                 * PUTTING ALL POINTS ON MAP
                 */

                for(int i = 0; i < points.size(); i++) {
                    Point point = points.get(i);
                    MarkerOptions location = new MarkerOptions();
                    location.position(new LatLng(point.getPointLatitude(), point.getPointLongitude()));
                    location.title(point.getPointName());
                    location.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                    googleMap.addMarker(location);
                }
            }

            @Override
            public void onFailure(Call<List<Point>> call, Throwable t) {
                Log.d(TAG, "onFailure: " + t.getMessage());
            }
        });
    }

    private void showInfoDialog(boolean inLocation, final Point point) {
        // Check if the dialog exists and if its showing
        if(dialog != null && dialog.isShowing()) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_layout, null);

        TextView dialogTitle = (TextView) view.findViewById(R.id.dialog_title);
        dialogTitle.setText(point.getPointName());

        TextView dialogText = (TextView) view.findViewById(R.id.dialog_text);
//        dialogText.setText("O Departamento de Eletrónica, Telecomunicações e Informática (DETI) foi fundado em 1974, " +
//                "com o nome de Departamento de Eletrónica e Telecomunicações, tendo sido um dos primeiros " +
//                "departamentos a iniciar atividade após a criação da Universidade de Aveiro em 1973. " +
//                "Em 2006 foi alterada a sua designação por forma a espelhar a atividade existente no Departamento na área da Informática.");
        dialogText.setText(point.getPointDescription());

//        ImageView dialogImage = (ImageView) view.findViewById(R.id.dialog_image);
//        dialogImage.setImageResource(R.drawable.deti);

        //Ask the user if they want to quit
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setView(view)
                .setIcon(R.mipmap.ic_info)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(true);

        if(!point.getPointURL().isEmpty() || point.getPointURL() != null) {
            builder.setNeutralButton("Learn more", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String url = point.getPointURL();
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                }
            });
        }

        if (inLocation)
            builder.setTitle("You are at " + point.getPointName());
        else
            builder.setTitle(point.getPointName());

        dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = 900;
        lp.height = 1300;
//        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.CENTER;

        dialog.show();
        dialog.getWindow().setAttributes(lp);

        TextView textView = (TextView) dialog.findViewById(android.R.id.message);
        Typeface tf = ResourcesCompat.getFont(getApplicationContext(), R.font.catamaran);
        textView.setTypeface(tf);
    }
}
