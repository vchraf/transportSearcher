package com.dev.achraf.demoapp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class OnAppStop extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        try {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("driversAvailable");

            GeoFire geoFire = new GeoFire(ref);
            geoFire.removeLocation(userId);
        }catch (Exception ex){
            Toast.makeText(this, "something wrong", Toast.LENGTH_SHORT).show();
        }

    }
}
