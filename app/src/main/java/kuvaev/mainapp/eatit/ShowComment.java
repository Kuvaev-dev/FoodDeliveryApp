package kuvaev.mainapp.eatit;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.squareup.picasso.Picasso;

import kuvaev.mainapp.eatit.Common.Common;
import kuvaev.mainapp.eatit.Common.NumberOfFood;
import kuvaev.mainapp.eatit.Model.Rating;
import kuvaev.mainapp.eatit.ViewHolder.ShowCommentViewHolder;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class ShowComment extends AppCompatActivity {
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    FirebaseDatabase database;
    DatabaseReference ratingDb;

    String foodId="";

    SwipeRefreshLayout swipeRefreshLayout;

    FirebaseRecyclerAdapter<Rating, ShowCommentViewHolder> adapter;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter !=null)
            adapter.stopListening();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/restaurant_font.otf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        setContentView(R.layout.activity_show_comment);

        // Firebase
        database = FirebaseDatabase.getInstance();
        ratingDb = database.getReference("Rating");

        recyclerView = findViewById(R.id.recycler_comment);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // swipe layout
        swipeRefreshLayout = findViewById(R.id.swipe_layout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (getIntent() != null)
                foodId = getIntent().getStringExtra(Common.INTENT_FOOD_ID);
            if (!foodId.isEmpty()) {
                // create request query
                Query query = ratingDb.orderByChild("foodId").equalTo(foodId);
                FirebaseRecyclerOptions<Rating> options = new FirebaseRecyclerOptions.Builder<Rating>()
                        .setQuery(query, Rating.class)
                        .build();

                adapter = new FirebaseRecyclerAdapter<Rating, ShowCommentViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull ShowCommentViewHolder holder, int position, @NonNull Rating model) {
                        holder.ratingBar.setRating(Float.parseFloat(model.getRateValue()));
                        holder.txtComment.setText(model.getComment());
                        holder.txtUserPhone.setText(model.getUserPhone());
                        holder.txtFoodName.setText(NumberOfFood.convertIdToName(model.getFoodId()));
                        Picasso.get().load(model.getImage()).into(holder.commentImage);
                    }

                    @NonNull
                    @Override
                    public ShowCommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.show_comment_layout, parent, false);
                        return new ShowCommentViewHolder(view);
                    }
                };

                loadComment();
            }
        });

        // Thread to load comment on first launch
        swipeRefreshLayout.post(() -> {
            swipeRefreshLayout.setRefreshing(true);

            if (getIntent() != null)
                foodId = getIntent().getStringExtra(Common.INTENT_FOOD_ID);
            if (!foodId.isEmpty()) {
                // create request query
                Query query = ratingDb.orderByChild("foodId").equalTo(foodId);
                FirebaseRecyclerOptions<Rating> options = new FirebaseRecyclerOptions.Builder<Rating>()
                        .setQuery(query, Rating.class)
                        .build();

                adapter = new FirebaseRecyclerAdapter<Rating, ShowCommentViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull ShowCommentViewHolder holder, int position, @NonNull Rating model) {
                        holder.ratingBar.setRating(Float.parseFloat(model.getRateValue()));
                        holder.txtComment.setText(model.getComment());
                        holder.txtUserPhone.setText(model.getUserPhone());
                        holder.txtFoodName.setText(NumberOfFood.convertIdToName(model.getFoodId()));
                        Picasso.get().load(model.getImage()).into(holder.commentImage);
                    }

                    @NonNull
                    @Override
                    public ShowCommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.show_comment_layout, parent, false);
                        return new ShowCommentViewHolder(view);
                    }
                };

                loadComment();
            }
        });
    }

    private void loadComment() {
        adapter.startListening();

        recyclerView.setAdapter(adapter);
        swipeRefreshLayout.setRefreshing(false);
    }
}
