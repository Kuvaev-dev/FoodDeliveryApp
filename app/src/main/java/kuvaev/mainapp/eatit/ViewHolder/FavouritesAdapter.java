package kuvaev.mainapp.eatit.ViewHolder;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

import kuvaev.mainapp.eatit.FoodDetailActivity;
import kuvaev.mainapp.eatit.Interface.ItemClickListener;
import kuvaev.mainapp.eatit.Model.Favorites;
import kuvaev.mainapp.eatit.R;

public class FavouritesAdapter extends RecyclerView.Adapter<FavouritesViewHolder> {
    private final Context context;
    private final List<Favorites> favoritesList;

    public FavouritesAdapter(Context context, List<Favorites> favoritesList) {
        this.context = context;
        this.favoritesList = favoritesList;
    }

    @NonNull
    @Override
    public FavouritesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.favourites_item, parent, false);
        return new FavouritesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavouritesViewHolder viewHolder, int position) {
        viewHolder.food_name.setText(favoritesList.get(position).getFoodName());
        Picasso.with(context)
                .load(favoritesList.get(position).getFoodImage())
                .into(viewHolder.food_image);

        final Favorites local = favoritesList.get(position);
        viewHolder.setItemClickListener((view, position1, isLongClick) -> {
            // Start new Activity
            Intent foodDetail = new Intent(context, FoodDetailActivity.class);
            foodDetail.putExtra("FoodId", favoritesList.get(position1).getFoodId()); // Send Food Id to new Activity
            context.startActivity(foodDetail);
        });
    }

    @Override
    public int getItemCount() {
        return favoritesList.size();
    }

    public void removeItem(int position){
        favoritesList.remove(position);
        notifyItemRemoved(position);
    }

    public void restoreItem(Favorites item, int position){
        favoritesList.add(position , item);
        notifyItemInserted(position);
    }

    public Favorites getItem(int position){
        return favoritesList.get(position);
    }
}
