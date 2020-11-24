package com.dev.achraf.demoapp.HistoryPackage;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.dev.achraf.demoapp.HistorySingleActivity;
import com.dev.achraf.demoapp.R;

public class HistoryViewHolders extends RecyclerView.ViewHolder implements View.OnClickListener {
    public TextView rideId,time,destination;

    public HistoryViewHolders(@NonNull View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);

        rideId = itemView.findViewById(R.id.rideId);
        time = itemView.findViewById(R.id.time);
        destination = itemView.findViewById(R.id.destination);

    }

    @Override
    public void onClick(View v) {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Intent intent = new Intent(v.getContext(), HistorySingleActivity.class);
        Bundle b = new Bundle();
        b.putString("rideId", rideId.getText().toString());
        intent.putExtras(b);
        v.getContext().startActivity(intent);
    }
}
