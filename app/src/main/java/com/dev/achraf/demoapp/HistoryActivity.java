package com.dev.achraf.demoapp;


import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import com.dev.achraf.demoapp.HistoryPackage.HistoryAdapter;
import com.dev.achraf.demoapp.HistoryPackage.HistoryObject;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.text.format.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {
    private String customerOrDriver, userId;

    private RecyclerView mHistoryRecyclerView;
    private RecyclerView.Adapter mHistoryAdapter;
    private RecyclerView.LayoutManager mHistoryLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        //RecyclerView parameter
        mHistoryRecyclerView =findViewById(R.id.historyRecyclerView);
        mHistoryRecyclerView.setNestedScrollingEnabled(false);
        mHistoryRecyclerView.setHasFixedSize(true);
        mHistoryLayoutManager = new LinearLayoutManager(HistoryActivity.this);
        mHistoryRecyclerView.setLayoutManager(mHistoryLayoutManager);
        //List<HistoryObject> + context
        mHistoryAdapter = new HistoryAdapter(getDataSetHistory(), HistoryActivity.this);
        mHistoryRecyclerView.setAdapter(mHistoryAdapter);

        //userType from Map-Onclick-HistoryButton
        customerOrDriver = getIntent().getExtras().getString("customerOrDriver");
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        getUserHistoryIds();
    }

    private void getUserHistoryIds() {
        DatabaseReference userHistoryDatabase = FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child(customerOrDriver)
                .child(userId)
                .child("history");
        userHistoryDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot history : dataSnapshot.getChildren()){
                        //
                        FetchRideInformation(history.getKey());
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void FetchRideInformation(String rideKey) {
        DatabaseReference historyDatabase = FirebaseDatabase.getInstance().getReference().child("history").child(rideKey);
        historyDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    String rideId = dataSnapshot.getKey();
                    long time=0L;
                    String destination="null";
                    for (DataSnapshot child:dataSnapshot.getChildren()){
                        if (child.getKey().equalsIgnoreCase("timestamp"))
                            time=Long.parseLong(child.getValue().toString());

                        if(child.getKey().equalsIgnoreCase("destination"))
                            destination=child.getValue().toString();
                    }
                    HistoryObject obj = new HistoryObject(rideId,getDate(time),destination);
                    resultsHistory.add(obj);
                    mHistoryAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }

    private String getDate(long time) {
        Calendar calendar=Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(time*1000);
        String data=DateFormat.format("MM-dd-yyyy hh:mm", calendar).toString();
        return data;
    }


    private ArrayList resultsHistory = new ArrayList<HistoryObject>();
    private ArrayList<HistoryObject> getDataSetHistory() {

        return resultsHistory;
    }
}
