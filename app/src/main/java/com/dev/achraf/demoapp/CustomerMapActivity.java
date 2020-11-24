package com.dev.achraf.demoapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.nfc.Tag;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;


import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
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
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private Button mLogout, mRequest,mSettings,mHistory;
    private LatLng pickupLocation;
    private Boolean requestBol=false;
    private Marker pickupMarker,destinationMarker;
    private String destination,requestService;
    private RadioGroup mRadioGroup;

    private RatingBar mRatingBar;

    private LatLng destinationLatLng;


    private LinearLayout mDriverInfo;

    private ImageView mDriverProfileImage;

    private TextView mDriverName, mDriverPhone, mDriverCar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mLogout  =findViewById(R.id.logout);
        mRequest =findViewById(R.id.request);
        mSettings=findViewById(R.id.settings);
        mHistory=findViewById(R.id.history);

        mRadioGroup=findViewById(R.id.radioGroup);
        mRadioGroup.check(R.id.carX);

        mRatingBar =  findViewById(R.id.ratingBar);


        mDriverInfo=findViewById(R.id.driverInfo);

        mDriverProfileImage=findViewById(R.id.driverProfileImage);

        mDriverName=findViewById(R.id.driverName);

        mDriverPhone=findViewById(R.id.driverPhone);

        mDriverCar=findViewById(R.id.driverCar);

        destinationLatLng=new LatLng(0.0,0.0);


/********************************||permission to use GPS and getting location||******************************************/

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CustomerMapActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.CALL_PHONE},LOCATION_REQUEST_CODE);
        }else {
            mapFragment.getMapAsync(this);//Map loading...
        }






/************************************||Logout||*********************************************/
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();//logout
                Intent intent = new Intent(CustomerMapActivity.this, UserLoginActivity.class);//back in the first page
                startActivity(intent);
                finish();
                return;
            }
        });






/********************************||call Driver||******************************************
*1-get Request service type
*2-get UID->create Ref->create GeoFire->set Location of this UID in fireBase=>create request request
*3-create location marker
*4-search the near Driver
/****************************************************************************************/
        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestBol){// cancelling The request

                    endRide();

                }else{

                    int selectedID=mRadioGroup.getCheckedRadioButtonId();
                    final RadioButton radioButton=findViewById(selectedID);
                    if (radioButton.getText()==null){
                        return;
                    }
                    requestService=radioButton.getText().toString();


                    requestBol=true;
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();//get user ID

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");//create request

                    GeoFire geoFire = new GeoFire(ref);//(use API GeoFire) initialize by DatabaseReference
                    //setting Location in fireBase
                    geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));//send Location to FireBase

                    pickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());//create location

                   pickupMarker= mMap.addMarker(new MarkerOptions()
                           .position(pickupLocation).title(getResources().getString(R.string.pickup_HereMark))
                           .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_here)));//_?_Map

                    destinationMarker=mMap.addMarker(new MarkerOptions()
                            .position(destinationLatLng)
                            .title(getResources().getString(R.string.destinationMark))
                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_destination)));


                   /* if(destinationMarker != null){
                        //destinationMarker.remove();
                    }else
                        destinationMarker=mMap.addMarker(new MarkerOptions()
                                .position(destinationLatLng)
                                .title("destination")
                                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_destination)));*/

                    mRequest.setText(getResources().getString(R.string.gettingYourDriver));

                    getClosestDriver();//this method search the near Driver
                }
            }
        });




/*****************************************| Settings |**************************************************/
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(CustomerMapActivity.this,CustomerSettingsActivity.class);
                intent.putExtra("SESSION","oldUser");
                startActivity(intent);
                //finish();
            }
        });






/*****************************************| History |**************************************************/
        mHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(CustomerMapActivity.this,HistoryActivity.class);
                intent.putExtra("customerOrDriver", "Customers");
                startActivity(intent);
                //finish();
            }
        });



/****************************************| Autocomplete |****************************************
*1-search a destination
*2-get name of location
*3-get location of destination
*4-create marker
/***********************************************************************************************/
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment) getFragmentManager()
                .findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                destination=place.getName().toString();
                destinationLatLng=place.getLatLng();
            }
            @Override
            public void onError(Status status) { }
        });
    }





