package com.dev.achraf.demoapp;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener,RoutingListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private Button mLogout,mSettings,mRideStatus,mHistory,mCall;

    private Switch mWorkingSwitch;

    private int status = 0;

    private String customerId = "";
    private String destination,phoneNum;
    private LatLng destinationLatLng,pickupLatLng;

    private boolean isLoggingOut=false;
    private LinearLayout mCustomerInfo;

    private ImageView mCustomerProfileImage;

    private TextView mCustomerName, mCustomerPhone, mCustomerDestination;

    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.blue};



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);


        polylines=new ArrayList<>();
        mRideStatus=findViewById(R.id.rideStatus);
        mSettings=findViewById(R.id.settings);
        mCustomerDestination=findViewById(R.id.customerDestination);
        mCustomerInfo=findViewById(R.id.customerInfo);
        mCustomerProfileImage=findViewById(R.id.customerProfileImage);
        mCustomerName=findViewById(R.id.customerName);
        mCustomerPhone=findViewById(R.id.customerPhone);
        mLogout = findViewById(R.id.logout);
        mHistory = findViewById(R.id.history);
        mWorkingSwitch=findViewById(R.id.workingSwitch);
        mCall=findViewById(R.id.buCall);

/********************************||permission to use GPS and getting location||******************************************/
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DriverMapActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.CALL_PHONE},REQUEST);
        }else{
            mapFragment.getMapAsync(this);
        }






/************************************||Logout||*********************************************/

        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoggingOut=true;
                disconnectDriver();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(DriverMapActivity.this, UserLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });



        getAssignedCustomer();






/*****************************************| Settings |**************************************************/
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DriverMapActivity.this, DriverSettingsActivity.class);
                intent.putExtra("SESSION","oldUser");
                startActivity(intent);
                return;
            }
        });






/*****************************************| History |**************************************************/
        mHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(DriverMapActivity.this,HistoryActivity.class);
                intent.putExtra("customerOrDriver", "Drivers");
                startActivity(intent);
                //finish();
            }
        });




/************************************************************************************************************
*verify status
*===>1: clear the map and switch to destination
* change the title
*===>2:save the work in History
*
*
/**********************************************************************************************************/
        mRideStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (status){
                    case 1:
                        status=2;
                        erasePolylines();//clean the first road to the customer
                        if(destinationLatLng.latitude!=0.0 && destinationLatLng.longitude!=0.0){
                            getRouteToMarker(destinationLatLng);//dr
                        }
                        mRideStatus.setText(getResources().getString(R.string.driveCompleted));
                        break;
                    case 2:
                        recordRide();
                        endRide();
                        break;
                }
            }
        });



/*****************************************| availability |*******************************************************/
        mWorkingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    connectDriver();
                }else{
                    disconnectDriver();
                }
            }
        });
        mCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (phoneNum==null) return;
                if (!phoneNum.trim().equals("")){
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:"+phoneNum.trim()));

                if (ActivityCompat.checkSelfPermission(DriverMapActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(DriverMapActivity.this,new String[]{Manifest.permission.CALL_PHONE},REQUEST); }
                startActivity(callIntent);
                return;
                }else
                    Toast.makeText(DriverMapActivity.this, "Any phone number exists", Toast.LENGTH_SHORT).show();
            }
        });
    }










/*******************************************| get Customer |*************************************************
*1-get UID of Driver
*2-after Customer requested he  create a child customerRequest and he add his Location an UID
*3-get Reference of customer request
*
*
*
*
/************************************************************************************************************/
    private void getAssignedCustomer(){

        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();//driver UID

        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child("Drivers")
                .child(driverId)
                .child("customerRequest")
                .child("customerRideId");

        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    status=1;
                    notifDrive(DriverMapActivity.this);
                    customerId = dataSnapshot.getValue().toString();
                    getAssignedCustomerPickupLocation();
                    getAssignedCustomerDestination();
                    getAssignedCustomerInfo();
                }else{
                    endRide();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }




