package com.example.locationapi;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;

import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int CHECH_SETTINGS_CODE = 111;
    private static final int REQUEST_LOCATION_PERMISSON = 222;

    private Button startLocationUpdatesButton;
    private Button stopLocationUpdatesButton;
    private TextView locationTextView;
    private TextView locationUpdateTimeTextView;

    private FusedLocationProviderClient fusedLocationClient;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;
    private Location currentLocation;

    private boolean isLocationUpdatesActive;
    private String locationUpdateTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startLocationUpdatesButton = findViewById(R.id.startLocationUpdatesButton);
        stopLocationUpdatesButton = findViewById(R.id.stopLocationUpdatesButton);
        locationTextView = findViewById(R.id.locationTextView);
        locationUpdateTimeTextView = findViewById(R.id.locationUpdateTimeTextView);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);


        startLocationUpdatesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLocationUpdates();
            }
        });

        stopLocationUpdatesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopLocationUpdates();
            }
        });

        buildLiocationReqest();
        buildLocationCallback();
        buildLocationSettingsReqest();
    }

    private void stopLocationUpdates() {
        if (!isLocationUpdatesActive) {
            return;
        }

        fusedLocationClient.removeLocationUpdates(locationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task <Void> task) {
                        isLocationUpdatesActive = false;
                        startLocationUpdatesButton.setEnabled(true);
                        stopLocationUpdatesButton.setEnabled(false);

                    }
                });
    }

    private void startLocationUpdates() {
        isLocationUpdatesActive = true;
        startLocationUpdatesButton.setEnabled(false);
        stopLocationUpdatesButton.setEnabled(true);

        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(this,
                        new OnSuccessListener<LocationSettingsResponse>() {
                            @RequiresApi(api = Build.VERSION_CODES.M)
                            @Override
                            public void onSuccess(
                                    LocationSettingsResponse locationSettingsResponse) {


                                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                        && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    // TODO: Consider calling
                                    //    Activity#requestPermissions
                                    // here to request the missing permissions, and then overriding
                                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                    //                                          int[] grantResults)
                                    // to handle the case where the user grants the permission. See the documentation
                                    // for Activity#requestPermissions for more details.
                                    return;
                                }
                                fusedLocationClient.requestLocationUpdates(
                                        locationRequest,
                                        locationCallback,
                                        Looper.myLooper()
                                );
                                updateLocationUI();
                            }
                        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();

                        switch (statusCode) {
                            case LocationSettingsStatusCodes
                                    .RESOLUTION_REQUIRED:
                                try {
                                    ResolvableApiException resolvableApiException =
                                            (ResolvableApiException) e;
                                    resolvableApiException.startResolutionForResult(
                                            MainActivity.this,
                                            CHECH_SETTINGS_CODE
                                    );
                                } catch (IntentSender.SendIntentException sie) {
                                    sie.printStackTrace();
                                }
                                break;

                            case LocationSettingsStatusCodes.
                                    SETTINGS_CHANGE_UNAVAILABLE:
                                String message = "Adjuct location settings on your device";
                                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                                isLocationUpdatesActive = false;
                                startLocationUpdatesButton.setEnabled(true);
                                stopLocationUpdatesButton.setEnabled(false);
                        }
                        updateLocationUI();

                    }
                });
    }

    private void buildLocationSettingsReqest() {
        LocationSettingsRequest.Builder builder =
                new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();
    }

    private void buildLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                currentLocation = locationResult.getLastLocation();

                updateLocationUI();
            }
        };
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case CHECH_SETTINGS_CODE:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.d("MainActivity", "User has agreed to change location settings");
                        startLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.d("MainActivity", "User has not agreed to change location settings");
                        isLocationUpdatesActive = false;
                        startLocationUpdatesButton.setEnabled(true);
                        stopLocationUpdatesButton.setEnabled(false);
                        updateLocationUI();
                        break;
                }
                break;
        }
    }

    private void updateLocationUI() {

        if (currentLocation != null) {
            locationTextView.setText("" +
                    currentLocation.getLatitude() + "/" +
                    currentLocation.getLongitude()
            );
            locationUpdateTimeTextView.setText(
                    DateFormat.getTimeInstance().format(new Date())
            );

        }
    }

    private void buildLiocationReqest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isLocationUpdatesActive && checkLocationPermission()) {
            startLocationUpdates();
        } else if (!checkLocationPermission()) {
            requestLocationPermission();
        }
    }

    private void requestLocationPermission() {
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (shouldProvideRationale) {
            showSnackBar("Location permission is needed for app functionflity",
                    "Ok",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(
                                    MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_LOCATION_PERMISSON
                            );
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSON
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_PERMISSON) {
            if (grantResults.length <= 0) {
                Log.d("onRequestPermissionsRes", "Request was cancald");
            }
        } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (isLocationUpdatesActive) {
                startLocationUpdates();
            } else {
                showSnackBar(
                        "Turn on location settings",
                        "settings",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                );
                                Uri uri = Uri.fromParts(
                                        "package",
                                        BuildConfig.APPLICATION_ID,
                                        null
                                );
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        }
                );
            }
        }
    }

    private void showSnackBar(final String mainText, final String action, View.OnClickListener listener) {
        Snackbar.make(findViewById(android.R.id.content),
                mainText,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(action, listener).show();
    }

    private boolean checkLocationPermission() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }
}