/********************************||search the near Driver||******************************************
* 1-go to Drivers available
* 2-get Customer Location
* 3-search a driver every 1Km
* 4-if the app find a driver
* 5-Compare service of Request with the service of driver
* 6-get UID of driver
* 7-Create new Child customerRequest and add details of Request in driver
* 8-get Driver location and make driver marker
* 9-get User Info
* 10-End Ride
**************************************************************************************************/

    private int radius = 1;//km
    private Boolean driverFound = false;
    private String driverFoundID;
    GeoQuery geoQuery;

    private void getClosestDriver(){
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");//ref of Driver

        GeoFire geoFire = new GeoFire(driverLocation);
       //search a driver by radius
         geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);

        geoQuery.removeAllListeners();//remove all event listeners for create a new geoQuery

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(final String key, GeoLocation location) {

                if (!driverFound && requestBol){    //find the near Driver

                    DatabaseReference mCustomerDatabase=FirebaseDatabase.getInstance().getReference()
                            .child("Users")
                            .child("Drivers")
                            .child(key);

                    mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){

                                Map<String ,Object> driverMap= (Map<String, Object>) dataSnapshot.getValue();

                                if (driverFound){
                                    return;
                                }

                                if (driverMap.get("service").equals(requestService)&&!key.equalsIgnoreCase(FirebaseAuth.getInstance().getCurrentUser().getUid())){

                                    driverFound = true;
                                    driverFoundID = dataSnapshot.getKey();//get UID

                                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference()
                                            .child("Users")
                                            .child("Drivers")
                                            .child(driverFoundID)
                                            .child("customerRequest");

                                    String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();//get UID of Customer(user)

                                    HashMap map = new HashMap();
                                    map.put("customerRideId", customerId);
                                    map.put("destination", destination);
                                    map.put("destinationLat", destinationLatLng.latitude);
                                    map.put("destinationLng", destinationLatLng.longitude);
                                    driverRef.updateChildren(map);//create a new child of customer in refDriver

                                    getDriverLocation(); //gat location of driver and create a marker in the map
                                    getDriverInfo();
                                    getHasRideEnded();
                                    mRequest.setText(getResources().getString(R.string.LookingForDriverLocation));
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override public void onKeyExited(String key) {}
            @Override public void onKeyMoved(String key, GeoLocation location) {}
            @Override public void onGeoQueryReady() {
                if (!driverFound)
                {
                    radius++;

                    getClosestDriver();
                }
            }
            @Override public void onGeoQueryError(DatabaseError error) { }
        });
    }
/***************************************************************************************************************
 *driverFound = true;
 * driverFoundID = key;//get UID
 *
 * DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference()
 *         .child("Users")
 *         .child("Drivers")
 *         .child(driverFoundID)
 *         .child("customerRequest");
 * String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();//get UID of Customer(user)
 * HashMap map = new HashMap();
 * map.put("customerRideId", customerId);
 * map.put("destination", destination);
 *
 * driverRef.updateChildren(map);//create a new child of customer in refDriver
 * getDriverLocation();
 * getDriverInfo();
 * mRequest.setText("Looking for Driver Location....");
 **************************************************************************************************************/






/*****************************************| Driver Location |********************************************
*1-get Reference location of driver from FireBase  (driverLocationRef)
*2-get Location Driver from FireBase (locationLat ,locationLng)
*  --> latitude longitude
*3-get Customer location/Driver Location
*4-calculet distance between Customer and drover
*5-make a marker
/**********************************************************************************************************/
    private Marker mDriverMarker;

    private DatabaseReference driverLocationRef;

    private ValueEventListener driverLocationRefEventListener;


    private void getDriverLocation(){

        driverLocationRef = FirebaseDatabase.getInstance().getReference()
                .child("driversWorking")
                .child(driverFoundID)
                .child("l");

        driverLocationRefEventListener =driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists()){

                    List<Object> map = (List<Object>) dataSnapshot.getValue();

                    double locationLat = 0;
                    double locationLng = 0;

                    mRequest.setText(getResources().getString(R.string.DriverFound));

                    if(map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());//get latitude
                    }

                    if(map.get(1) != null){
                        locationLng = Double.parseDouble(map.get(1).toString());//get longitude
                    }

                    LatLng driverLatLng = new LatLng(locationLat,locationLng);//Location of driver

                    if(mDriverMarker != null){

                        mDriverMarker.remove();

                    }


                    //Customer location
                    Location loc1 = new Location("");

                    loc1.setLatitude(pickupLocation.latitude);

                    loc1.setLongitude(pickupLocation.longitude);



                    //Driver Location
                    Location loc2 = new Location("");

                    loc2.setLatitude(driverLatLng.latitude);

                    loc2.setLongitude(driverLatLng.longitude);


                    float distance = loc1.distanceTo(loc2);//distance by m

                    if (distance<100){
                        mRequest.setText(getResources().getString(R.string.YourDriverIsHere));
                    }else
                        mRequest.setText(getResources().getString(R.string.DriverFoundDes) + String.valueOf(distance));

                    mDriverMarker = mMap.addMarker(new MarkerOptions()//make marker
                            .position(driverLatLng)
                            .title(getResources().getString(R.string.yourDriverMark))
                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car)));
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

    }
/***********************************************************************************************************/







