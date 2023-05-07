package edu.cuhk.csci3310.trackee;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.text.Layout;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
//import android.widget.SearchView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.widget.SearchView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import android.Manifest;
public class DetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    //String to store in share SharedPreferences
    private String title;
    private String remarks;
    private String end_time;
    private String start_time;
    private String location;


    private JSONArray job_list;
    private Boolean creation_flag;
    private Integer job_index;

    //reference to job_detail.xml
    private EditText edit_job_title;
    private EditText edit_remarks;
    private TimePicker start_time_picker;
    private TimePicker end_time_picker;
    private TextView start_time_view;
    private TextView end_time_view;
    private SearchView searchView;
    private SupportMapFragment mapFragment;
    private ViewGroup layout;

    private GoogleMap mMap;

    private LocationRequest locationRequest;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.job_detail);
        //get reference of element in detail page
        referenceInit();
        //store data from intent to string
        stringDataInit();
        //init title,remark with placeholder
        titleAndRemarkInit();
        //init timeView,timePicker with placeholder
        timeViewAndPickerInit();
        //init google map
        mapInit();
        //init search query submit handler
        searchViewQuerySubmitInit();
        //init search View on focus handler
        searchViewOnFocusInit();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }



    
    public void saveEntry(View view){
        //click handler of save button
        //perform time checking,end time cannot equal or before start time
        if(end_time_picker.getCurrentHour()<start_time_picker.getCurrentHour()||
                (end_time_picker.getCurrentHour()==start_time_picker.getCurrentHour()&&
                        end_time_picker.getCurrentMinute()<=start_time_picker.getCurrentMinute())){
            Toast.makeText(getApplicationContext(), "End time cannot early than start time", Toast.LENGTH_SHORT).show();
            return;
        }
        //acquire lock, avoid race condition with w
        while(MainActivity.lock);
        MainActivity.lock=true;
        //init the share preference
        SharedPreferences resource = this.getSharedPreferences("store", MODE_PRIVATE);
        SharedPreferences.Editor editor = resource.edit();
        //obtain input
        title=edit_job_title.getText().toString();
        remarks=edit_remarks.getText().toString();
        start_time=start_time_picker.getCurrentHour()+":"+start_time_picker.getCurrentMinute();
        end_time=end_time_picker.getCurrentHour()+":"+end_time_picker.getCurrentMinute();
        location=searchView.getQuery().toString();
        //save to an json object and store to list
        JSONObject obj=new JSONObject();
        try {
            obj.put("title", title);
            obj.put("remarks", remarks);
            obj.put("start_time", start_time);
            obj.put("end_time", end_time);
            obj.put("location",location);
            job_list = new JSONArray(resource.getString("job_list", "[]"));
            if (creation_flag) {
                //if create,just add to array
                job_list.put(obj);
            } else {
                //else replace the original one
                job_list.put(job_index, obj);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        //convert job list back to string and store
        String job_list_in_string = job_list.toString();
        editor.putString("job_list",job_list_in_string);
        editor.commit();

        //go back to MainActivity
        try {
            Intent intent = new Intent();
            intent.putExtra("result", "Hello from SecondActivity!");
            intent.putExtra("job_list", job_list_in_string);
            setResult(RESULT_OK, intent);
            finish();
        }catch(Exception e){
            e.printStackTrace();
        }
        MainActivity.lock=false;
    }

    public void cancelEntry(View view){
        //reset fields to initial state
        Intent intent = getIntent();
        //reset as the initial state->the intent state
        edit_job_title.setText(intent.getStringExtra("job"));
        edit_remarks.setText(intent.getStringExtra("remarks"));
        //base edit mode or creation mode,do diff operation
        if(creation_flag){
            //reset time view base on current time
            Calendar currentTime = Calendar.getInstance();
            int hour = currentTime.get(Calendar.HOUR_OF_DAY);
            int minute = currentTime.get(Calendar.MINUTE);
            start_time_view.setText(hour+":"+minute);
            end_time_view.setText(hour+":"+minute);
            start_time_picker.setCurrentHour(hour);
            start_time_picker.setCurrentMinute(minute);
            end_time_picker.setCurrentHour(hour);
            end_time_picker.setCurrentMinute(minute);
        }else{
            //reset time view base on intent value->the job data
            start_time_view.setText(intent.getStringExtra("start_time"));
            end_time_view.setText(intent.getStringExtra("end_time"));
            start_time_picker.setCurrentHour(Integer.parseInt(start_time.split(":")[0]));
            start_time_picker.setCurrentMinute(Integer.parseInt(start_time.split(":")[1]));
            end_time_picker.setCurrentHour(Integer.parseInt(end_time.split(":")[0]));
            end_time_picker.setCurrentMinute(Integer.parseInt(end_time.split(":")[1]));
        }
    }

    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //trigger search query for locate google map into default position->Kwun Tong
        searchView.setQuery(location,true);
    }

    private void referenceInit(){
        edit_job_title=findViewById(R.id.edit_job_title);
        edit_remarks=findViewById(R.id.edit_remarks);
        //obtain textview of time
        start_time_view=findViewById(R.id.start_time);
        end_time_view=findViewById(R.id.end_time);
        //obtain time picker
        start_time_picker= (TimePicker) this.findViewById(R.id.start_time_picker);
        end_time_picker = (TimePicker) this.findViewById(R.id.end_time_picker);
        //obtain other
        layout = findViewById(R.id.detail_layout);
        searchView = findViewById(R.id.search_view);
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

    }
    private void stringDataInit(){
        //update the placeholder base on intent data
        Intent intent =getIntent();
        title=intent.getStringExtra("title");
        remarks=intent.getStringExtra("remarks");
        end_time=intent.getStringExtra("end_time");
        start_time=intent.getStringExtra("start_time");
        creation_flag=intent.getBooleanExtra("creation_flag",true);
        job_index=intent.getIntExtra("job_index",-1);
        location=intent.getStringExtra("location");
    }
    private void titleAndRemarkInit(){
        //init placeholder of title and remark
        edit_job_title.setText(title);
        edit_remarks.setText(remarks);
    }
    private void timeViewAndPickerInit(){
        //set time picker to 24hours mode
        start_time_picker.setIs24HourView(true);
        end_time_picker.setIs24HourView(true);
        //add time change listener for start time and end time that set time to textView
        start_time_picker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                start_time_view.setText("Start Time: "+start_time_picker.getCurrentHour()+":"+start_time_picker.getCurrentMinute());
            }
        });
        end_time_picker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                end_time_view.setText("End Time: "+end_time_picker.getCurrentHour()+":"+end_time_picker.getCurrentMinute());
            }
        });

        //init the textview value and time picker value
        if(creation_flag){
            start_time_view.setText("Start Time: "+start_time_picker.getCurrentHour()+":"+start_time_picker.getCurrentMinute());
            end_time_view.setText("End Time: "+end_time_picker.getCurrentHour()+":"+end_time_picker.getCurrentMinute());
        }else{
            start_time_view.setText("Start Time: "+start_time);
            end_time_view.setText("End Time: "+end_time);
            start_time_picker.setCurrentHour(Integer.parseInt(start_time.split(":")[0]));
            start_time_picker.setCurrentMinute(Integer.parseInt(start_time.split(":")[1]));
            end_time_picker.setCurrentHour(Integer.parseInt(end_time.split(":")[0]));
            end_time_picker.setCurrentMinute(Integer.parseInt(end_time.split(":")[1]));
        }
    }
    private void mapInit(){
        mapFragment.getMapAsync(this);
    }
    private void searchViewQuerySubmitInit(){
        //query listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //instantiate a geocoder for submiting query
                Geocoder geocoder = new Geocoder(getApplicationContext());
                List<Address> addressList = null;
                try {
                    //use List store query resu;t
                    addressList = geocoder.getFromLocationName(query, 1);
                    if (addressList != null && addressList.size() > 0) {
                        //after obtain location
                        Address address = addressList.get(0);
                        LatLng target_latLng = new LatLng(address.getLatitude(), address.getLongitude());
                        //clear map and add marker to location of the query place
                        mMap.clear();
                        mMap.addMarker(new MarkerOptions().position(target_latLng).title(query));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target_latLng, 12));
                        if(mMap!=null){
                            if (ContextCompat.checkSelfPermission(DetailActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                                    == PackageManager.PERMISSION_GRANTED) {
                                if(GPSIsOn()){
                                    //make request for current position
                                    locationRequest = LocationRequest.create();
                                    LocationServices.getFusedLocationProviderClient(DetailActivity.this)
                                        .requestLocationUpdates(locationRequest, new LocationCallback() {
                                            @Override
                                            public void onLocationResult(@NonNull LocationResult locationResult) {
                                                super.onLocationResult(locationResult);
                                                LocationServices.getFusedLocationProviderClient(DetailActivity.this)
                                                        .removeLocationUpdates(this);
                                                if (locationResult != null && locationResult.getLocations().size() >0){
                                                    //also add marker of current position
                                                    int index = locationResult.getLocations().size() - 1;
                                                    double latitude = locationResult.getLocations().get(index).getLatitude();
                                                    double longitude = locationResult.getLocations().get(index).getLongitude();
                                                    LatLng curr_latLng = new LatLng(latitude, longitude);
                                                    mMap.addMarker(new MarkerOptions().position(curr_latLng).title("Current Location"));
                                                    //plot the path between these 2 point if any
                                                    plotPath(curr_latLng,target_latLng);
                                                }
                                            }
                                        }, Looper.getMainLooper());}
                                else{
                                    requireGPS();
                                }

                            }else{
                                ActivityCompat.requestPermissions(DetailActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        000);
                            }

                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }

            private boolean GPSIsOn(){
                //return GPS On result
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            }


            private void requireGPS(){
                //require GPS
                AlertDialog.Builder builder = new AlertDialog.Builder(DetailActivity.this);
                builder.setTitle("GPS not yet enabled");
                builder.setMessage("Enable GPS for Path");
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

            private void plotPath(LatLng curr_location,LatLng target_location){
                try{
                    //use google api for path searching
                    GeoApiContext context = new GeoApiContext.Builder()
                            .apiKey(String.valueOf(R.string.google_maps_direction_key))
                            .build();
                    LatLng origin = curr_location;
                    LatLng destination = target_location;
                    DirectionsApiRequest directions = DirectionsApi.newRequest(context);
                    directions.origin(new com.google.maps.model.LatLng(origin.latitude, origin.longitude));
                    directions.destination(new com.google.maps.model.LatLng(destination.latitude, destination.longitude));
                    directions.mode(TravelMode.DRIVING);
                    DirectionsResult result = directions.await();
                    List<LatLng> path = new ArrayList<>();
                    List<com.google.maps.model.LatLng> resultPath = result.routes[0].overviewPolyline.decodePath();
                    for (com.google.maps.model.LatLng latLng : resultPath) {
                        path.add(new LatLng(latLng.lat, latLng.lng));
                    }
                    //plot the graph
                    PolylineOptions polylineOptions = new PolylineOptions()
                            .addAll(path)
                            .width(20)
                            .color(Color.RED);
                    mMap.addPolyline(polylineOptions);
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        });
    }
    private void searchViewOnFocusInit(){
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                //on focus handler of search View
                View fragmentView = mapFragment.getView();
                ConstraintLayout.LayoutParams params;
                if (hasFocus) {
                    //hide all other element except search view and google map
                    for (int i = 0; i < layout.getChildCount(); i++) {
                        View child = layout.getChildAt(i);
                        if(child==(View)searchView||child==(View)fragmentView){
                            continue;
                        }
                        child.setVisibility(View.INVISIBLE);
                    }

                    //reposition the search view to the top of page
                    params = (ConstraintLayout.LayoutParams) searchView.getLayoutParams();
                    params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
                    searchView.setLayoutParams(params);
                    //reposition google map
                    params = new ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.MATCH_PARENT,
                            1000
                    );
                    params.topToBottom = R.id.search_view;
                    fragmentView.setLayoutParams(params);

                } else {
                    //else lost focus,reset back other element to visible
                    for (int i = 0; i < layout.getChildCount(); i++) {
                        View child = layout.getChildAt(i);
                        child.setVisibility(View.VISIBLE);
                    }
                    //reposition search View back to original pos
                    params = new ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.MATCH_PARENT,
                            ConstraintLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.topToBottom = R.id.end_time_picker;
                    searchView.setLayoutParams(params);
                    //reposition google map
                    params = new ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.MATCH_PARENT,
                            0
                    );
                    params.topToBottom=R.id.search_view;
                    params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
                    fragmentView.setLayoutParams(params);
                }

            }
        });
    }
}
