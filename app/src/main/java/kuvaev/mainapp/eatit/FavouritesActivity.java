package kuvaev.mainapp.eatit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.RelativeLayout;

import com.google.android.material.snackbar.Snackbar;

import java.util.Objects;

import kuvaev.mainapp.eatit.Common.Common;
import kuvaev.mainapp.eatit.Database.Database;
import kuvaev.mainapp.eatit.Helper.RecyclerItemTouchHelper;
import kuvaev.mainapp.eatit.Interface.RecyclerItemTouchHelperListener;
import kuvaev.mainapp.eatit.Model.Favorites;
import kuvaev.mainapp.eatit.ViewHolder.FavouritesAdapter;
import kuvaev.mainapp.eatit.ViewHolder.FavouritesViewHolder;

public class FavouritesActivity extends AppCompatActivity implements RecyclerItemTouchHelperListener {
    RecyclerView recyclerView;
    FavouritesAdapter adapter;
    RelativeLayout rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourites);

        rootLayout = findViewById(R.id.root_Layout);

        recyclerView = findViewById(R.id.recycler_favorites);
        recyclerView.setVisibility(View.VISIBLE);

        // Swipe to delete item
        // Very important put code ItemTouchHelper.... AFTER init RecyclerView
        ItemTouchHelper.SimpleCallback itemTouchHelperCallBack = new RecyclerItemTouchHelper(0,
                ItemTouchHelper.LEFT,
                this);
        new ItemTouchHelper(itemTouchHelperCallBack).attachToRecyclerView(recyclerView);

        loadAllFavorites();
    }

    private void loadAllFavorites() {
        adapter = new FavouritesAdapter(this, new Database(this).getAllFavorites(Common.currentUser.getPhone()));
        recyclerView.setAdapter(adapter);
        startRecyclerViewAnimation();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void startRecyclerViewAnimation() {
        Context context = recyclerView.getContext();
        LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(
                context , R.anim.layout_slide_right);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //Set Animation
        recyclerView.setLayoutAnimation(controller);
        Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
        recyclerView.scheduleLayoutAnimation();
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof FavouritesViewHolder){
            String name = ((FavouritesAdapter) Objects.requireNonNull(recyclerView.getAdapter())).getItem(position).getFoodName();

            final Favorites deleteItem = ((FavouritesAdapter)recyclerView.getAdapter()).getItem(viewHolder.getBindingAdapterPosition());
            final int deleteIndex = viewHolder.getBindingAdapterPosition();

            adapter.removeItem(viewHolder.getBindingAdapterPosition());
            new Database(getBaseContext()).removeFromFavorites(deleteItem.getFoodId() , Common.currentUser.getPhone());

            // Make Snackbar
            Snackbar snackbar = Snackbar.make(rootLayout , name + " is removed from favorites!", Snackbar.LENGTH_LONG);
            snackbar.setAction("UNDO", v -> {
                adapter.restoreItem(deleteItem ,deleteIndex);
                new Database(getBaseContext()).addToFavorites(deleteItem);
            });
            snackbar.setActionTextColor(Color.YELLOW);
            snackbar.show();
        }
    }
}