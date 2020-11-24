package com.dev.achraf.demoapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
Button mdrever,mcustomer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mdrever=findViewById(R.id.driver);
        mcustomer=findViewById(R.id.customer);

            startService(new Intent(MainActivity.this,OnAppStop.class));
        mdrever.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               mdrever.setBackground(getResources().getDrawable(R.drawable.driver_bu_clicked_img));
                Intent intent=new Intent(MainActivity.this,DriverMapActivity.class);
                startActivity(intent);
                finish();
            }
        });
        mcustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mcustomer.setBackground(getResources().getDrawable(R.drawable.customer_bu_clicked_img));
                Intent intent=new Intent(MainActivity.this,CustomerMapActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
    final int REQUEST_CODE=1;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_CODE:{
                if (grantResults.length > 0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                }else {
                    Toast.makeText(this, "Please Provide the Permission", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }
}
