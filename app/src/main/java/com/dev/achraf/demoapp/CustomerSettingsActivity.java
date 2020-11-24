package com.dev.achraf.demoapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class CustomerSettingsActivity extends AppCompatActivity {
private Button mConfirm;
private EditText mFirstNameField,mLastNameField,mCINFieald,mPhoneField;

private FirebaseAuth mAuth;

private ImageView mProfileImage;

private String userID;
private String mFirstName,mLastName,mCIN,mPhone,mProfileImageUrl;
private Uri resultUri;

private DatabaseReference mCustomerDatabase;

private String session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_settings);
        mConfirm=findViewById(R.id.confirm);

        mProfileImage=findViewById(R.id.profileImage);

        mFirstNameField=findViewById(R.id.firsName);
        mLastNameField=findViewById(R.id.lastName);
        mCINFieald=findViewById(R.id.CIN);
        mPhoneField=findViewById(R.id.phone);

        session= getIntent().getStringExtra("SESSION");


        mAuth = FirebaseAuth.getInstance();
        userID=mAuth.getCurrentUser().getUid();

        mCustomerDatabase=FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(userID);

        getUserInfo();

       mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConfirm.setBackground(getResources().getDrawable(R.drawable.confirm_bu_clicked));

                saveUserInformation();
            }
        });


        mProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,1);
            }
        });
    }

    private void getUserInfo(){
        mCustomerDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String ,Object> map= (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("firstName")!=null){
                        mFirstName=map.get("firstName").toString();
                        mFirstNameField.setText(mFirstName);
                    }
                    if (map.get("lastName")!=null){
                        mLastName=map.get("lastName").toString();
                        mLastNameField.setText(mLastName);
                    }
                    if (map.get("CIN")!=null){
                        mCIN=map.get("CIN").toString();
                        mCINFieald.setText(mCIN);
                    }
                    if (map.get("phone")!=null){
                        mPhone=map.get("phone").toString();
                        mPhoneField.setText(mPhone);
                    }
                    if (map.get("profileImageUrl")!=null){
                        mProfileImageUrl=map.get("profileImageUrl").toString();
                        Glide.with(getApplication()).load(mProfileImageUrl).into(mProfileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

    }


    private void saveUserInformation() {
        mFirstName=mFirstNameField.getText().toString();
        mLastName=mLastNameField.getText().toString();
        mCIN=mCINFieald.getText().toString();
        mPhone=mPhoneField.getText().toString();

        Map userInfo=new HashMap();

        userInfo.put("firstName",mFirstName);
        userInfo.put("lastName",mLastName);
        userInfo.put("CIN",mCIN);
        userInfo.put("phone",mPhone);

        mCustomerDatabase.updateChildren(userInfo);
        if (resultUri!=null){
            final StorageReference filePath =FirebaseStorage.getInstance().getReference().child("profile_images").child(userID);
            Bitmap bitmap=null;

            try {

                bitmap=MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(),resultUri);

            } catch (IOException e) { e.printStackTrace(); }

            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG,20,baos);

            byte[] data = baos.toByteArray();
            UploadTask uploadTask=filePath.putBytes(data);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    if (session.equalsIgnoreCase("newUser")){
                        Intent intent = new Intent(CustomerSettingsActivity.this, CustomerMapActivity.class);
                        startActivity(intent);
                        finish();
                        return;
                    }else{
                        finish();
                        return;
                    }
                }
            });
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Map newImage = new HashMap();
                            newImage.put("profileImageUrl", uri.toString());
                            mCustomerDatabase.updateChildren(newImage);
                            if (session.equalsIgnoreCase("newUser")){
                                Intent intent = new Intent(CustomerSettingsActivity.this, CustomerMapActivity.class);
                                startActivity(intent);
                                finish();
                                return;
                            }else{
                                finish();
                                return;
                            }
                        }
                    });
                }
            });

        }else {
            if (session.equalsIgnoreCase("newUser")){
                Intent intent = new Intent(CustomerSettingsActivity.this, CustomerMapActivity.class);
                startActivity(intent);
                finish();
                return;
            }else{
                finish();
                return;
            }
        }



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK){
            final Uri imageUri = data.getData();
            resultUri = imageUri;
            mProfileImage.setImageURI(resultUri);
        }
    }
}
