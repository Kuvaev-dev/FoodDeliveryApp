package kuvaev.mainapp.eatit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.CallbackManager;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import kuvaev.mainapp.eatit.Common.Common;
import kuvaev.mainapp.eatit.Database.Database;
import kuvaev.mainapp.eatit.Model.Favorites;
import kuvaev.mainapp.eatit.Model.Food;
import kuvaev.mainapp.eatit.Model.Order;
import kuvaev.mainapp.eatit.ViewHolder.FoodViewHolder;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class SearchActivity extends AppCompatActivity {
    // search
    FirebaseRecyclerAdapter<Food, FoodViewHolder> adapter;
    FirebaseRecyclerAdapter<Food, FoodViewHolder> searchAdapter;
    List<String> suggestList = new ArrayList<>();
    MaterialSearchBar materialSearchBar;

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;
    FirebaseDatabase database;
    DatabaseReference foodList;

    // favourites
    Database localDB;

    // facebook share
    CallbackManager callbackManager;
    ShareDialog shareDialog;

    // create target from Picasso
    Target target = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            // create photo from bitmap
            SharePhoto photo = new SharePhoto.Builder()
                    .setBitmap(bitmap)
                    .build();

            if (ShareDialog.canShow(SharePhotoContent.class)) {
                SharePhotoContent content = new SharePhotoContent.Builder()
                        .addPhoto(photo)
                        .build();
                shareDialog.show(content);
            }
        }
        @Override
        public void onBitmapFailed(Exception e, Drawable errorDrawable) {

        }
        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    };

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

        setContentView(R.layout.activity_search);

        //init Facebook
        callbackManager = CallbackManager.Factory.create();
        shareDialog = new ShareDialog(this);

        // Firebase
        database = FirebaseDatabase.getInstance();
        foodList = database.getReference("Foods");

        // local Database
        localDB = new Database(this);

        recyclerView = (RecyclerView)findViewById(R.id.recycler_search);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // search
        materialSearchBar = (MaterialSearchBar)findViewById(R.id.searchBar);
        materialSearchBar.setHint("Enter your food");

        // write function to load suggest from firebase
        loadSuggest();

        materialSearchBar.setLastSuggestions(suggestList);
        materialSearchBar.setCardViewElevation(10);
        materialSearchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // when user type their text, we will change suggest list
                List<String> suggest = new ArrayList<>();
                for (String search:suggestList){ // loop in suggest list
                    if (search.toLowerCase().contains(materialSearchBar.getText().toLowerCase()))
                        suggest.add(search);
                }
                materialSearchBar.setLastSuggestions(suggest);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        materialSearchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {
                // when search bar is closed
                // restore original adapter
                if (!enabled)
                    recyclerView.setAdapter(adapter);
            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                // when search finish
                // show result of search adapter
                startSearch(text);
            }

            @Override
            public void onButtonClicked(int buttonCode) {

            }
        });

        // load all food
        loadAllFoods();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadAllFoods() {
        // create query by category ID
        Query searchByName = foodList;

        // create option with query
        FirebaseRecyclerOptions<Food> foodOptions = new FirebaseRecyclerOptions.Builder<Food>()
                .setQuery(searchByName, Food.class)
                .build();

        adapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(foodOptions) {
            @Override
            protected void onBindViewHolder(@NonNull FoodViewHolder holder, int position, @NonNull Food model) {
                holder.food_name.setText(model.getName());
                holder.food_price.setText(String.format("RM %s", model.getPrice()));
                Picasso.get().load(model.getImage()).into(holder.food_image);

                // Quick cart
                holder.quick_cart.setOnClickListener(v -> {
                    boolean isExists = new Database(getBaseContext()).checkFoodExists(adapter.getRef(position).getKey(), Common.currentUser.getPhone());
                    if (!isExists) {
                        new Database(getBaseContext()).addToCart(new Order(
                                Common.currentUser.getPhone(),
                                adapter.getRef(position).getKey(),
                                model.getName(),
                                "1",
                                model.getPrice(),
                                model.getImage()

                        ));
                    } else {
                        new Database(getBaseContext()).increaseCart(Common.currentUser.getPhone(), adapter.getRef(position).getKey());
                    }
                    Toast.makeText(SearchActivity.this, "Added to Cart", Toast.LENGTH_SHORT).show();
                });

                // add favourites
                if (localDB.isFavourite(adapter.getRef(position).getKey(), Common.currentUser.getPhone()))
                    holder.fav_image.setImageResource(R.drawable.ic_favorite_black_24dp);

                // click to share
                holder.share_image.setOnClickListener(v -> Picasso.get()
                        .load(model.getImage())
                        .into(target));

                //click to change the status of favourites
                holder.fav_image.setOnClickListener(v -> {
                    Favorites favorites = new Favorites();
                    favorites.setFoodId(adapter.getRef(position).getKey());
                    favorites.setFoodName(model.getName());
                    favorites.setFoodDescription(model.getDescription());
                    favorites.setFoodImage(model.getImage());
                    favorites.setFoodMenuId(model.getMenuId());
                    favorites.setUserPhone(Common.currentUser.getPhone());
                    favorites.setFoodPrice(model.getPrice());

                    if (!localDB.isFavourite(adapter.getRef(position).getKey(), Common.currentUser.getPhone())) {
                        localDB.addToFavourites(favorites);
                        holder.fav_image.setImageResource(R.drawable.ic_favorite_black_24dp);
                        Toast.makeText(SearchActivity.this, "" + model.getName() +
                                " was added to Favourites", Toast.LENGTH_SHORT).show();
                    } else {
                        localDB.removeFromFavourites(adapter.getRef(position).getKey(), Common.currentUser.getPhone());
                        holder.fav_image.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                        Toast.makeText(SearchActivity.this, "" + model.getName() +
                                " was removed from Favourites", Toast.LENGTH_SHORT).show();
                    }
                });

                holder.setItemClickListener((view, position1, isLongClick) -> {
                    // start new activity
                    Intent foodDetail = new Intent(SearchActivity.this, FoodDetail.class);
                    foodDetail.putExtra("FoodId", adapter.getRef(position1).getKey()); //send FoodId to new activity
                    startActivity(foodDetail);
                });
            }

            @NonNull
            @Override
            public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.food_item, parent, false);
                return new FoodViewHolder(itemView);
            }
        };
        adapter.startListening();
        recyclerView.setAdapter(adapter);

        // Animation
        Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
        recyclerView.scheduleLayoutAnimation();
    }

    private void startSearch(CharSequence text) {
        // create query by name
        Query searchByName = foodList.orderByChild("name").equalTo(text.toString());
        // create option with query
        FirebaseRecyclerOptions<Food> foodOptions = new FirebaseRecyclerOptions.Builder<Food>()
                .setQuery(searchByName, Food.class)
                .build();

        searchAdapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(foodOptions) {
            @Override
            protected void onBindViewHolder(@NonNull FoodViewHolder viewHolder, int position, @NonNull Food model) {
                viewHolder.food_name.setText(model.getName());
                Picasso.get().load(model.getImage()).into(viewHolder.food_image);

                viewHolder.setItemClickListener((view, position1, isLongClick) -> {
                    // start new activity
                    Intent foodDetail = new Intent(SearchActivity.this, FoodDetail.class);
                    foodDetail.putExtra("FoodId", searchAdapter.getRef(position1).getKey()); //send FoodId to new activity
                    startActivity(foodDetail);
                });
            }

            @NonNull
            @Override
            public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.food_item, parent, false);
                return new FoodViewHolder(itemView);
            }
        };
        // set adapter for recycle view is search result
        searchAdapter.startListening();
        recyclerView.setAdapter(searchAdapter);
    }

    private void loadSuggest() {
        foodList.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot:dataSnapshot.getChildren()){
                    Food item = postSnapshot.getValue(Food.class);
                    // add food name to suggest list
                    assert item != null;
                    suggestList.add(item.getName());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onStop() {
        if (adapter !=null)
            adapter.stopListening();
        if (searchAdapter !=null)
            searchAdapter.stopListening();
        super.onStop();
    }

    @Override
    protected void onResume() {
        if(adapter !=null)
            adapter.startListening();
        if (searchAdapter !=null)
            searchAdapter.startListening();
        super.onResume();
    }
}
