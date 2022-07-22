package kuvaev.mainapp.eatit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

import kuvaev.mainapp.eatit.Common.Common;
import kuvaev.mainapp.eatit.Model.Rating;
import kuvaev.mainapp.eatit.ViewHolder.ShowCommentViewHolder;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class ShowCommentActivity extends AppCompatActivity {
    RecyclerView recyclerView;

    FirebaseDatabase database;
    DatabaseReference ratingTbl;

    SwipeRefreshLayout mSwipeRefreshLayout;
    FirebaseRecyclerAdapter<Rating, ShowCommentViewHolder> adapter;

    String foodId = "";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/restaurant_font.otf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        setContentView(R.layout.activity_show_comment);

        //FireBase
        database = FirebaseDatabase.getInstance();
        ratingTbl = database.getReference("Rating");

        recyclerView = findViewById(R.id.recyclerComment);
        recyclerView.setVisibility(View.VISIBLE);

        //Swipe Refresh Layout
        mSwipeRefreshLayout = findViewById(R.id.swipe_layout);
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            if (getIntent().getExtras() != null)
                foodId = getIntent().getStringExtra(Common.INTENT_FOOD_ID);
            if (!foodId.isEmpty()){
                FirebaseRecyclerOptions<Rating> options = new FirebaseRecyclerOptions.Builder<Rating>().build();
                new FirebaseRecyclerAdapter<Rating, ShowCommentViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull ShowCommentViewHolder showCommentViewHolder,
                                                    int position,
                                                    @NonNull Rating model) {
                        ratingTbl.orderByChild("foodId").equalTo(foodId);
                        showCommentViewHolder.ratingBar.setRating(Float.parseFloat(model.getRateValue()));
                        showCommentViewHolder.txtUserPhone.setText(model.getUserPhone());
                        showCommentViewHolder.txtComment.setText(model.getComment());
                    }

                    @NonNull
                    @Override
                    public ShowCommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.activity_show_comment, parent, false);

                        return new ShowCommentViewHolder(view);
                    }
                };

                recyclerView.setAdapter(adapter);
                mSwipeRefreshLayout.setRefreshing(false);
                startAnimation();
            }
        });

        //Thread to load comment on first lunch
        mSwipeRefreshLayout.post(() -> {
            mSwipeRefreshLayout.setRefreshing(true);

            if (getIntent().getExtras() != null)
                foodId = getIntent().getStringExtra(Common.INTENT_FOOD_ID);

            if (!foodId.isEmpty()){
                FirebaseRecyclerOptions<Rating> options = new FirebaseRecyclerOptions.Builder<Rating>().build();
                new FirebaseRecyclerAdapter<Rating, ShowCommentViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull ShowCommentViewHolder showCommentViewHolder,
                                                    int position,
                                                    @NonNull Rating model) {
                        ratingTbl.orderByChild("foodId").equalTo(foodId);
                        showCommentViewHolder.ratingBar.setRating(Float.parseFloat(model.getRateValue()));
                        showCommentViewHolder.txtUserPhone.setText(model.getUserPhone());
                        showCommentViewHolder.txtComment.setText(model.getComment());
                    }

                    @NonNull
                    @Override
                    public ShowCommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.activity_show_comment, parent, false);

                        return new ShowCommentViewHolder(view);
                    }
                };

                recyclerView.setAdapter(adapter);
                mSwipeRefreshLayout.setRefreshing(false);
                startAnimation();
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void startAnimation() {
        Context context = recyclerView.getContext();
        LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(context , R.anim.layout_slide_right);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        recyclerView.setLayoutAnimation(controller);
        Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
        recyclerView.scheduleLayoutAnimation();
    }
}