/******************************************| Customer Location Pickup|*************************************************
*1-get ref of customer location (assignedCustomerPickupLocationRef)
*2-get Location Customer from FireBase (locationLat ,locationLng)
*  --> latitude longitude
*3-get Customer location
*4-Draw the road
*********************************************************************************************************************/
    Marker pickupMarker;

    private DatabaseReference assignedCustomerPickupLocationRef;

    private ValueEventListener assignedCustomerPickupLocationRefListener;

    private void getAssignedCustomerPickupLocation(){

        assignedCustomerPickupLocationRef = FirebaseDatabase.getInstance()
                .getReference().child("customerRequest")
                .child(customerId)
                .child("l");

        assignedCustomerPickupLocationRefListener=assignedCustomerPickupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists() && !customerId.equalsIgnoreCase("")){

                    List<Object> map = (List<Object>) dataSnapshot.getValue();//get Location data from  FireBase

                    double locationLat = 0;
                    double locationLng = 0;

                    if(map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());//get latitude
                    }


                    if(map.get(1) != null){
                        locationLng = Double.parseDouble(map.get(1).toString());//get longitude
                    }


                     pickupLatLng = new LatLng(locationLat,locationLng);//customer Location


                     pickupMarker = mMap.addMarker(new MarkerOptions()
                            .position(pickupLatLng)
                            .title(getResources().getString(R.string.pickup_HereMark))
                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_here)));

                    getRouteToMarker(pickupLatLng);//Draw the road

                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }













/*************************************| Customer Destination Location |************************************************
*1-get UID of Driver(driverId)
*2-get Ref of Customer Request (assignedCustomerRef)
*3-get Name of Customer Destination and display it
*4-get Destination Location
*********************************************************************************************************************/
Marker destinationMarker;

    private void getAssignedCustomerDestination(){

        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child("Drivers")
                .child(driverId)
                .child("customerRequest");

        assignedCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists()){

                    Map<String ,Object> map= (Map<String, Object>) dataSnapshot.getValue();//get Customer Request data

                 if (map.get("destination")!=null){

                     destination = map.get("destination").toString();
                     mCustomerDestination.setText(getResources().getString(R.string.destination)+ destination);// display Name of destination

                 }else{

                     mCustomerDestination.setText(getResources().getString(R.string.destinationEmpty));
                 }



                    Double destinationLat = 0.0;
                    Double destinationLng= 0.0;

                    if(map.get("destinationLat") != null){
                        destinationLat = Double.valueOf(map.get("destinationLat").toString());
                    }

                    if(map.get("destinationLng") != null){
                        destinationLng = Double.valueOf(map.get("destinationLng").toString());
                    }

                    destinationLatLng = new LatLng(destinationLat, destinationLng);
                    destinationMarker=mMap.addMarker(new MarkerOptions()
                            .position(destinationLatLng)
                            .title(getResources().getString(R.string.destinationMark))
                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_destination)));


                }
            }
            @Override public void onCancelled(DatabaseError databaseError) { }
        });
    }







/*************************************************| Customer Info |**************************************************/
    private void getAssignedCustomerInfo(){
        mCustomerInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase=FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child("Customers")
                .child(customerId);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String ,Object> map= (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("lastName")!=null){
                        mCustomerName.setText(map.get("lastName").toString());
                    }
                    if (map.get("phone")!=null){
                        mCustomerPhone.setText(map.get("phone").toString());
                        phoneNum=map.get("phone").toString();
                    }
                    if (map.get("profileImageUrl")!=null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mCustomerProfileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

    }






    /**********************************************************************************************************/
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


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DriverMapActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.CALL_PHONE},REQUEST);
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





/***************************************| Driver status |***************************************************
*1-make two ref to Driver (driversAvailable-driversWorking)
*2-create tow GeoFire for Those ref
*3-if customer requested a driver the app remove Driver UID from driversAvailable
*4-add Driver UID in driversWorking(use GeoFire to send Driver location)
********************************************************************************************************/
    @Override
    public void onLocationChanged(Location location) {
        if(getApplicationContext()!=null){

            mLastLocation = location;
            LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(14.46888f));

            //Driver status
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable");
            DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("driversWorking");
            GeoFire geoFireAvailable = new GeoFire(refAvailable);
            GeoFire geoFireWorking = new GeoFire(refWorking);

            switch (customerId){
                case "":
                    geoFireWorking.removeLocation(userId);
                    geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;

                default:
                    geoFireAvailable.removeLocation(userId);
                    geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
            }
        }
    }







