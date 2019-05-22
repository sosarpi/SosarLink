package be.kuleuven.ee5.eliasstalpaert.sosarlink;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;

import static android.content.Context.JOB_SCHEDULER_SERVICE;

public class PassesFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private RecyclerAdapter mAdapterCasted;
    //private ArrayList<RecyclerItem> recyclerList;

    private Button refreshButton;
    private BroadcastReceiver mNotificationReceiver;
    private MainActivity mainActivity;

    private View fragmentView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainActivity = (MainActivity) getActivity();
        mainActivity.setTitle("Captures");

        mNotificationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                update();
                Log.d("Notification Receiver", "Broadcast received");
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        mainActivity.registerReceiver(mNotificationReceiver, new IntentFilter("SATELLITE"));
        update();
    }

    @Override
    public void onPause() {
        super.onPause();
        mainActivity.unregisterReceiver(mNotificationReceiver);
    }

    public void update() {

        Log.d("PassesFragment", "Updating UI");

        ArrayList<String> stringList = MainActivity.getArrayList(MainActivity.LIST_NAME,mainActivity);
        if (stringList != null) {
            Log.d("PassesFragment", "Stringlist found");
            Iterator<String> iterator = stringList.iterator();
            while (iterator.hasNext()) {
                String next = iterator.next();
                if(!parseString(next)) iterator.remove();
            }
            mAdapter.notifyDataSetChanged();
            mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
        } else {
            Log.d("PassesFragment", "Stringlist not found");
        }

    }

    public boolean parseString(String s) {
        if (s.length() >= 18) {
            String satellite_name = s.substring(0, 6);
            String concatTimeDate = s.substring(6, 18);
            RecyclerItem new_item  = new RecyclerItem(0,satellite_name, concatTimeDate);
            Iterator<RecyclerItem> itemIterator = mAdapterCasted.getmRecyclerList().iterator();
            boolean found = false;
            while(itemIterator.hasNext() && found == false) {
                RecyclerItem next = itemIterator.next();
                if(next.getText1().equals(satellite_name) && next.getText2().equals(concatTimeDate)) {
                    found = true;
                    Log.d("parseString", "Item already in RecyclerView, skipping...");
                }
            }
            if(found == false) {
                mRecyclerView.smoothScrollToPosition(0);
                mAdapterCasted.getmRecyclerList().add(new RecyclerItem(0, satellite_name, concatTimeDate));
            }
            return true;
        }
        return false;
    }

    public void removeItem(int position) {
        RecyclerItem toBeRemoved_item = mAdapterCasted.getmRecyclerList().get(position);
        if (toBeRemoved_item != null) {
            String toBeRemoved_string = toBeRemoved_item.getText1() + toBeRemoved_item.getText2();
            Log.d("PassesFragment", "To be removed string: " + toBeRemoved_string);
            ArrayList<String> stringList = MainActivity.getArrayList(MainActivity.LIST_NAME,mainActivity);
            if (stringList != null) {
                Iterator<String> iterator = stringList.iterator();
                boolean found = false;
                while (iterator.hasNext() && found == false) {
                    String next = iterator.next();
                    if (next.equals(toBeRemoved_string)) {
                        iterator.remove();
                        found = true;
                        MainActivity.saveArrayList(stringList,MainActivity.LIST_NAME,mainActivity);
                        Log.d("PassesFragment", "Element removed from SharedPreferences");
                    }
                }
                if (found == false) {
                    Log.d("PassesFragment", "Element not found in SharedPreferences");
                }
            } else {
                Log.d("PassesFragment", "Stringlist not found");
            }
        }
        ((RecyclerAdapter) mAdapter).removeItem(position);
    }

    public void refreshJob() {
        JobScheduler mJobScheduler = (JobScheduler) mainActivity
                .getSystemService(JOB_SCHEDULER_SERVICE);
        mJobScheduler.cancelAll();

        JobInfo.Builder mJobBuilder =
                new JobInfo.Builder(1,
                        new ComponentName(mainActivity, Job.class))
                        .setPersisted(true)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);
        int resultCode = mJobScheduler.schedule(mJobBuilder.build());
        if(resultCode == JobScheduler.RESULT_SUCCESS){
            Log.d("Passes Fragment", "Refreshed successfully");
        }
        else {
            Log.d("Passes Fragment", "Failed to refresh");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_passes, container, false);

        mRecyclerView = fragmentView.findViewById(R.id.recyclerView);
        refreshButton = fragmentView.findViewById(R.id.refreshButton);

        mRecyclerView.setHasFixedSize(true); //better performance
        mLayoutManager = new LinearLayoutManager(mainActivity);
        ((LinearLayoutManager) mLayoutManager).setReverseLayout(true);
        ((LinearLayoutManager) mLayoutManager).setStackFromEnd(true);
        mAdapter = new RecyclerAdapter(new ArrayList<RecyclerItem>());
        mAdapterCasted = (RecyclerAdapter) mAdapter;
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                removeItem(viewHolder.getAdapterPosition());
            }
        }).attachToRecyclerView(mRecyclerView);

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshJob();
            }
        });

        return fragmentView;
    }
}
