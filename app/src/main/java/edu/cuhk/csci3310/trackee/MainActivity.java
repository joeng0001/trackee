package edu.cuhk.csci3310.trackee;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalTime;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import javax.mail.Quota;


public class MainActivity extends AppCompatActivity {

    public static Boolean lock=false;


    private RecyclerView mRecyclerView;
    private JobListAdapter mAdapter;
    private HistoryJobListAdapter mHistoryAdapter;

    //storing list from share prefrence
    private JSONArray job_list;
    private JSONArray job_history_list;
    // for passing to adapter
    private static LinkedList<JSONObject> jobList=new LinkedList<>();
    private LinkedList<JSONObject> jobHistoryList=new LinkedList<>();


   private SharedPreferences resource;

    //implement on Activity Result when return back from detail activity
    //update adapter for job list displaying
    private ActivityResultLauncher<Intent> detailActLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK) {
                        try {
                            Intent data = result.getData();
                            String job_list_in_string = data.getStringExtra("job_list");
                            job_list = new JSONArray(job_list_in_string);
                            jobList.clear();
                            parseJSONArrayToLinkList();
                            mAdapter = new JobListAdapter(MainActivity.this, MainActivity.jobList);
                            mRecyclerView.setAdapter(mAdapter);
                        }catch(Exception e){
                            e.printStackTrace();
                        }

                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //require GPS
        if(!GPSIsOn()){
            requireGPS();
        }
        //require network
        if(!NetworkIsOn()){
            requireNetwork();
        }
        //require access location permission
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    000);
        }
        //obtain the share preference
        resource = this.getSharedPreferences("store", MODE_PRIVATE);
        //extract json array from sharePreference,store to link list
        parseJSONArrayToLinkList();
        //initiate bottom navigate->Home , History
        bottomNavigationInit();

        //Get the RecyclerView.
        mRecyclerView = findViewById(R.id.recyclerview);
        //Create an adapter and supply the data to be displayed,
        mAdapter = new JobListAdapter(this,this.jobList);
        //Connect the adapter with the RecyclerView.
        mRecyclerView.setAdapter(mAdapter);
        //1 column
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        //initiate 2 timer->1 for track location every minutes,1 for reset at 23:59
        timerInit();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.crud_menu, menu);
        return true;
    }





    //top right menu select handler
    //if remove all job,reset the job list in share preference
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.add_job){
            //if "Add job",go to detail activity
            Intent intent=new Intent(this, DetailActivity.class);
            intent.putExtra("title","");
            intent.putExtra("remarks","");
            intent.putExtra("start_time","");
            intent.putExtra("end_time","");
            intent.putExtra("creation_flag",true);
            intent.putExtra("job_index",-1);
            intent.putExtra("location","Kwun Tong");
            detailActLauncher.launch(intent);
        }else if(item.getItemId()==R.id.remove_all_job){
            //remove all job from share preference by setting job list to empty array
            SharedPreferences settings = this.getSharedPreferences("store", MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("job_list","[]");
            editor.commit();
            recreate();
        }



        return super.onOptionsItemSelected(item);
    }

    public void direct_to_email_page(View view){
        //onclick listener of email button,direct to email page
        Intent intent=new Intent(this, EmailActivity.class);
        this.startActivity(intent);
    }

    private void parseJSONArrayToLinkList(){
        try{
            //parse json array to linked list respectively according to the time
            job_list=new JSONArray(resource.getString("job_list","[]"));
            job_history_list=new JSONArray(resource.getString("job_history_list","[]"));

            int curr_hour =Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            int curr_minute=Calendar.getInstance().get(Calendar.MINUTE);

            for( int i=0;i<job_list.length();i++){
                JSONObject obj=(JSONObject) job_list.get(i);
                if(curr_hour>Integer.parseInt(obj.getString("end_time").split(":")[0])||
                        (curr_hour==Integer.parseInt(obj.getString("end_time").split(":")[0])&&
                                curr_minute>Integer.parseInt(obj.getString("end_time").split(":")[1]))){
                    jobHistoryList.addLast(obj);
                }else{
                    jobList.addLast((JSONObject) job_list.get(i));
                }

            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    private void bottomNavigationInit(){
        //handler of bottom navigation->Home,History
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_home:
                        //update adapter to view job list
                        mRecyclerView = findViewById(R.id.recyclerview);
                        mAdapter = new JobListAdapter(MainActivity.this,jobList);
                        mRecyclerView.setAdapter(mAdapter);
                        mRecyclerView.setLayoutManager(new GridLayoutManager(MainActivity.this, 1));
                        return true;
                    case R.id.menu_history:
                        ////update adapter to view job history list
                        mRecyclerView = findViewById(R.id.recyclerview);
                        mHistoryAdapter = new HistoryJobListAdapter(MainActivity.this,jobHistoryList);
                        mRecyclerView.setAdapter(mHistoryAdapter);
                        mRecyclerView.setLayoutManager(new GridLayoutManager(MainActivity.this, 1));
                        return true;
                }
                return false;
            }
        });
    }
    private boolean GPSIsOn(){
        //return result of GPS turning ON
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
    private boolean NetworkIsOn() {
        //return result of Network turning ON
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void requireNetwork() {
        //prompt an alert dialog that require Network
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Network not yet enabled");
        builder.setMessage("Enable network connection for Google Map");
        builder.setPositiveButton("Enable network", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                startActivity(intent);
            }
        });
        builder.setNegativeButton("Reject", null);
        builder.create().show();
    }

    private void requireGPS(){
        //prompt an alert dialog that require GPS
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("GPS not yet enabled");
        builder.setMessage("Enable GPS for Google Map");
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
    private void timerInit(){
        //add 2 timer through application,1 for tracing location,1 for reset
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            private LocationRequest locationRequest;
            private SharedPreferences.Editor editor = resource.edit();
            @Override
            public void run() {
                //execute below function per minutes
                updateJobList2HistoryList();
                recordPathList();
            }

            private void updateJobList2HistoryList(){
                try{
                    //acquire the lock->avoid race condition with job creation
                    while(lock);
                    lock=true;
                    //instantiate 2 JSONArray new list
                    JSONArray new_job_list=new JSONArray();
                    JSONArray new_job_history_list=new JSONArray();
                    //obtain current time
                    int curr_hour =Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                    int curr_minute=Calendar.getInstance().get(Calendar.MINUTE);
                    //empty original linked list, ready for
                    jobList.clear();
                    //iterate job list,push to the new json array or the linked list
                    for( int i=0;i<job_list.length();i++){
                        JSONObject obj=(JSONObject) job_list.get(i);
                        if(curr_hour>Integer.parseInt(obj.getString("end_time").split(":")[0])||
                                (curr_hour==Integer.parseInt(obj.getString("end_time").split(":")[0])&&
                                        curr_minute>Integer.parseInt(obj.getString("end_time").split(":")[1]))){
                            new_job_history_list.put(obj);
                            jobHistoryList.addLast(obj);
                        }else{
                            new_job_list.put(obj);
                            jobList.addLast(obj);
                        }
                    }
                    //refresh original JSONArray to the new one
                    job_list=new_job_list;
                    job_history_list=new_job_history_list;
                    //store back to share preference
                    SharedPreferences.Editor editor = resource.edit();
                    editor.putString("job_list",new_job_list.toString());
                    editor.putString("job_history_list",new_job_history_list.toString());
                    editor.commit();
                    //release the lock
                    lock=false;
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            private void recordPathList(){
                //check permission and GPS on before request for location
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED && GPSIsOn()) {
                        locationRequest = LocationRequest.create();
                        LocationServices.getFusedLocationProviderClient(MainActivity.this)
                                .requestLocationUpdates(locationRequest, new LocationCallback() {
                                    @Override
                                    public void onLocationResult(@NonNull LocationResult locationResult) {
                                        super.onLocationResult(locationResult);
                                        //reauire current location
                                        LocationServices.getFusedLocationProviderClient(MainActivity.this)
                                                .removeLocationUpdates(this);
                                        if (locationResult != null && locationResult.getLocations().size() >0){
                                            try{
                                                int index = locationResult.getLocations().size() - 1;
                                                double latitude = locationResult.getLocations().get(index).getLatitude();
                                                double longitude = locationResult.getLocations().get(index).getLongitude();
                                                LatLng curr_latLng = new LatLng(latitude, longitude);
                                                //store current position (lat lng)with time
                                                JSONArray path_record=new JSONArray(resource.getString("path_record","[]"));
                                                JSONObject obj=new JSONObject();
                                                Calendar currentTime = Calendar.getInstance();
                                                int hour = currentTime.get(Calendar.HOUR_OF_DAY);
                                                int minute = currentTime.get(Calendar.MINUTE);
                                                obj.put("time",(hour)+":"+(minute));
                                                obj.put("lat",curr_latLng.latitude);
                                                obj.put("lng",curr_latLng.longitude);
                                                path_record.put(obj);
                                                editor.putString("path_record",path_record.toString());
                                                editor.commit();
                                            }catch(Exception e){
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }, Looper.getMainLooper());

                }
            }



        },  0, 60 * 1000);
        //create a schedule time 23:59
        Calendar scheduledTime = Calendar.getInstance();
        scheduledTime.set(Calendar.HOUR_OF_DAY,23);
        scheduledTime.set(Calendar.MINUTE,59);

        //another timer
        Timer timer2 = new Timer();
        timer2.schedule(new TimerTask() {
            @Override
            public void run() {
                try{
                    //clear all record in share preference
                    while(!lock);
                    lock=true;
                    SharedPreferences.Editor editor = resource.edit();
                    editor.clear();
                    timer2.cancel();
                    lock=false;
                }catch(Exception e){
                    e.printStackTrace();
                }

            }
        }, scheduledTime.getTime());
    }
}