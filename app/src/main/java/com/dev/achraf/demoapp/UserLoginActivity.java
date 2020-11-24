package com.dev.achraf.demoapp;

import android.content.Intent;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserLoginActivity extends AppCompatActivity {

    private EditText mEmail,mPassword;
    private Button mLogin,mRegistration;
    private TextView forgetPassword,errorMsg;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_login);

        mAuth = FirebaseAuth.getInstance();

        mEmail=findViewById(R.id.email);
        mPassword=findViewById(R.id.password);

        errorMsg=findViewById(R.id.errorMsg);
        forgetPassword=findViewById(R.id.forgetPassword);

        mRegistration=findViewById(R.id.registration);
        mLogin=findViewById(R.id.login);


        errorMsg.setVisibility(View.GONE);


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


        startService(new Intent(UserLoginActivity.this,OnAppStop.class));
        /********************************||verifier la connection||******************************************/
        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();//get User frome Database
                if(user!=null){
                    final String UID =user.getUid();
                    DatabaseReference mUserDatabase=FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(UID);
                    mUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

//                            for (DataSnapshot data:dataSnapshot.getChildren()){
                                if (dataSnapshot.exists()){
                                    Intent intent = new Intent(UserLoginActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                    return;
                                }else {
                                    Intent intent = new Intent(UserLoginActivity.this, CustomerMapActivity.class);
                                    startActivity(intent);
                                    finish();
                                    return;
                                }
                            }
//                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) { }
                    });
                    //////////////////
                }
            }
        };
        /********************************||Login in FireBase||******************************************/
        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLogin.setBackground(getResources().getDrawable(R.drawable.login_bu_clicked));

                final String email = mEmail.getText().toString().trim();
                final String password = mPassword.getText().toString().trim();



if (isAllUserInfoValid(email,password)){
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(UserLoginActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if(!task.isSuccessful()){
                                    mLogin.setBackground(getResources().getDrawable(R.drawable.login_bu));
                                    errorMsg.setVisibility(View.VISIBLE);
                                    mEmail.setCompoundDrawablesWithIntrinsicBounds(0, 0,0, 0);
                                    mPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                                }else
                                    errorMsg.setVisibility(View.GONE);
                            }
                        });
                }
            }
        });

        /********************************||Registration||******************************************/
        mRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRegistration.setBackground(getResources().getDrawable(R.drawable.signup_login_buclicked));
                Intent intent=new Intent(UserLoginActivity.this,UserRegistrationActivity.class);
                startActivity(intent);
                finish();
            }
        });
        /********************************||forget password||******************************************/
        forgetPassword.setPaintFlags(forgetPassword.getPaintFlags()|Paint.UNDERLINE_TEXT_FLAG);
        forgetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(UserLoginActivity.this,ResetPasswordActivity.class);
                startActivity(intent);
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

    boolean isAllUserInfoValid(String email,String password){
        boolean isValid=true;
        if (email.isEmpty() && password.isEmpty()) {
            mEmail.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.error_icon, 0);
            mPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.error_icon, 0);
            mLogin.setBackground(getResources().getDrawable(R.drawable.login_bu));
            isValid=false;
        }
        if (!isEmailValid(email)) {
            mLogin.setBackground(getResources().getDrawable(R.drawable.login_bu));
            mEmail.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.error_icon, 0);
            isValid=false;
        }
        if (password.length()<6) {
            mLogin.setBackground(getResources().getDrawable(R.drawable.login_bu));
            mPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.error_icon, 0);
            isValid=false;
        }
        return isValid;
    }
}

