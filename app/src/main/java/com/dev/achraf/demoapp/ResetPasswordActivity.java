package com.dev.achraf.demoapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {
    private Button restPassword;
    private EditText userEmail;
    private FirebaseAuth auth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        restPassword=findViewById(R.id.restPassword);
        userEmail=findViewById(R.id.email);

        auth=FirebaseAuth.getInstance();
        restPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = userEmail.getText().toString().trim();
                if (!isEmailValid(email)) {
                    userEmail.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.error_icon, 0);
                } else {
                    auth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                userEmail.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                                Toast.makeText(ResetPasswordActivity.this, "We have sent you instructions to reset your password!", Toast.LENGTH_SHORT).show();
                            } else {
                                userEmail.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.error_icon, 0);
                            }
                        }
                    });
                }
            }
        });
    }
    boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }


}
