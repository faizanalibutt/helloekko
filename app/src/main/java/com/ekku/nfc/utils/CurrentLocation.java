package com.ekku.nfc.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.tasks.Task;

import java.util.List;

public class CurrentLocation implements LocationListener {

    private LocationResultListener locationResultListener;
    private LocationRequest mLocationRequest;
    private Context mContext;
    public static final int REQUEST_LOCATION = 101;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationSettingsRequest.Builder locationSettingsRequest;

    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    /* location manager settings */
    //private LocationManager mLocationManager;
    //private boolean isLocationSettingsShown = false;

    public CurrentLocation(Context context) {
        this.mContext = context;
        if (mContext != null) {
            fusedLocationProviderClient = new FusedLocationProviderClient(mContext);
            //mLocationManager = (LocationManager) mContext.getApplicationContext()
            // .getSystemService(Context.LOCATION_SERVICE);
        }
    }

    /*public LocationManager getLocationManager() {
        return mLocationManager;
    }

    public boolean getLocation(LocationResultListener result, FragmentActivity activity) {
        getLocation(result, activity, false);

        return true;
    }

    public boolean getLocation(LocationResultListener result, FragmentActivity activity, boolean subView) {

        if (isLocationSettingsShown)
            return false;

        locationResultListener = result;
        isLocationSettingsShown = true;

        if (isGPSEnabled(mContext) && !subView) {
            *//*fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null && locationResultListener != null) {
                    locationResultListener.gotLocation(location);
                }else{
                    requestLocation();
                }
            });*//*

            requestLocation();
        } else if (isGPSEnabled(mContext) && subView) {
            requestLocation();
        } else if (!subView) {
            gpsLocationSetting();
        }


        return true;
    }*/

    public boolean getLocation(LocationResultListener result) {

        locationResultListener = result;
        if (locationResultListener != null) {
            if (isGPSEnabled(mContext)) {
           /* fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null && locationResultListener != null) {
                    locationResultListener.gotLocation(location);
                }else{
                    requestLocation();
                }
            });*/
                requestLocation();
            } else {
                gpsLocationSetting();
            }
        }


        return true;
    }

    @Override
    public void onLocationChanged(Location location) {
        locationResultListener.gotLocation(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public interface LocationResultListener {
        void gotLocation(Location location);
    }

    private void gpsLocationSetting() {

        /*new AlertDialog.Builder(mContext)
                .setMessage(R.string.mesg_locationDisabledSelfHotspot)
                .setNegativeButton(R.string.butn_cancel, null)
                .setPositiveButton(R.string.butn_locationSettings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((FragmentActivity) mContext).startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                *//*.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)*//*, MainActivity.LOCATION_SERVICE_RESULT);
                    }
                })
                .show();*/

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        locationSettingsRequest = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);

        Task<LocationSettingsResponse> task =
                LocationServices.getSettingsClient(mContext).checkLocationSettings(locationSettingsRequest.build());


        task.addOnSuccessListener((Activity) mContext, locationSettingsResponse -> {
            // All location settings are satisfied. The client can initialize
            // location requests here.
            // ...
        });

        task.addOnFailureListener((Activity) mContext, e -> {
            if (e instanceof ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult((Activity) mContext,
                            REQUEST_LOCATION);
                } catch (IntentSender.SendIntentException sendEx) {
                    // Ignore the error.
                }
            }
        });
    }

    /*public boolean isGPSEnabled(Context mContext) {
        return mLocationManager != null && mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }*/

    private boolean isGPSEnabled(Context mContext) {
        LocationManager locationManager = (LocationManager)
                mContext.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } else {
            return false;
        }
    }


    @SuppressLint("MissingPermission")
    private void requestLocation() {

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (NetworkUtils.isOnline(mContext))
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        else
            locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);


        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResults) {
                if (locationResults == null) {
                    return;
                }
                List<Location> locationList = locationResults.getLocations();
                if (locationList.size() > 0) {
                    Location location = locationList.get(locationList.size() - 1);
                    if (location != null && locationResultListener != null) {
                        locationResultListener.gotLocation(location);
                    }
                }
            }

        };

        fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());

        /*mLocationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 0, 0, this);*/

    }


    public void removeFusedLocationClient() {

        if (fusedLocationProviderClient != null && locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }

    }


}
