package kuvaev.mainapp.eatit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.share.Sharer;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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
import kuvaev.mainapp.eatit.ViewHolder.FoodViewHolder;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class SearchActivity extends AppCompatActivity {
    // Search functionality
    FirebaseRecyclerAdapter<Food, FoodViewHolder> searchAdapter;
    List<String> suggestList = new ArrayList<>();
    MaterialSearchBar materialSearchBar;

    RecyclerView recyclerView;

    FirebaseDatabase database;
    DatabaseReference foodList;
    FirebaseRecyclerAdapter<Food, FoodViewHolder> adapter;

    // Favorites
    Database localDB;

    // FaceBook Share
    CallbackManager callbackManager;
    ShareDialog shareDialog;

    // Create Target from Picasso
    Target target = new Target() {
        @Override
        public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
            //Share Photo from bitmap
            if (ShareDialog.canShow(SharePhotoContent.class)){
                shareDialog.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {
                    @Override
                    public void onSuccess(Sharer.Result result) {
                        SharePhoto photo = new SharePhoto.Builder()
                                .setBitmap(bitmap)
                                .build();

                        final SharePhotoContent content = new SharePhotoContent.Builder()
                                .addPhoto(photo)
                                .build();

                        shareDialog.show(content);
                        Toast.makeText(SearchActivity.this, "Photo is shared successfully !!!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancel() {
                        Toast.makeText(SearchActivity.this, "Process of sharing is canceled !", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull FacebookException error) {
                        Toast.makeText(SearchActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

            }
            else
                Toast.makeText(SearchActivity.this, "you can't share this photo !!!", Toast.LENGTH_SHORT).show();
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

        // Note: add this code before setContentView method
        // Add new Font to activity
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/restaurant_font.otf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        setContentView(R.layout.activity_search);

        // Init FaceBook
        callbackManager  = CallbackManager.Factory.create();
        shareDialog = new ShareDialog(this);

        // Init Firebase
        database = FirebaseDatabase.getInstance();
        foodList = database.getReference("Foods");

        // LocalDB
        localDB = new Database(this);

        // Init RecyclerView
        recyclerView = findViewById(R.id.recycler_search);
        recyclerView.setVisibility(View.VISIBLE);

        // Search
        materialSearchBar = findViewById(R.id.search_bar);
        materialSearchBar.setHint("Enter your food...");
        // materialSearchBar.setSpeechMode(false);   //no need, becuz we already defined it at XML
        loadSuggest();  // Write function to load Suggest from firebase

        materialSearchBar.setCardViewElevation(10);
        materialSearchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // When user type their text , we will change suggest list
                List<String> suggest = new ArrayList<>();
                for (String search :  suggestList){
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
                // When search Bar is close
                // Restore original adapter
                if (!enabled)
                    recyclerView.setAdapter(adapter);
            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                // When search finish
                // Show result of search adapter
                startSearch(text);
            }

            @Override
            public void onButtonClicked(int buttonCode) {

            }
        });

        // Load all foods
        loadAllFoods();
        recyclerView.setAdapter(adapter);
        startRecyclerViewAnimation();
    }

    private void loadAllFoods() {
        FirebaseRecyclerOptions<Food> options = new FirebaseRecyclerOptions.Builder<Food>().build();
        new FirebaseRecyclerAdapter<Food, FoodViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull FoodViewHolder orderViewHolder,
                                            int position,
                                            @NonNull Food model) {
                orderViewHolder.food_name.setText(model.getName());
                Picasso.get()
                        .load(model.getImage())
                        .into(orderViewHolder.food_image);

                // Click to Share
                orderViewHolder.share_image.setOnClickListener(v -> Picasso.get()
                        .load(model.getImage())
                        .into(target));

                // Add Favorites
                if (localDB.isFavorites(adapter.getRef(position).getKey(), Common.currentUser.getPhone()))
                    orderViewHolder.fav_image.setImageResource(R.drawable.ic_favorite_black_24dp);

                // Click to change state of favorites
                orderViewHolder.fav_image.setOnClickListener(v -> {
                    Favorites favorites = new Favorites();
                    favorites.setFoodId(adapter.getRef(position).getKey());
                    favorites.setFoodName(model.getName());
                    favorites.setFoodDescription(model.getDescription());
                    favorites.setFoodDiscount(model.getDiscount());
                    favorites.setFoodImage(model.getImage());
                    favorites.setFoodMenuId(model.getMenuId());
                    favorites.setUserPhone(Common.currentUser.getPhone());
                    favorites.setFoodPrice(model.getPrice());

                    if (!localDB.isFavorites(adapter.getRef(position).getKey() , Common.currentUser.getPhone())){
                        localDB.addToFavorites(favorites);
                        orderViewHolder.fav_image.setImageResource(R.drawable.ic_favorite_black_24dp);
                        Toast.makeText(SearchActivity.this, "" + model.getName() + " was added to Favorites", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        localDB.removeFromFavorites(adapter.getRef(position).getKey() , Common.currentUser.getPhone());
                        orderViewHolder.fav_image.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                        Toast.makeText(SearchActivity.this, "" + model.getName() + " was removed from Favorites", Toast.LENGTH_SHORT).show();
                    }
                });

                orderViewHolder.setItemClickListener((view, position1, isLongClick) -> {
                    // Start new Activity
                    Intent foodDetail = new Intent(SearchActivity.this, FoodDetailActivity.class);
                    foodDetail.putExtra("FoodId" , adapter.getRef(position1).getKey());   //Send Food Id to new Activity
                    startActivity(foodDetail);
                });
            }

            @NonNull
            @Override
            public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.activity_food_list, parent, false);

                return new FoodViewHolder(view);
            }
        };
    }

    private void startSearch(CharSequence text) {
        FirebaseRecyclerOptions<Food> options = new FirebaseRecyclerOptions.Builder<Food>().build();
        new FirebaseRecyclerAdapter<Food, FoodViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull FoodViewHolder foodViewHolder,
                                            int position,
                                            @NonNull Food model) {
                foodList.orderByChild("name").equalTo(text.toString());
                foodViewHolder.food_name.setText(model.getName());
                Picasso.get()
                        .load(model.getImage())
                        .into(foodViewHolder.food_image);

                foodViewHolder.setItemClickListener((view, position1, isLongClick) -> {
                    // Start new Activity
                    Intent foodDetail = new Intent(SearchActivity.this, FoodDetailActivity.class);
                    // Because now our adapter is searchAdapter , so we need get index on it
                    foodDetail.putExtra("FoodId" , searchAdapter.getRef(position1).getKey());
                    startActivity(foodDetail);
                });
            }

            @NonNull
            @Override
            public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.activity_food_list, parent, false);

                return new FoodViewHolder(view);
            }
        };

        recyclerView.setAdapter(searchAdapter);  //Set adapter ofr Recycler View is Search result
    }

    private void loadSuggest() {
        foodList.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapShot : dataSnapshot.getChildren()){
                    Food item = postSnapShot.getValue(Food.class);
                    assert item != null;
                    suggestList.add(item.getName()); //add name of food to suggest list
                }

                materialSearchBar.setLastSuggestions(suggestList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    @SuppressLint("NotifyDataSetChanged")
    private void startRecyclerViewAnimation() {
        Context context = recyclerView.getContext();
        LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(
                context , R.anim.layout_slide_right);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Set Animation
        recyclerView.setLayoutAnimation(controller);
        Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
        recyclerView.scheduleLayoutAnimation();
    }
}