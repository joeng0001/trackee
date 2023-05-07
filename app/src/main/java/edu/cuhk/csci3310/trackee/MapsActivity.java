package edu.cuhk.csci3310.trackee;


import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private LocationRequest locationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_map);
        //init google map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //requeire GPS if not enabled
        mMap = googleMap;
        if(!GPSIsOn()){
            requireGPS();
        }
        //add mark in current position and target position with path
        addTargetPosMarker();
        addCurrPosMarker();
        plotPath();
    }

    private void addTargetPosMarker(){
        Geocoder geocoder = new Geocoder(getApplicationContext());
        List<Address> addressList = null;
        try {
            //search lat lng base on the target location string and add marker on it
            addressList = geocoder.getFromLocationName(getIntent().getStringExtra("target_location"), 1);
            if (addressList != null && addressList.size() > 0) {
                Address address = addressList.get(0);
                LatLng target_latLng = new LatLng(address.getLatitude(), address.getLongitude());
                mMap.addMarker(new MarkerOptions().position(target_latLng).title(getIntent().getStringExtra("target_location")));
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void addCurrPosMarker(){
        if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED && GPSIsOn()) {
                //obtain current position ,move camera and add marker on it
                locationRequest = LocationRequest.create();
                LocationServices.getFusedLocationProviderClient(MapsActivity.this)
                        .requestLocationUpdates(locationRequest, new LocationCallback() {
                            @Override
                            public void onLocationResult(@NonNull LocationResult locationResult) {
                                super.onLocationResult(locationResult);
                                LocationServices.getFusedLocationProviderClient(MapsActivity.this)
                                        .removeLocationUpdates(this);
                                if (locationResult != null && locationResult.getLocations().size() >0){
                                    int index = locationResult.getLocations().size() - 1;
                                    double latitude = locationResult.getLocations().get(index).getLatitude();
                                    double longitude = locationResult.getLocations().get(index).getLongitude();
                                    LatLng curr_latLng = new LatLng(latitude, longitude);
                                    mMap.addMarker(new MarkerOptions().position(curr_latLng).title("Current Location"));
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(curr_latLng, 12));
                                }
                            }
                        }, Looper.getMainLooper());
        }

    }

        private boolean GPSIsOn(){
            //return GPS on result
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }


        private void requireGPS(){
            //create dialog to require turning on GPS
            AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
            builder.setTitle("GPS not yet enabled");
            builder.setMessage("Enable GPS for Application");
            builder.setPositiveButton("Enable GPS", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            });
            builder.setNegativeButton("Reject", null);
            builder.create().show();
        }

        private void plotPath(){
            try{
                SharedPreferences resource = this.getSharedPreferences("store", MODE_PRIVATE);
                JSONArray arr=new JSONArray(resource.getString("path_record","[]"));
                //find index that match the start/end time of that job->the intent start/end time
                Integer start_index=0;
                Integer end_index=0;
                for(Integer i=0;i<arr.length();i++){
                    JSONObject obj=arr.getJSONObject(i);
                    if(obj.get("time").toString().equals(getIntent().getStringExtra("start_time"))){
                        start_index=i;
                    }
                    if(obj.get("time").toString().equals(getIntent().getStringExtra("end_time"))){
                        end_index=i;
                    }
                }
                //then put them into a latlng list and plot the path
                List<LatLng> lat_lng_list = new ArrayList<>();
                for (int i = start_index; i <= end_index; i++) {
                    JSONObject obj=arr.getJSONObject(i);
                    lat_lng_list.add(new LatLng((double)obj.get("lat"), (double)obj.get("lng")));
                }
                PolylineOptions polylineOptions = new PolylineOptions()
                        .addAll(lat_lng_list)
                        .width(20)
                        .color(Color.RED);
                mMap.addPolyline(polylineOptions);
            }catch (Exception e){
                e.printStackTrace();
            }
        }



    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }


}