/**************************************************|Driver Info|***********************************************/
    private void getDriverInfo(){
        mDriverInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase=FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child("Drivers")
                .child(driverFoundID);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String ,Object> map= (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("lastName")!=null){
                        mDriverName.setText(map.get("lastName").toString());
                    }
                    if (map.get("numPlates")!=null){
                        mDriverPhone.setText(map.get("numPlates").toString());
                    }
                    if (map.get("carName")!=null){
                        mDriverCar.setText(map.get("carName").toString());
                    }
                    if (map.get("profileImageUrl")!=null){
                        //poster Driver Image
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mDriverProfileImage);
                    }


                    int ratingSum = 0;
                    float ratingsTotal = 0;
                    float ratingsAvg = 0;
                    for (DataSnapshot child : dataSnapshot.child("rating").getChildren()){
                        ratingSum=ratingSum+Integer.valueOf(child.getValue().toString());
                        ratingsTotal++;
                    }
                    if(ratingsTotal!= 0){
                        ratingsAvg = ratingSum/ratingsTotal;
                        mRatingBar.setRating(ratingsAvg);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

    }
/***********************************************************************************************************/




/***********************************************************************************************************/
    private DatabaseReference driveHasEndedRef;
    private ValueEventListener driveHasEndedRefListener;


    private void getHasRideEnded(){
         driveHasEndedRef = FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child("Drivers")
                .child(driverFoundID)
                .child("customerRequest")
                .child("customerRideId");
        driveHasEndedRefListener=driveHasEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                }else{
                    endRide();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }
/***********************************************************************************************************/






/************************************************| ENDRIDE |************************************************
*1-Remove all GeoQuery Listener
*2-Remove all ValueEventListener
*3-remove Customer Request from driver
*4-clean driverFoundID
*5-get UID->create Ref->create GeoFire->Remove Location of this UID =>remove request
*6-remove all marker
*7-clean all Driver Info
/*********************************************************************************************************/
    private void endRide(){

        requestBol=false;

        geoQuery.removeAllListeners();
        if (driverLocationRef!=null)
        driverLocationRef.removeEventListener(driverLocationRefEventListener);

        if (driveHasEndedRef!=null)
        driveHasEndedRef.removeEventListener(driveHasEndedRefListener);

        if (driverFoundID!=null){
            DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference()
                    .child("Users")
                    .child("Drivers")
                    .child(driverFoundID)
                    .child("customerRequest");//ref of Driver
            driverRef.removeValue();
            driverFoundID=null;
        }

        driverFound=false;
        radius=1;
        
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();//get user ID

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");//create request
        
        GeoFire geoFire = new GeoFire(ref);//(use API GeoFire) initialize by DatabaseReference
        //setting Location in fireBase
        geoFire.removeLocation(userId);//remove Location from  FireBase
        if (pickupMarker!=null){
            pickupMarker.remove();
            //Toast.makeText(CustomerMapActivity.this, "runnnn", Toast.LENGTH_SHORT).show();
        }
        if(mDriverMarker != null){
            mDriverMarker.remove();
        }

        if(destinationMarker != null){
            destinationMarker.remove();
        }

        if (destinationMarker!=null){
            destinationMarker.remove();
        }

        mRequest.setText(getResources().getString(R.string.call_Driver));
        mDriverInfo.setVisibility(View.GONE);
        mDriverPhone.setText("");
        mDriverName.setText("");
        mDriverCar.setText("");
        mDriverProfileImage.setImageResource(R.mipmap.ic_default_user);
        }
/***********************************************************************************************************/





    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


//        try {
//            // Customise the styling of the base map using a JSON object defined
//            // in a raw resource file.
//            boolean success = googleMap.setMapStyle(
//                    MapStyleOptions.loadRawResourceStyle(
//                            this, R.raw.map_style));
//
//            if (!success) {
//                Log.e("error", "Style parsing failed.");
//            }
//        } catch (Resources.NotFoundException e) {
//            Log.e("error", "Can't find style. Error: ", e);
//        }


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CustomerMapActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.CALL_PHONE},LOCATION_REQUEST_CODE);
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }



    protected synchronized void buildGoogleApiClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }


    @Override
    public void onLocationChanged(Location location) {
        if(getApplicationContext()!=null){
            mLastLocation = location;
            LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(14.46888f));

        }
    }



    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CustomerMapActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.CALL_PHONE},LOCATION_REQUEST_CODE);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }
    @Override
    public void onConnectionSuspended(int i) { }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) { }
    @Override
    protected void onStop() { super.onStop(); }






    /********************************||Verifier les permission if user ||******************************************/

    final int LOCATION_REQUEST_CODE=1;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case LOCATION_REQUEST_CODE:{
                if (grantResults.length > 0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                }else {
                    Toast.makeText(this, "Please Provide the Permission", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

}
