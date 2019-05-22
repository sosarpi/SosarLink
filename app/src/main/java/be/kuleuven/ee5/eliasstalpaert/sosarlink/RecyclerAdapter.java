package be.kuleuven.ee5.eliasstalpaert.sosarlink;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.RecyclerViewHolder> {

    private ArrayList<RecyclerItem> mRecyclerList;

    public static class RecyclerViewHolder extends RecyclerView.ViewHolder {

        public ImageView mImageView;
        public TextView mTextView1;
        public TextView mTextView2;

        public RecyclerViewHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.imageViewRecycler);
            mTextView1 = itemView.findViewById(R.id.textViewRecycler);
            mTextView2 = itemView.findViewById(R.id.textViewRecycler2);
        }

    }

    public RecyclerAdapter(ArrayList<RecyclerItem> recyclerList) {
        mRecyclerList = recyclerList;
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.example_item, viewGroup, false);
        RecyclerViewHolder rvh = new RecyclerViewHolder(v);
        return rvh;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder recyclerViewHolder, int i) {
        RecyclerItem currentItem = mRecyclerList.get(i);

        int image_resource;

        String satellite_name = currentItem.getText1();
        switch (satellite_name) {
            case "NOAA15":
                satellite_name = "NOAA-15";
                image_resource = R.mipmap.ic_launcher;
                break;
            case "NOAA18":
                satellite_name = "NOAA-18";
                image_resource = R.mipmap.ic_launcher;
                break;
            case "NOAA19":
                satellite_name = "NOAA-19";
                image_resource = R.mipmap.ic_launcher;
                break;
            case "METEOR":
                satellite_name = "METEOR-M N2";
                image_resource = R.mipmap.ic_launcher;
                break;
            default:
                satellite_name = "ERROR";
                image_resource = R.mipmap.ic_launcher;
                break;
        }

        recyclerViewHolder.mImageView.setImageResource(image_resource);

        recyclerViewHolder.mTextView1.setText(satellite_name);
        String concatTimeDate = currentItem.getText2();
        String hours = concatTimeDate.substring(0, 2);
        String minutes = concatTimeDate.substring(2, 4);
        String seconds = concatTimeDate.substring(4, 6);
        String day = concatTimeDate.substring(6, 8);
        String month = concatTimeDate.substring(8, 10);
        String year = concatTimeDate.substring(10, 12);
        recyclerViewHolder.mTextView2.setText(hours + ":" + minutes + ":" + seconds + "\t" + day + "/" + month + "/" + year);
    }

    public void removeItem(int position) {
        mRecyclerList.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public int getItemCount() {
        return mRecyclerList.size();
    }

    public ArrayList<RecyclerItem> getmRecyclerList() {
        return mRecyclerList;
    }
}