/************************************************| ENDRIDE |************************************************
 *1-Remove all GeoQuery Listener
 *3-remove Customer Request from driver
 *4-clean driverFoundID
 *5-get UID->create Ref->create GeoFire->Remove Location of this UID =>remove request
 *6-remove all marker
 *7-clean all Customer Info
 /*********************************************************************************************************/
    private void endRide(){
        mRideStatus.setText(getResources().getString(R.string.pickedCustomer));
        erasePolylines();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child("Drivers")
                .child(userId)
                .child("customerRequest");
        driverRef.removeValue();


            /*****************************
             * make ref of customerRequest
             * make GeoFire by The Reference
             * finally i remove the customerRequest of the customerId
             *****************************/
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
            GeoFire geoFire = new GeoFire(ref);
            geoFire.removeLocation(customerId);
            customerId="";



        if(pickupMarker != null){
            pickupMarker.remove();
        }

        if(destinationMarker != null){
            destinationMarker.remove();
        }

        if (assignedCustomerPickupLocationRefListener != null){
            assignedCustomerPickupLocationRef.removeEventListener(assignedCustomerPickupLocationRefListener);
        }
        mCustomerInfo.setVisibility(View.GONE);
        mCustomerName.setText("");
        mCustomerPhone.setText("");
        mCustomerDestination.setText(getResources().getString(R.string.destinationEmpty));
        mCustomerProfileImage.setImageResource(R.mipmap.ic_default_user);
    }

 /*********************************************************************************************************************/
    private void recordRide(){
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("history");

        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId).child("history");

        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("history");

        String requestId = historyRef.push().getKey();

        driverRef.child(requestId).setValue(true);
        customerRef.child(requestId).setValue(true);

        HashMap map = new HashMap();
        map.put("driver", userId);
        map.put("customer", customerId);
        map.put("rating", 0);
        map.put("timestamp", getCurrentTimestamp());
        map.put("destination", destination);
        map.put("location/from/lat", pickupLatLng.latitude);
        map.put("location/from/lng", pickupLatLng.longitude);
        map.put("location/to/lat", destinationLatLng.latitude);
        map.put("location/to/lng", destinationLatLng.longitude);

        //map.put("distance", rideDistance);

        historyRef.child(requestId).updateChildren(map);
    }







 /************************************************| Date Hour|************************************************/

    private long getCurrentTimestamp() {
        long timestamp=System.currentTimeMillis()/1000;
        return timestamp;
    }


    /**********************************************************************************************************/
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
}
    @Override public void onConnectionSuspended(int i) { }
    @Override public void onConnectionFailed(@NonNull ConnectionResult connectionResult) { }






/*********************************|availability of driver|****************************************************
 * chick the permission to use User location
 * make update to user location
 *************************************************************************************************************/
    private void connectDriver(){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DriverMapActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.CALL_PHONE},REQUEST);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }



/**********************************************|unavailability of driver|*******************************
 * stop update of user location
 *get user ID
 *remove driver available from the fireBase
 ******************************************************************************************************/

    private void disconnectDriver(){
        //remove Driver Available from FireBase
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("driversAvailable");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);
    }








/*********************************************| Draw the road to where ??|************************************************/
    private void getRouteToMarker(LatLng pickupLatLng) {
        Routing routing = new Routing.Builder()
                .key(getResources().getString(R.string.roadkey))
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()), pickupLatLng)
                .build();
        routing.execute();
    }


    @Override
    public void onRoutingFailure(RouteException e) {

        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.d("er",e.getMessage());
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }


    @Override public void onRoutingStart() {}


    @Override
    public void onRoutingSuccess(ArrayList<Route>  route, int shortestRouteIndex) {

        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(6 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }
    }
    @Override public void onRoutingCancelled() { }







/***********************| Clean The Road |**********************/
    private void erasePolylines(){
        for(Polyline line : polylines){
            line.remove();
        }
        polylines.clear();
    }



public void notifDrive(Context context){
    NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
            .setSmallIcon(R.mipmap.ic_car)
            .setContentTitle("You have a request")
            .setContentText("You have a customer a request");


    // Creates the intent needed to show the notification
//                Intent notificationIntent = new Intent(MainActivity.this,MainActivity.class);
//                PendingIntent contentIntent = PendingIntent.getActivity(MainActivity.this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//                builder.setContentIntent(contentIntent);

    // Add as notification
    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
    {
        NotificationChannel nChannel = new NotificationChannel("blabla", "MY_NOTIFICATIONS", NotificationManager.IMPORTANCE_HIGH);
        nChannel.enableLights(true);
        assert manager != null;
        builder.setChannelId("blabla");
        manager.createNotificationChannel(nChannel);
    }
    manager.notify(0, builder.build());
}



/********************************||Verifier les permission if user ||******************************************/
final int REQUEST=123;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST:{
                if (grantResults.length > 0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                }else
                    Toast.makeText(this, "Please Provide the Permission", Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }


}