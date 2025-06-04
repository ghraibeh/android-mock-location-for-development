package amotz.example.com.mocklocationfordeveloper;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener {

    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 1;
    String TAG = "MainActivity";

    /* use for test */
    final float dummyLat = -10;
    final float dummylong = 10;
    GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private GoogleMap mMap;
    private double selectedLat = dummyLat;
    private double selectedLon = dummylong;
    private Marker selectedMarker;

    private boolean recording = false;
    private List<LatLng> currentPath = new ArrayList<>();
    private Polyline currentPolyline;
    private PathModel selectedPath;
    private PathStorage pathStorage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pathStorage = new PathStorage(this);

        View startBtn = findViewById(R.id.startPathButton);
        if (startBtn != null) {
            startBtn.setVisibility(View.GONE);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        if (!isMockSettingsON()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("In order to use this app you must enable mock location do you want to enable it now?").setTitle("Mock location is not enable");
            builder.setPositiveButton("yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                    startActivity(i);
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    finish();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        createLocationRequest();

    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();

    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    private boolean isMockSettingsON() {
        boolean isMockLocation = false;
        try
        {
            //if marshmallow
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                AppOpsManager opsManager = (AppOpsManager) this.getSystemService(Context.APP_OPS_SERVICE);
                isMockLocation = (opsManager.checkOp(AppOpsManager.OPSTR_MOCK_LOCATION, android.os.Process.myUid(), BuildConfig.APPLICATION_ID)== AppOpsManager.MODE_ALLOWED);
            }
            else
            {
                // in marshmallow this will always return true
                isMockLocation = !android.provider.Settings.Secure.getString(this.getContentResolver(), "mock_location").equals("0");
            }
        }
        catch (Exception e)
        {
            return isMockLocation;
        }

        return isMockLocation;

    }

    public void buttonCmd(View view) {
    }

    public void buttonInterval(View view) {
    }

    public void testMock(View view) { // On some version do like this
        triggerMockLocation();
    }

    private void triggerMockLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            askRunTimePermissions();
            return;
        }
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            Intent intent = new Intent();
            intent.putExtra("lat", String.valueOf(selectedLat));
            intent.putExtra("lon", String.valueOf(selectedLon));
            intent.setAction("send.mock");
            sendBroadcast(intent);
        } else {
            Toast.makeText(this, "Failed - Google play services is not connected", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMock(double lat, double lon) {
        Intent intent = new Intent();
        intent.putExtra("lat", String.valueOf(lat));
        intent.putExtra("lon", String.valueOf(lon));
        intent.setAction("send.mock");
        sendBroadcast(intent);
    }

    public void recordPath(View view) {
        if (!recording) {
            recording = true;
            currentPath.clear();
            if (currentPolyline != null) {
                currentPolyline.remove();
            }
            ((android.widget.Button)view).setText(getString(R.string.save_path));
        } else {
            final android.widget.EditText input = new android.widget.EditText(this);
            new AlertDialog.Builder(this)
                    .setTitle("Path name")
                    .setView(input)
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String name = input.getText().toString();
                            pathStorage.savePath(new PathModel(name, new ArrayList<>(currentPath)));
                            recording = false;
                            ((android.widget.Button)view).setText(getString(R.string.record_path));
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            recording = false;
                            ((android.widget.Button)view).setText(getString(R.string.record_path));
                        }
                    }).show();
        }
    }

    public void selectPath(View view) {
        final List<PathModel> paths = pathStorage.getPaths();
        if (paths.isEmpty()) {
            Toast.makeText(this, "No paths", Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence[] names = new CharSequence[paths.size()];
        for (int i = 0; i < paths.size(); i++) {
            names[i] = paths.get(i).name;
        }
        new AlertDialog.Builder(this)
                .setTitle("Choose path")
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedPath = paths.get(which);
                        if (currentPolyline != null) currentPolyline.remove();
                        PolylineOptions opts = new PolylineOptions().addAll(selectedPath.points);
                        currentPolyline = mMap.addPolyline(opts);
                        View startBtn = findViewById(R.id.startPathButton);
                        if (startBtn != null) startBtn.setVisibility(View.VISIBLE);
                    }
                }).show();
    }

    public void startPath(View view) {
        if (selectedPath == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (LatLng p : selectedPath.points) {
                    sendMock(p.latitude, p.longitude);
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                }
            }
        }).start();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void askRunTimePermissions() {
        ActivityCompat.requestPermissions(this
                ,new  String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                MY_PERMISSIONS_REQUEST_FINE_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this,"Got permission location ",Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(this,"No location permissions",Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(0);
        mLocationRequest.setSmallestDisplacement(1);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location.getLatitude() == dummyLat && location.getLongitude() == dummylong) {
            Toast.makeText(this,"Mock location application is working",Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(this,String.format(Locale.US,"setting mock to Latitude=%f, Longitude=%f Altitude=%f Accuracy=%f",
                    location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getAccuracy()),Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this);
        LatLng start = new LatLng(dummyLat, dummylong);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 3f));
        if (selectedPath != null) {
            PolylineOptions opts = new PolylineOptions().addAll(selectedPath.points);
            currentPolyline = mMap.addPolyline(opts);
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (recording) {
            currentPath.add(latLng);
            if (currentPolyline != null) {
                currentPolyline.remove();
            }
            currentPolyline = mMap.addPolyline(new PolylineOptions().addAll(currentPath));
        } else {
            selectedLat = latLng.latitude;
            selectedLon = latLng.longitude;
            if (selectedMarker != null) {
                selectedMarker.remove();
            }
            selectedMarker = mMap.addMarker(new MarkerOptions().position(latLng));
        }

        selectedMarker = mMap.addMarker(new MarkerOptions().position(latLng));
        // Trigger mock location immediately after selecting a point on the map
        triggerMockLocation();

    }
}

