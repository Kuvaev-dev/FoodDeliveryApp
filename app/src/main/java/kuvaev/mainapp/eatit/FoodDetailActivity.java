package kuvaev.mainapp.eatit;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.andremion.counterfab.CounterFab;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.stepstone.apprating.listener.RatingDialogListener;
import com.stepstone.apprating.AppRatingDialog;

import java.util.Arrays;

import kuvaev.mainapp.eatit.Common.Common;
import kuvaev.mainapp.eatit.Database.Database;
import kuvaev.mainapp.eatit.Model.Food;
import kuvaev.mainapp.eatit.Model.Order;
import kuvaev.mainapp.eatit.Model.Rating;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class FoodDetailActivity extends AppCompatActivity implements RatingDialogListener{
    TextView food_name , food_price , food_description;
    ImageView food_image;
    CollapsingToolbarLayout collapsingToolbarLayout;
    FloatingActionButton btnRating;
    CounterFab btnCart;
    ElegantNumberButton numberButton;
    RatingBar ratingBar;
    Button btnShowComment;

    String foodId = "";

    FirebaseDatabase database;
    DatabaseReference foods;
    DatabaseReference ratingTbl;

    Food currentFood;

    // Set value to RatingBar
    int count = 0, sum = 0;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Note: add this code before setContentView method
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/restaurant_font.otf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        setContentView(R.layout.activity_food_detail);

        //FireBase
        database = FirebaseDatabase.getInstance();
        foods = database.getReference("Foods");
        ratingTbl = database.getReference("Rating");

        //Init view
        numberButton = (ElegantNumberButton)findViewById(R.id.number_button);
        btnCart = (CounterFab) findViewById(R.id.btnCart);
        btnRating = (FloatingActionButton)findViewById(R.id.btn_rating);
        food_name = (TextView)findViewById(R.id.food_name);
        food_price = (TextView)findViewById(R.id.food_price);
        food_description = (TextView)findViewById(R.id.food_description);
        food_image = (ImageView)findViewById(R.id.img_food);
        ratingBar = (RatingBar)findViewById(R.id.ratingBar);
        btnShowComment = (Button)findViewById(R.id.btnShowComment);

        collapsingToolbarLayout = (CollapsingToolbarLayout)findViewById(R.id.collapsing);
        collapsingToolbarLayout.setExpandedTitleTextAppearance(R.style.ExpandedAppbar);
        collapsingToolbarLayout.setCollapsedTitleTextAppearance(R.style.CollapsedAppbar);

        btnShowComment.setOnClickListener(v -> {
            Intent intent = new Intent(FoodDetailActivity.this, ShowCommentActivity.class);
            intent.putExtra(Common.INTENT_FOOD_ID, foodId);
            startActivity(intent);
        });

        btnRating.setOnClickListener(v -> showRatingDialog());

        // Button Cat Clicked
        btnCart.setOnClickListener(v -> {
            boolean isExists = new Database(getApplicationContext()).checkFoodExists(foodId , Common.currentUser.getPhone());
            if (!isExists){
                new Database(getBaseContext()).addToCart(new Order(
                        Common.currentUser.getPhone(),
                        foodId,
                        currentFood.getName(),
                        numberButton.getNumber(),
                        currentFood.getPrice(),
                        currentFood.getDiscount(),
                        currentFood.getImage()
                ));
            } else {
                new Database(getApplicationContext()).increaseCart(Common.currentUser.getPhone(), foodId);
            }

            btnCart.setCount(new Database(getApplicationContext()).getCountCart(Common.currentUser.getPhone()));
            Toast.makeText(FoodDetailActivity.this, "Added to Cart", Toast.LENGTH_SHORT).show();
        });

        btnCart.setCount(new Database(this).getCountCart(Common.currentUser.getPhone()));

        // Get Food Id from Intent
        if (getIntent() != null)
            foodId = getIntent().getStringExtra("FoodId");

        if (!foodId.isEmpty()){
            if (Common.isConnectionToInternet(this)){
                getDetailFood(foodId);
                getRatingFood(foodId);
            } else {
                Toast.makeText(this, "Please check your connection !!!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getRatingFood(final String foodId) {
        // Here we have some mistakes
        Query foodRating = ratingTbl.orderByChild("foodId").equalTo(foodId);

        foodRating.addValueEventListener(new ValueEventListener() {
            int count = 0, sum = 0;
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()){
                    Rating item = postSnapshot.getValue(Rating.class);
                    assert item != null;
                    sum += Integer.parseInt(item.getRateValue());
                    count++;
                }

                if (count != 0){
                    float average = sum / count;
                    ratingBar.setRating(average);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void showRatingDialog() {
        new AppRatingDialog.Builder()
                .setPositiveButtonText("Submit")
                .setNegativeButtonText("Cancel")
                .setNoteDescriptions(Arrays.asList("Very Bad", "Not Good", "Quite Ok", "Very Good", "Excellent"))
                .setDefaultRating(1)
                .setTitle("Rate this food")
                .setDescription("Please select some stars and give your feedback")
                .setTitleTextColor(R.color.colorPrimary)
                .setDescriptionTextColor(R.color.colorPrimary)
                .setHint("Please write your comment here...")
                .setHintTextColor(R.color.colorAccent)
                .setCommentTextColor(android.R.color.white)
                .setCommentBackgroundColor(R.color.colorPrimaryDark)
                .setWindowAnimation(R.style.RatingDialogFadeAnim)
                .create(FoodDetailActivity.this)
                .show();
    }

    private void getDetailFood(final String foodId) {
        foods.child(foodId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentFood = dataSnapshot.getValue(Food.class);

                // Set Image
                Picasso.with(getBaseContext())
                        .load(currentFood.getImage())
                        .into(food_image);

                collapsingToolbarLayout.setTitle(currentFood.getName());

                food_price.setText(currentFood.getPrice());
                food_name.setText(currentFood.getName());
                food_description.setText(currentFood.getDescription());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onPositiveButtonClicked(int value, String comments) {

        //Get Rating and upload to FireBase
        final Rating rating = new Rating(Common.currentUser.getPhone(),
                foodId,
                String.valueOf(value),
                comments);

        //Fix user can rate multiple times
        ratingTbl.push().setValue(rating)
                .addOnCompleteListener(task -> Toast.makeText(FoodDetailActivity.this, "Thank you for submit rating !!!", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onNegativeButtonClicked() {

    }

    @Override
    public void onNeutralButtonClicked() {

    }
}