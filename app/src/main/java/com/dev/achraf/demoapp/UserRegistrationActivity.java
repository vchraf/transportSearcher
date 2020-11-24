package com.dev.achraf.demoapp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.Console;

public class UserRegistrationActivity extends AppCompatActivity {

    private EditText mEmail,mPassword,mPasswordV;
    private Button mRegistration,imDriver,imCustomer;
    private TextView errorMsg;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;
    private String userType="Customers";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_registration);

        mAuth = FirebaseAuth.getInstance();

        mEmail=findViewById(R.id.email);
        mPassword=findViewById(R.id.password);
        mPasswordV=findViewById(R.id.passwordValid);

        errorMsg=findViewById(R.id.errorMsg);

        imDriver=findViewById(R.id.driver);
        imCustomer=findViewById(R.id.customer);
        mRegistration=findViewById(R.id.registration);


        errorMsg.setVisibility(View.GONE);

        imCustomer.setBackground(getResources().getDrawable(R.drawable.customer_bu_clicked_img));

        imDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userType="Drivers";
                imDriver.setBackground(getResources().getDrawable(R.drawable.driver_bu_clicked_img));
                imCustomer.setBackground(getResources().getDrawable(R.drawable.customer_bu_img));
            }
        });
        imCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userType="Customers";
                imDriver.setBackground(getResources().getDrawable(R.drawable.driver_bu_img));
                imCustomer.setBackground(getResources().getDrawable(R.drawable.customer_bu_clicked_img));
            }
        });
        /****************************************************************************************************/
        mEmail.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    mEmail.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                else{
                    if (!isEmailValid(mEmail.getText().toString().trim()))
                        mEmail.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.error_icon, 0);
                }
            }
        });
        mPassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    mPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

            }
        });
        mPasswordV.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    mPasswordV.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

            }
        });
        /********************************||verifier la connection||******************************************/
        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();//get User frome Database
                if(user!=null){
                    if (userType.equalsIgnoreCase("Drivers")){
                        Intent intent = new Intent(UserRegistrationActivity.this, DriverSettingsActivity.class);
                        intent.putExtra("SESSION","newUser");
                        startActivity(intent);
                        finish();
                        return;
                    }else{
                        Intent intent = new Intent(UserRegistrationActivity.this, CustomerSettingsActivity.class);
                        intent.putExtra("SESSION","newUser");
                        startActivity(intent);
                        finish();
                        return;
                    }
                }
            }
        };
        /********************************||Registration in FireBase||******************************************/
        mRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                errorMsg.setVisibility(View.GONE);
                mRegistration.setBackground(getResources().getDrawable(R.drawable.singup_bu));
                final String email = mEmail.getText().toString().trim();
                final String password = mPassword.getText().toString().trim();
                final String passwordV = mPasswordV.getText().toString().trim();

                if (isAllUserInfoValid(email, password, passwordV)) {
                    mAuth.createUserWithEmailAndPassword(email, password)//Registration
                            .addOnCompleteListener(UserRegistrationActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (!task.isSuccessful()) {
                                        errorMsg.setVisibility(View.VISIBLE);
                                        mRegistration.setBackground(getResources().getDrawable(R.drawable.singup_bu));
                                    } else {
                                        errorMsg.setVisibility(View.GONE);
                                        String user_id = mAuth.getCurrentUser().getUid();//get user ID
                                        DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference()
                                                .child("Users")
                                                .child(userType)
                                                .child(user_id);
                                        current_user_db.setValue(true);
                                    }
                                }
                            });
                }
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(firebaseAuthListener);//Call firebaseAuthListener after Login/Registration
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(firebaseAuthListener);//  end
    }



    boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }



    boolean isAllUserInfoValid(String email,String password,String passwordV){
        boolean isValid=true;
        if (passwordV.isEmpty() && password.isEmpty()) {
            mPasswordV.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.error_icon, 0);
            mPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.error_icon, 0);
            isValid=false;
        }else{
            mPasswordV.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            mPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0,0, 0);
        }


        if (!isEmailValid(email)) {
            mEmail.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.error_icon, 0);
            isValid=false;
        }else
            mEmail.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);



        if (password.length()<6) {
            mPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.error_icon, 0);
            isValid=false;
        }else
            mPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0,0, 0);

        if (!password.isEmpty()) {
            if (!passwordV.equals(password)) {
                mPasswordV.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.error_icon, 0);
                isValid = false;
            } else
                mPasswordV.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }

        mRegistration.setBackground(getResources().getDrawable(R.drawable.singup_bu));
        return isValid;
    }
}
