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

    private ArrayList<RecyclerItem> recyclerList;
    private OnItemClickListener onItemClickListener;

    public RecyclerAdapter(ArrayList<RecyclerItem> recyclerList) {
        this.recyclerList = recyclerList;
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.recycler_item, viewGroup, false);
        return new RecyclerViewHolder(v, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder recyclerViewHolder, int i) {
        RecyclerItem currentItem = recyclerList.get(i);

        int imageResource;

        String satelliteName = currentItem.getSatelliteName();
        switch (satelliteName) {
            case "NOAA15":
                satelliteName = "NOAA-15";
                imageResource = R.drawable.noaa_iconv2;
                break;
            case "NOAA18":
                satelliteName = "NOAA-18";
                imageResource = R.drawable.noaa_iconv2;
                break;
            case "NOAA19":
                satelliteName = "NOAA-19";
                imageResource = R.drawable.noaa_iconv2;
                break;
            case "METEOR":
                satelliteName = "METEOR-M N2";
                imageResource = R.drawable.meteorv4;
                break;
            default:
                satelliteName = "ERROR";
                imageResource = R.drawable.ic_error;
                break;
        }

        recyclerViewHolder.imageView.setImageResource(imageResource);
        recyclerViewHolder.satelliteName.setText(satelliteName);

        String concatTimeDate = currentItem.getTimeDate();
        String hours = concatTimeDate.substring(0, 2);
        String minutes = concatTimeDate.substring(2, 4);
        String seconds = concatTimeDate.substring(4, 6);
        String year = concatTimeDate.substring(6, 8);
        String month = concatTimeDate.substring(8, 10);
        String day = concatTimeDate.substring(10, 12);

        recyclerViewHolder.timeDate.setText(hours + ":" + minutes + ":" + seconds + "\t\t" + day + "/" + month + "/" + year);
    }

    @Override
    public int getItemCount() {
        return recyclerList.size();
    }



    public void setOnItemClickListener(OnItemClickListener listener){
        this.onItemClickListener = listener;
    }

    public void removeItemAtPosition(int position) {
        recyclerList.remove(position);
        notifyItemRemoved(position);
    }


    public static class RecyclerViewHolder extends RecyclerView.ViewHolder {

        public ImageView imageView;
        public TextView satelliteName;
        public TextView timeDate;

        public RecyclerViewHolder(View itemView, final OnItemClickListener listener) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewRecycler);
            satelliteName = itemView.findViewById(R.id.textViewRecycler);
            timeDate = itemView.findViewById(R.id.textViewRecycler2);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(listener != null) {
                        int position = getAdapterPosition();
                        if(position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(position);
                        }
                    }
                }
            });
        }

    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }
}
