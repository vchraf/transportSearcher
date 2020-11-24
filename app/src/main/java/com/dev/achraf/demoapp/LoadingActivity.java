package com.dev.achraf.demoapp;

        import android.Manifest;
        import android.app.Activity;
        import android.content.Context;
        import android.content.Intent;
        import android.content.pm.PackageManager;
        import android.net.ConnectivityManager;
        import android.net.NetworkInfo;
        import android.os.Build;
        import android.os.Handler;
        import android.support.annotation.NonNull;
        import android.support.v4.app.ActivityCompat;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.view.View;
        import android.widget.Button;
        import android.widget.Toast;

        import com.google.firebase.auth.FirebaseAuth;
        import com.google.firebase.auth.FirebaseUser;
        import com.google.firebase.database.DataSnapshot;
        import com.google.firebase.database.DatabaseError;
        import com.google.firebase.database.DatabaseReference;
        import com.google.firebase.database.FirebaseDatabase;
        import com.google.firebase.database.ValueEventListener;
        import com.victor.loading.rotate.RotateLoading;

public class LoadingActivity extends AppCompatActivity {
    private RotateLoading loading;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;
    private FirebaseAuth mAuth;
    private Class activityClass;
    private Thread loader;
    private Handler loaderUi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
         loading = findViewById(R.id.rotateloading);
        mAuth = FirebaseAuth.getInstance();
        loading.start();

         loader = new Thread() {
            @Override
            public void run() {
                try {
                    sleep( 1700 );
                    if (isInternetAvailable()){
                        Intent intent=new Intent( LoadingActivity.this,activityClass );
                        startActivity( intent );
                        finish();
                    }else{
                        runOnUiThread(new Runnable(){
                            public void run() {
                            loading.stop();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        };



        /********************************||verifier la connection||******************************************/
            firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    loading.start();
                    if(!loading.isStart()) loading.start();
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();//get User frome Database
                    if (isInternetAvailable()) {
                        if (user != null) {
                            final String UID = user.getUid();
                            DatabaseReference mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(UID);
                            mUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        activityClass = MainActivity.class;
                                        loader.start();
                                    } else {
                                        activityClass = CustomerMapActivity.class;
                                        loader.start();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                }
                            });
                        } else {
                            activityClass = UserLoginActivity.class;
                            loader.start();
                        }
                    }else{
                        activityClass = null;
                        loader.start();
                        }
                }
            };
    }

    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    @Override
    protected void onStart() {
        super.onStart();
if (firebaseAuthListener!=null)
    mAuth.addAuthStateListener(firebaseAuthListener);//Call firebaseAuthListener after Login/Registration
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (firebaseAuthListener!=null)
        mAuth.removeAuthStateListener(firebaseAuthListener);//  end
    }


}
