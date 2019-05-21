package be.kuleuven.ee5.eliasstalpaert.sosarlink;

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
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public class PassesFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private SharedPreferences sharedPreferences;
    private ArrayList<RecyclerItem> recyclerList;

    private View fragmentView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle("Captures");
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        recyclerList = new ArrayList<>();

    }

    @Override
    public void onResume() {
        super.onResume();
        update();
    }

    public void update() {

        HashSet<String> stringSet = (HashSet<String>) sharedPreferences.getStringSet("passes",null);
        if(stringSet != null) {
            Log.d("PassesFragment", "Stringset found");
            Iterator<String> iterator = stringSet.iterator();
            while(iterator.hasNext()) {
                String next = iterator.next();
                parseString(next);
            }
            mAdapter.notifyDataSetChanged();
        }
        else {
            Log.d("PassesFragment", "Stringset not found");
        }

    }

    public void parseString(String s){
        if(s.length() >= 18){
            String satellite_name = s.substring(0,6);
            String concatTimeDate = s.substring(6,18);
            recyclerList.add(new RecyclerItem(0, satellite_name, concatTimeDate));
        }
    }

    public void removeItem(int position) {
        RecyclerItem toBeRemoved_item = recyclerList.get(position);
        if(toBeRemoved_item != null){
            String toBeRemoved_string = toBeRemoved_item.getText1()+toBeRemoved_item.getText2();
            Log.d("PassesFragment", "To be removed string: " + toBeRemoved_string);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            HashSet<String> stringSet = (HashSet<String>) sharedPreferences.getStringSet("passes",null);
            if(stringSet != null) {
                Iterator<String> iterator = stringSet.iterator();
                boolean found = false;
                while(iterator.hasNext() && found == false) {
                    String next = iterator.next();
                    if(next.equals(toBeRemoved_string)){
                        iterator.remove();
                        found = true;
                        editor.putStringSet("passes", stringSet);
                        editor.apply();
                        Log.d("PassesFragment", "Element removed from SharedPreferences");
                    }
                }
                if(found == false) {
                    Log.d("PassesFragment", "Element not found in SharedPreferences");
                }
            }
            else {
                Log.d("PassesFragment", "Stringset not found");
            }
        }
        ((RecyclerAdapter)mAdapter).removeItem(position);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_passes, container, false);

        mRecyclerView = fragmentView.findViewById(R.id.recyclerView);
        mRecyclerView.setHasFixedSize(true); //better performance
        mLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        mAdapter = new RecyclerAdapter(recyclerList);

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

        return fragmentView;
    }
}
