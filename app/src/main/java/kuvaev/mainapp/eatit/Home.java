package kuvaev.mainapp.eatit;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.andremion.counterfab.CounterFab;
import com.daimajia.slider.library.Animations.DescriptionAnimation;
import com.daimajia.slider.library.SliderLayout;
import com.daimajia.slider.library.SliderTypes.BaseSliderView;
import com.daimajia.slider.library.SliderTypes.TextSliderView;
import com.firebase.ui.database.FirebaseRecyclerAdapter;

import android.text.TextUtils;
import android.view.View;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;

import android.view.MenuItem;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.picasso.Picasso;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.Menu;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.paperdb.Paper;
import kuvaev.mainapp.eatit.Common.Common;
import kuvaev.mainapp.eatit.Database.Database;
import kuvaev.mainapp.eatit.Model.Banner;
import kuvaev.mainapp.eatit.Model.Category;
import kuvaev.mainapp.eatit.Model.Token;
import kuvaev.mainapp.eatit.Model.User;
import kuvaev.mainapp.eatit.ViewHolder.MenuViewHolder;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class Home extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    FirebaseDatabase database;
    DatabaseReference category;

    TextView txtFullName;

    RecyclerView recycler_menu;
    FirebaseRecyclerAdapter<Category , MenuViewHolder> adapter;

    SwipeRefreshLayout swipeRefreshLayout;
    CounterFab fab;

    // Slider
    HashMap<String, String> image_list;
    SliderLayout mSlider;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onStop() {
        super.onStop();
        mSlider.stopAutoCycle();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Note: add this code before setContentView method
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/restaurant_font.otf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        setContentView(R.layout.activity_home);

        // init Paper
        Paper.init(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Menu");
        setSupportActionBar(toolbar);

        // RecyclerView & Load menu
        recycler_menu = (RecyclerView)findViewById(R.id.recycler_menu);
        recycler_menu.setVisibility(View.VISIBLE);

        // Init Swipe Refresh Layout
        swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark
        );
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (Common.isConnectionToInternet(getBaseContext())){
                loadMenu();
                swipeRefreshLayout.setRefreshing(false);
                startRecyclerViewAnimation();
            }
            else {
                Toast.makeText(getBaseContext(), "Please check your connection !!!", Toast.LENGTH_SHORT).show();
            }
        });

        // Default, load for first time
        swipeRefreshLayout.post(() -> {
            if (Common.isConnectionToInternet(getBaseContext())){
                loadMenu();
                swipeRefreshLayout.setRefreshing(false);
                startRecyclerViewAnimation();
            }
            else {
                Toast.makeText(getBaseContext(), "Please check your connection !!!", Toast.LENGTH_SHORT).show();
            }
        });

        // Init Firebase
        database = FirebaseDatabase.getInstance();
        category = database.getReference("Category");

        // Make sure you move this function after database is getInstance()
        if (Common.isConnectionToInternet(getBaseContext())){
            loadMenu();
            recycler_menu.setAdapter(adapter);
            swipeRefreshLayout.setRefreshing(false);
            startRecyclerViewAnimation();
        }
        else {
            Toast.makeText(getBaseContext(), "Please check your connection !!!", Toast.LENGTH_SHORT).show();
            return;
        }

        fab = (CounterFab) findViewById(R.id.fab);
        fab.setColorFilter(getResources().getColor(R.color.colorPrimary));
        fab.setOnClickListener(view -> {
            Intent cartIntent = new Intent(Home.this, CartActivity.class);
            startActivity(cartIntent);
        });
        fab.setCount(new Database(this).getCountCart(Common.currentUser.getPhone()));

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Set name for user
        View headerView = navigationView.getHeaderView(0);
        txtFullName = (TextView)headerView.findViewById(R.id.txtFullName);
        txtFullName.setText(Common.currentUser.getName());

        // Send Token
        updateToken(FirebaseMessaging.getInstance().getToken().toString());

        // Setup Slider
        // Need call this function after you init database FireBase
        setupSlider();
    }

    private void setupSlider() {
        mSlider = (SliderLayout)findViewById(R.id.slider);
        image_list = new HashMap<>();

        final DatabaseReference banners = database.getReference("Banner");
        banners.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapShot : dataSnapshot.getChildren()){
                    Banner banner = postSnapShot.getValue(Banner.class);

                    // We will concat string name and id like
                    // PIZZA@@@01 => And we will use , use PIZZA for show description , 01 for food id to click
                    assert banner != null;
                    image_list.put(banner.getName() + "@@@" + banner.getId() , banner.getImage());
                }

                for (String key : image_list.keySet()){
                    String[] keySplit = key.split("@@@");
                    String nameOfFood = keySplit[0];
                    String idOfFood = keySplit[1];

                    // Create Slider
                    final TextSliderView textSliderView = new TextSliderView(getBaseContext());
                    textSliderView
                            .description(nameOfFood)
                            .image(image_list.get(key))
                            .setScaleType(BaseSliderView.ScaleType.Fit)
                            .setOnSliderClickListener(slider -> {
                                Intent intent = new Intent(Home.this, FoodDetailActivity.class);
                                // We will send food id to FoodFetail
                                intent.putExtras(textSliderView.getBundle());
                                startActivity(intent);
                            });

                    // Add extra bundle
                    textSliderView.bundle(new Bundle());
                    textSliderView.getBundle().putString("FoodId" , idOfFood);

                    mSlider.addSlider(textSliderView);

                    // Remove event after finish
                    banners.removeEventListener(this);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mSlider.setPresetTransformer(SliderLayout.Transformer.Background2Foreground);
        mSlider.setPresetIndicator(SliderLayout.PresetIndicators.Center_Bottom);
        mSlider.setCustomAnimation(new DescriptionAnimation());
        mSlider.setDuration(4000);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void startRecyclerViewAnimation() {
        Context context = recycler_menu.getContext();
        LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(
                context , R.anim.layout_fall_down);

        recycler_menu.setHasFixedSize(true);
        recycler_menu.setLayoutManager(new GridLayoutManager(this , 2));

        // Set Animation
        recycler_menu.setLayoutAnimation(controller);
        Objects.requireNonNull(recycler_menu.getAdapter()).notifyDataSetChanged();
        recycler_menu.scheduleLayoutAnimation();
    }

    private void updateToken(String token) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference tokens = database.getReference("Tokens");
        Token data = new Token(token, false);  // false becuz this token send from Client App
        tokens.child(Common.currentUser.getPhone()).setValue(data);
    }

    private void loadMenu() {
        adapter = new FirebaseRecyclerAdapter<Category, MenuViewHolder>(
                Category.class ,
                R.layout.menu_item ,
                MenuViewHolder.class ,
                category) {
            @Override
            protected void populateViewHolder(MenuViewHolder viewHolder, Category model, int position) {

                viewHolder.txtMenuName.setText(model.getName());
                Picasso.with(getBaseContext())
                        .load(model.getImage())
                        .into(viewHolder.imageView);

                viewHolder.setItemClickListener((view, position1, isLongClick) -> {
                    // Get CategoryId and send to new Activity
                    Intent foodList = new Intent(Home.this, FoodListActivity.class);

                    // Because CategoryId is key , so we just get key of this item
                    foodList.putExtra("CategoryId" , adapter.getRef(position1).getKey());
                    startActivity(foodList);
                });
            }
        };
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onResume() {
        super.onResume();
        fab.setCount(new Database(this).getCountCart(Common.currentUser.getPhone()));

        // Fix click back button from Food and don't see category
        if (adapter != null)
            adapter.notifyDataSetChanged();

        mSlider.startAutoCycle();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_search){
            startActivity(new Intent(Home.this , SearchActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_menu) {
            // Handle the camera action
        }

        else if (id == R.id.nav_cart) {
            if (Common.isConnectionToInternet(this)){
                Intent cartIntent = new Intent(Home.this, CartActivity.class);
                startActivity(cartIntent);
            }
            else
                Toast.makeText(this, "Please check your connection !!!", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_orders) {
            if (Common.isConnectionToInternet(this)){
                Intent orderIntent = new Intent(Home.this, OrderStatusActivity.class);
                orderIntent.putExtra("userPhone" , "-1");
                startActivity(orderIntent);
            } else
                Toast.makeText(this, "Please check your connection !!!", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_change_pwd) {
            if (Common.isConnectionToInternet(this)){
                showChangePasswordDialog();
            }
            else
                Toast.makeText(this, "Please check your connection !!!", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_change_name) {
            if  (Common.isConnectionToInternet(this)){
                showChangeNameDialog();
            }
            else
                Toast.makeText(this, "Please check your connection !!!", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_log_out) {
            if (Common.isConnectionToInternet(this)){
                // Delete Remember user & password
                Paper.book().destroy();

                // Remove Carts
                new Database(getApplicationContext()).cleanCart(Common.currentUser.getPhone());

                // Remove the user's account in FireBase's database (case sign in by firebase account)
                DatabaseReference users = FirebaseDatabase.getInstance().getReference("User");
                users.child(Common.currentUser.getPhone())
                        .removeValue();

                // If sign in by facebook account => will remove it in Facebook account kit
                if (AccountKit.getCurrentAccessToken() != null){
                    AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                        @Override
                        public void onSuccess(Account account) {
                            if (account.getPhoneNumber().toString().equals(Common.currentUser.getPhone()))
                                AccountKit.logOut();
                        }

                        @Override
                        public void onError(AccountKitError accountKitError) {
                            Toast.makeText(Home.this, accountKitError.getErrorType().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

                }

                // Make  Common.currentUser is null
                Common.currentUser = null;

                Intent mainActivity = new Intent(Home.this, MainActivity.class);
                mainActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(mainActivity);
            }
            else
                Toast.makeText(this, "Please check your connection !!!", Toast.LENGTH_SHORT).show();

        } else if (id == R.id.nav_home_address){
            showHomeAddressDialog();
        } else if (id == R.id.nav_settings){
            showSettingsDialog();
        } else if (id == R.id.nav_favorites){
            startActivity(new Intent(Home.this, FavouritesActivity.class));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showHomeAddressDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this)
                .setTitle("CHANGE HOME ADDRESS")
                .setMessage("Please fill all information");

        View view = getLayoutInflater().inflate(R.layout.layout_home_address, null);

        final MaterialEditText edtHomeAddress = (MaterialEditText)view.findViewById(R.id.edtHomeAddress);
        if (Common.currentUser.getHomeAddress() != null && !Common.currentUser.getHomeAddress().isEmpty())
            edtHomeAddress.setText(Common.currentUser.getHomeAddress());

        alertDialog.setView(view);
        alertDialog.setPositiveButton("CHANGE", (dialog, which) -> {
            // set new Home Address
            Common.currentUser.setHomeAddress(Objects.requireNonNull(edtHomeAddress.getText()).toString());

            FirebaseDatabase.getInstance().getReference("User")
                    .child(Common.currentUser.getPhone())
                    .setValue(Common.currentUser)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()){
                            Toast.makeText(Home.this, "Update Home Address successful !", Toast.LENGTH_SHORT)
                                    .show();
                            changeCurrentUser();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(Home.this, e.getMessage(), Toast.LENGTH_SHORT).show());
        });
        alertDialog.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder alertDialog= new AlertDialog.Builder(this)
                .setTitle("SETTINGS")
                .setMessage("Please fill all information");

        View view = getLayoutInflater().inflate(R.layout.layout_setting, null);
        final CheckBox ckb_subscribe_new = (CheckBox)view.findViewById(R.id.ckb_sub_news);

        //Add code remember state of checkbox
        Paper.init(Home.this);

        String isSubscribe = Paper.book().read("sub_new");
        ckb_subscribe_new.setChecked(isSubscribe != null && !TextUtils.isEmpty(isSubscribe) && !isSubscribe.equals("false"));

        alertDialog.setView(view);

        alertDialog.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
            if (ckb_subscribe_new.isChecked()){
                FirebaseMessaging.getInstance().subscribeToTopic(Common.topicName);
                // Write value
                Paper.book().write("sub_new" , "true");
            } else {
                FirebaseMessaging.getInstance().subscribeToTopic(Common.topicName);
                // Write value
                Paper.book().write("sub_new" , "false");
            }
        });

        alertDialog.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    private void showChangeNameDialog() {
        AlertDialog.Builder alertDialog= new AlertDialog.Builder(this)
                .setTitle("UPDATE Name")
                .setMessage("Please fill all information");

        View view = getLayoutInflater().inflate(R.layout.layout_change_name, null);
        final MaterialEditText edtName = (MaterialEditText)view.findViewById(R.id.edtName);
        alertDialog.setView(view);

        alertDialog.setPositiveButton("UPDATE", (dialog, which) -> {
            // use android.app.AlertDialog for SpotsDialog , not from v7 in AlertDialog
            final android.app.AlertDialog waitingDialog = new SpotsDialog(Home.this);
            waitingDialog.show();
            waitingDialog.setMessage("Please waiting...");

            // Update Name
            Map<String , Object> update_name = new HashMap<>();
            update_name.put("name" , Objects.requireNonNull(edtName.getText()).toString());

            FirebaseDatabase.getInstance().getReference("User")
                    .child(Common.currentUser.getPhone())
                    .updateChildren(update_name)
                    .addOnCompleteListener(task -> {
                        waitingDialog.dismiss();

                        if (task.isSuccessful()){
                            Toast.makeText(Home.this, "Name was updated !", Toast.LENGTH_SHORT).show();
                            changeCurrentUser();
                        }
                    })
                    .addOnFailureListener(e -> {
                        waitingDialog.dismiss();
                        Toast.makeText(Home.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });


        alertDialog.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder alertDialog= new AlertDialog.Builder(this)
                .setTitle("CHANGE PASSWORD")
                .setMessage("Please fill all information");

        View view = getLayoutInflater().inflate(R.layout.layout_change_password, null);
        final MaterialEditText edtPassword = (MaterialEditText)view.findViewById(R.id.edtPassword);
        final MaterialEditText edtNewPassword = (MaterialEditText)view.findViewById(R.id.edtNewPassword);
        final MaterialEditText edtRepeatPassword = (MaterialEditText)view.findViewById(R.id.edtRepeatPassword);
        alertDialog.setView(view);

        alertDialog.setPositiveButton("CHANGE", (dialog, which) -> {
            //Change password here
            //use android.app.AlertDialog for SpotsDialog , not from v7 in AlertDialog
            final android.app.AlertDialog waitingDialog = new SpotsDialog(Home.this);
            waitingDialog.show();
            waitingDialog.setMessage("Please waiting...");

            // Check old Password
            if (Objects.requireNonNull(edtPassword.getText()).toString().equals(Common.currentUser.getPassword())){
                // Check new password and repeat password
                if (Objects.requireNonNull(edtNewPassword.getText()).toString().equals(Objects.requireNonNull(edtRepeatPassword.getText()).toString())){
                    Map<String , Object> passwordUpdate = new HashMap<>();
                    passwordUpdate.put("password" , edtNewPassword.getText().toString());

                    // Make update
                    final DatabaseReference user = FirebaseDatabase.getInstance().getReference("User");
                    user.child(Common.currentUser.getPhone())
                            .updateChildren(passwordUpdate)
                            .addOnCompleteListener(task -> {
                                changeCurrentUser();

                                waitingDialog.dismiss();
                                Toast.makeText(Home.this, "Password was updated !", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(Home.this, e.getMessage(), Toast.LENGTH_SHORT).show());
                } else {
                    waitingDialog.dismiss();
                    Toast.makeText(Home.this, "New password doesn't match", Toast.LENGTH_SHORT).show();
                }
            } else {
                waitingDialog.dismiss();
                Toast.makeText(Home.this, "Wrong old password !", Toast.LENGTH_SHORT).show();
            }
        });

        alertDialog.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    private void changeCurrentUser() {
        if (Common.isConnectionToInternet(Home.this)) {
            DatabaseReference user = database.getReference("User");
            user.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    User user = dataSnapshot.child(Common.currentUser.getPhone()).getValue(User.class);
                    assert user != null;
                    user.setPhone(Common.currentUser.getPhone()); // set Phone

                    Common.currentUser = user;
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        } else {
            Toast.makeText(Home.this, "Plaese check your connection !!!", Toast.LENGTH_SHORT).show();
        }
    }
}