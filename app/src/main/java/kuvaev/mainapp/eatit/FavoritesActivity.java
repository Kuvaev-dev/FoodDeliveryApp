package kuvaev.mainapp.eatit;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.Objects;

import kuvaev.mainapp.eatit.Common.Common;
import kuvaev.mainapp.eatit.Database.Database;
import kuvaev.mainapp.eatit.Helper.RecyclerItemTouchHelper;
import kuvaev.mainapp.eatit.Interface.RecyclerItemTouchHelperListener;
import kuvaev.mainapp.eatit.Model.Favorites;
import kuvaev.mainapp.eatit.ViewHolder.FavoritesAdapter;
import kuvaev.mainapp.eatit.ViewHolder.FavoritesViewHolder;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class FavoritesActivity extends AppCompatActivity implements RecyclerItemTouchHelperListener {
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    FavoritesAdapter adapter;
    RelativeLayout rootLayout;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // add calligraphy
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/restaurant_font.otf")
                .setFontAttrId(R.attr.fontPath)
                .build());
        setContentView(R.layout.activity_favorites);

        rootLayout = findViewById(R.id.root_layout);

        recyclerView = findViewById(R.id.recycler_fav);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        //Swipe to delete
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper(0,ItemTouchHelper.LEFT,this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);

        loadFavorites();
    }

    private void loadFavorites() {
        adapter = new FavoritesAdapter(this, new Database(this).getAllFavorites(Common.currentUser.getPhone()));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof FavoritesViewHolder) {
            String name = ((FavoritesAdapter) Objects.requireNonNull(recyclerView.getAdapter())).getItem(position).getFoodName();

            final Favorites deleteItem = ((FavoritesAdapter)recyclerView.getAdapter()).getItem(viewHolder.getBindingAdapterPosition());
            final int deleteIndex = viewHolder.getBindingAdapterPosition();

            adapter.removeItem(viewHolder.getBindingAdapterPosition());
            new Database(getBaseContext()).removeFromFavourites(deleteItem.getFoodId(), Common.currentUser.getPhone());

            Snackbar snackbar = Snackbar.make(rootLayout,name + " removed from favorites!", Snackbar.LENGTH_LONG);
            snackbar.setAction("UNDO", v -> {
                adapter.restoreItem(deleteItem,deleteIndex);
                new Database(getBaseContext()).addToFavourites(deleteItem);
            });
            snackbar.setActionTextColor(Color.RED);
            snackbar.show();
        }
    }
}
