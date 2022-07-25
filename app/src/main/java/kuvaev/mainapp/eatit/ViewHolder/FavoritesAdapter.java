package kuvaev.mainapp.eatit.ViewHolder;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

import kuvaev.mainapp.eatit.Common.Common;
import kuvaev.mainapp.eatit.Database.Database;
import kuvaev.mainapp.eatit.FoodDetail;
import kuvaev.mainapp.eatit.Model.Favorites;
import kuvaev.mainapp.eatit.Model.Order;
import kuvaev.mainapp.eatit.R;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesViewHolder> {
    private final Context context;
    private final List<Favorites> favoritesList;

    public FavoritesAdapter(Context context, List<Favorites> favoritesList) {
        this.context = context;
        this.favoritesList = favoritesList;
    }

    @NonNull
    @Override
    public FavoritesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.favorites_item, parent, false);
        return new FavoritesViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoritesViewHolder viewHolder, int position) {
        viewHolder.food_name.setText(favoritesList.get(position).getFoodName());
        viewHolder.food_price.setText(String.format("RM %s", favoritesList.get(position).getFoodPrice()));
        Picasso.get().load(favoritesList.get(position).getFoodImage()).into(viewHolder.food_image);

        //Quick cart

        viewHolder.quick_cart.setOnClickListener(v -> {
            boolean isExists = new Database(context).checkFoodExists(favoritesList.get(position).getFoodId(), Common.currentUser.getPhone());
            if (!isExists) {
                new Database(context).addToCart(new Order(
                        Common.currentUser.getPhone(),
                        favoritesList.get(position).getFoodId(),
                        favoritesList.get(position).getFoodName(),
                        "1",
                        favoritesList.get(position).getFoodPrice(),
                        favoritesList.get(position).getFoodImage()
                ));
            } else {
                new Database(context).increaseCart(Common.currentUser.getPhone(),
                        favoritesList.get(position).getFoodId());
            }
            Toast.makeText(context, "Added to Cart", Toast.LENGTH_SHORT).show();
        });

        viewHolder.setItemClickListener((view, position1, isLongClick) -> {
            //start new activity
            Intent foodDetail = new Intent(context, FoodDetail.class);
            foodDetail.putExtra("FoodId", favoritesList.get(position1).getFoodId()); //send FoodId to new activity
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
        favoritesList.add(position,item);
        notifyItemInserted(position);
    }

    public Favorites getItem(int position){
        return favoritesList.get(position);
    }
}
