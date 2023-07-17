package com.csis4495_cmk.webuy.fragments;

import static android.content.ContentValues.TAG;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.csis4495_cmk.webuy.R;
import com.csis4495_cmk.webuy.adapters.SellerGroupListRecyclerAdapter;
import com.csis4495_cmk.webuy.models.Group;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SellerGroupClosingSoonFragment extends Fragment {

    private NavController navController;

    private RecyclerView mRecyclerView;

    private FirebaseAuth auth = FirebaseAuth.getInstance();

    private FirebaseUser firebaseUser;

    private FirebaseDatabase firebaseDatabase;

    private DatabaseReference dBRef;

    private DatabaseReference groupRef;

    private SellerGroupListRecyclerAdapter groupListRecyclerAdapter;

    private String sellerId;

    private List<Group> closingSoonGroups = new ArrayList<>();

    private List<String> groupIds = new ArrayList<>();

    private TextView tv_group_list_no_closing_soon;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_seller_group_closing_soon, container, false);
        auth = FirebaseAuth.getInstance();
        firebaseUser = auth.getCurrentUser();
        if (firebaseUser != null) {
            sellerId = firebaseUser.getUid();
        }

        firebaseDatabase = FirebaseDatabase.getInstance();
        dBRef = firebaseDatabase.getReference();
        groupRef = dBRef.child("Group");

        navController = NavHostFragment.findNavController(SellerGroupClosingSoonFragment.this);

        mRecyclerView = view.findViewById(R.id.rv_group_list_closing_soon);

        tv_group_list_no_closing_soon = view.findViewById(R.id.tv_group_list_no_closing_soon);

        groupListRecyclerAdapter = new SellerGroupListRecyclerAdapter();

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mRecyclerView.setAdapter(groupListRecyclerAdapter);

        getGroupsData();

        return view;

    }

    private void getGroupsData(){
        groupRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                closingSoonGroups.clear();
                groupIds.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                    Group gp = dataSnapshot.getValue(Group.class);
                    long currentTime = System.currentTimeMillis();
                    String groupId = dataSnapshot.getKey();

                    //Get seller's not closed groups data and update status
                    if(gp.getSellerId().equals(sellerId) && gp.getStatus() != 2){
                        if(gp.getGroupType() == 1){
                            if (currentTime < gp.getStartTimestamp()) {
                                // Group is not yet opened
                                gp.setStatus(0);
                            } else if (currentTime > gp.getEndTimestamp()) {
                                // Group is closed
                                gp.setStatus(2);
                            } else {
                                // Group is opening
                                gp.setStatus(1);
                            }
                            DatabaseReference currentGp = groupRef.child(groupId);
                            currentGp.child("status").setValue(gp.getStatus()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "Group status is updated: " + Integer.toString(gp.getStatus()));
                                    } else {
                                        Log.d(TAG, "Group status update error", task.getException());
                                    }
                                }
                            });
                        }

                        long remainingTime = gp.getEndTimestamp() - currentTime;
                        long remainingInDays = TimeUnit.MILLISECONDS.toDays(remainingTime);
                        if (remainingInDays < 5 && gp.getStatus() == 1) {
                            closingSoonGroups.add(gp);
                            groupIds.add(groupId);
                        }
                    }
                }

                if (closingSoonGroups.isEmpty()) {
                    tv_group_list_no_closing_soon.setVisibility(View.VISIBLE);
                    mRecyclerView.setVisibility(View.GONE);
                }else{
                    tv_group_list_no_closing_soon.setVisibility(View.GONE);
                    mRecyclerView.setVisibility(View.VISIBLE);

                    groupListRecyclerAdapter.setContext(getContext());
                    groupListRecyclerAdapter.setGroups(closingSoonGroups);
                    groupListRecyclerAdapter.setGroupIds(groupIds);
                    groupListRecyclerAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }
}