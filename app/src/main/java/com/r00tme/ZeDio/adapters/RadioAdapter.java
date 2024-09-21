package com.r00tme.ZeDio.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.r00tme.ZeDio.R;
import com.r00tme.ZeDio.classes.Radio;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter class for managing and displaying radio stations in a RecyclerView.
 * Implements Filterable for filtering radio stations by name, genre, or country.
 */
public class RadioAdapter extends RecyclerView.Adapter<RadioAdapter.ViewHolder> implements Filterable {

    private static final String TAG = "RadioViewAdapter";
    private final ArrayList<Radio> radioListFull;
    private final ArrayList<Radio> radioList;
    private final Context mContext;

    /**
     * Constructor for RadioAdapter.
     * @param radioList The list of Radio objects to display.
     * @param context The application context.
     */
    public RadioAdapter(ArrayList<Radio> radioList, Context context) {
        this.radioList = radioList;
        radioListFull = new ArrayList<>(radioList);
        this.mContext = context;
    }

    /**
     * Inflates the RecyclerView item layout and creates the ViewHolder.
     * @param parent The parent view group.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a view of the RecyclerView item.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_item, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds data to the view holder for a specific position.
     * @param holder The ViewHolder to update with data.
     * @param position The position of the item in the data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) throws ArrayIndexOutOfBoundsException {

        Radio currentRadio = this.radioList.get(position);

        RequestOptions options = new RequestOptions()
                .priority(Priority.HIGH)
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.ALL);

        Glide.with(mContext).asBitmap().load(currentRadio.getRadioLogo()).apply(options).into(holder.radioLogo);

        holder.radioName.setText(currentRadio.getRadioName());
        holder.radioGenre.setText(currentRadio.getRadioGenre());
        holder.radioCountry.setText(currentRadio.getRadioCountry());

        holder.radioViewLayout.setOnClickListener(v -> {
            // Not in use at the moment. Activity moved to MainActivity class
        });
    }

    /**
     * Returns the total number of radio stations in the list.
     * @return The item count of the RecyclerView.
     */
    @Override
    public int getItemCount() {
        return this.radioList.size();
    }

    /**
     * Provides the filter implementation for filtering radio stations.
     * @return A filter object that filters radio stations.
     */
    @Override
    public Filter getFilter() {
        return radioFilter;
    }

    /**
     * ViewHolder class that describes an item view and metadata about its place within the RecyclerView.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView radioLogo;
        TextView radioName;
        TextView radioGenre;
        TextView radioCountry;
        CardView radioViewLayout;

        /**
         * Constructor for ViewHolder.
         * @param itemView The view that holds the individual radio station data.
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            radioLogo = itemView.findViewById(R.id.radio_logo);
            radioName = itemView.findViewById(R.id.radio_name);
            radioGenre = itemView.findViewById(R.id.radio_genre);
            radioCountry = itemView.findViewById(R.id.radio_country);
            radioViewLayout = itemView.findViewById(R.id.radio_view_layout);
        }
    }

    private final Filter radioFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Radio> filteredRadioList = new ArrayList<>();
            String[] getData = constraint.toString().split("#");
            if (getData.length == 1) {
                filteredRadioList.addAll(radioListFull);
            } else {
                String filterBy = getData[0];
                String searchSequence = getData[1].toLowerCase().trim();
                for (Radio item : radioListFull) {
                    if (filterBy.contains("Name") && item.getRadioName().toLowerCase().contains(searchSequence)) {
                        filteredRadioList.add(item);
                    } else if (filterBy.contains("Genre") && item.getRadioGenre().toLowerCase().contains(searchSequence)) {
                        filteredRadioList.add(item);
                    } else if (filterBy.contains("Country") && item.getRadioCountry().toLowerCase().contains(searchSequence)) {
                        filteredRadioList.add(item);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredRadioList;
            return results;
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            radioList.clear();
            radioList.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };
}
