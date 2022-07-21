package kuvaev.mainapp.eatit;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.places.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import kuvaev.mainapp.eatit.Common.Common;
import kuvaev.mainapp.eatit.Common.PaypalConfig;
import kuvaev.mainapp.eatit.Database.Database;
import kuvaev.mainapp.eatit.Helper.RecyclerItemTouchHelper;
import kuvaev.mainapp.eatit.Interface.RecyclerItemTouchHelperListener;
import kuvaev.mainapp.eatit.Model.DataMessage;
import kuvaev.mainapp.eatit.Model.Order;
import kuvaev.mainapp.eatit.Model.Request;
import kuvaev.mainapp.eatit.Model.Token;
import kuvaev.mainapp.eatit.Model.User;
import kuvaev.mainapp.eatit.Remote.APIService;
import kuvaev.mainapp.eatit.Remote.GoogleServiceAction;
import kuvaev.mainapp.eatit.ViewHolder.CartAdapter;
import kuvaev.mainapp.eatit.ViewHolder.CartViewHolder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class CartActivity extends AppCompatActivity implements RecyclerItemTouchHelperListener {
    private static final int PAYPAL_REQUEST_CODE = 9999;
    private static final int CODE_REQUEST_PERMISSION_lOCATION = 1000;
    RecyclerView recyclerView;
    Button btnPlace;
    public TextView txtTotalPrice;

    RelativeLayout rootLayout;

    FirebaseDatabase database;
    DatabaseReference requests;

    List<Order> cart = new ArrayList<>();
    CartAdapter adapter;

    APIService mService;

    // Google API
    Place shippingAddress;

    // Location
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;
    LocationRequest locationRequest;
    LocationManager manager;
    Location mLastLocation;

    // Declare Google Map API Retrofit
    GoogleServiceAction mGoogleMapService;

    static PayPalConfiguration config;

    String address, comment;

    // Press Ctrl + O
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

        setContentView(R.layout.activity_cart);

        // PayPal payment
        config = new PayPalConfiguration()
                .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX) //use SendBox because we test , change it late if you going to production
                .clientId(PaypalConfig.PAYPAL_CLIENT_ID);

        // Init Google Map API
        mGoogleMapService = Common.getGoogleMapAPI();

        // init rootLayout
        rootLayout = (RelativeLayout) findViewById(R.id.rootLayout);

        // Init PayPal
        Intent intent = new Intent(this, PayPalService.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        startActivity(intent);

        // Init Service
        mService = Common.getFCMService();

        // FireBase
        database = FirebaseDatabase.getInstance();
        requests = database.getReference("Requests");

        // Init
        btnPlace = (Button) findViewById(R.id.btnPlaceOrder);
        txtTotalPrice = (TextView) findViewById(R.id.total);

        recyclerView = (RecyclerView) findViewById(R.id.listCart);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Swipe to delete item
        // Very important put code ItemTouchHelper....  AFTER init RecyclerView
        ItemTouchHelper.SimpleCallback itemTouchHelperCallBack = new RecyclerItemTouchHelper(0,
                ItemTouchHelper.LEFT,
                this);
        new ItemTouchHelper(itemTouchHelperCallBack).attachToRecyclerView(recyclerView);

        btnPlace.setOnClickListener(v -> {
            if (cart.size() > 0)
                showAlertDialog();
            else
                Toast.makeText(CartActivity.this, "Your cart is empty !!!", Toast.LENGTH_SHORT).show();
        });

        loadListFood();

        buildLocationRequest();
        buildLocationCallBack();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    private void showAlertDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this)
                .setTitle("One more step!")
                .setMessage("Enter your address: ");

        View order_address_comment = getLayoutInflater().inflate(R.layout.layout_order_address_comment, null);

        // final MaterialEditText edtAddress = (MaterialEditText)order_address_comment.findViewById(R.id.edtAddress);
        final AutocompleteSupportFragment  edtAddress = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);//Hide search icon before fragment
        assert edtAddress != null;
        edtAddress.requireView().findViewById(R.id.place_autocomplete_search_button).setVisibility(View.GONE);
        // Set Hint for Autocomplete Edit Text
        ((EditText) edtAddress.requireView().findViewById(R.id.place_autocomplete_search_input))
                .setHint("Enter your address...");
        // Set Text size
        ((EditText) edtAddress.requireView().findViewById(R.id.place_autocomplete_search_input))
                .setTextSize(14);
        //Get Address from Place Autocomplete
        edtAddress.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull com.google.android.libraries.places.api.model.Place place) {
                shippingAddress = (Place) place;
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.e("ERROR", status.getStatusMessage());
            }
        });

        final MaterialEditText edtComment = (MaterialEditText) order_address_comment.findViewById(R.id.edtComment);
        final RadioButton rdiShipToAddress = (RadioButton) order_address_comment.findViewById(R.id.rdiShipToAddress);
        final RadioButton rdiHomeAddress = (RadioButton) order_address_comment.findViewById(R.id.rdiHomeAddress);

        final RadioButton rdiCOD = (RadioButton) order_address_comment.findViewById(R.id.rdiCOD);
        final RadioButton rdiPaypal = (RadioButton) order_address_comment.findViewById(R.id.rdiPaypal);
        final RadioButton rdiBalance = (RadioButton) order_address_comment.findViewById(R.id.rdiEatItBalance);

        // Event Home address Radio Button
        rdiHomeAddress.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked){
                if (TextUtils.isEmpty(Common.currentUser.getHomeAddress()) ||
                        Common.currentUser.getHomeAddress() == null)
                    Toast.makeText(CartActivity.this, "Please update your Home Address", Toast.LENGTH_SHORT).show();

                else{
                    address = Common.currentUser.getHomeAddress();
                    // Then set this address to edtAddress
                    ((EditText) edtAddress.requireView().findViewById(R.id.place_autocomplete_search_input))
                            .setText(address);
                }
            }
        });

        // Event Ship to this address Radio Button
        rdiShipToAddress.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Ship to this address feature
            if (isChecked) {  // isChecked == true
                buildLocationRequest();
                buildLocationCallBack();

                if (mLastLocation != null){

                    mGoogleMapService.getAddressName(
                            // Copy this link and put LatLng and paste it in Google search , you will see JSON file
                            String.format(Locale.US, "............................",
                                    mLastLocation.getLatitude(),
                                    mLastLocation.getLongitude()) ,

                            Common.MAP_API_KEY

                    ).enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            // If fetch API ok
                            try {
                                JSONObject jsonObject = new JSONObject(response.body());
                                JSONArray resultArray = jsonObject.getJSONArray("results");
                                JSONObject firstObject = resultArray.getJSONObject(0);

                                if (firstObject != null)
                                    address = firstObject.getString("formatted_address");
                                else
                                    address = "";

                                // Then set this address to edtAddress
                                ((EditText) edtAddress.requireView().findViewById(R.id.place_autocomplete_search_input))
                                        .setText(address);
                            }
                            catch (JSONException e) {
                                e.printStackTrace();
                                Toast.makeText(CartActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(CartActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else
                {
                    address = "";
                    Toast.makeText(CartActivity.this, "Location is NULL", Toast.LENGTH_SHORT).show();
                }
            }
        });

        alertDialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);
        alertDialog.setView(order_address_comment);
        alertDialog.setPositiveButton("YES", (dialog, which) -> {
            comment = Objects.requireNonNull(edtComment.getText()).toString();
            // Check Payment
            if (!rdiCOD.isChecked() && !rdiPaypal.isChecked() && !rdiBalance.isChecked()){  // If both COD and paypal and Balance is not checked
                Toast.makeText(CartActivity.this, "Please select Payment options", Toast.LENGTH_SHORT).show();
                // Remove Fragment (Google Places API)
                getSupportFragmentManager()
                        .beginTransaction()
                        .remove(Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.place_autocomplete_fragment)))
                        .commit();

                return;
            }
            else if (rdiPaypal.isChecked()){
                // If Paypal run successfully , we must CUT code in (CODE 1) and paste in onActivityResult()
                usingPaypalForPayment();
            }
            else if (rdiCOD.isChecked()){
                usingCODForPayment();
                // for we don't complete this method , where we will see add two request in Firebase's database
                // If Paypal run successfully , and cut and paste in usingPaypalForPayment() , must remove this (return)
                return;
            }
            else if (rdiBalance.isChecked()){
                usingEatItBalanceForPayment();
                // for we don't complete this method , where we will see add two request in Firebase's database
                // If Paypal run successfully , and cut and paste in usingPaypalForPayment() , must remove this (return)
                return;
            }

            // Add check Delivery condition here
            // If user select address from Place fragment , just use it
            // If use Ship to this address , get Address from location and use it
            // If use select Home address , get Home address from Retrofit and use it
            if (!rdiHomeAddress.isChecked() && !rdiShipToAddress.isChecked()){
                //if both radio is not selected ->
                if (shippingAddress != null && !Objects.requireNonNull(shippingAddress.getAddress()).toString().isEmpty()){
                    address = shippingAddress.getAddress().toString();
                }
                else {
                    Toast.makeText(CartActivity.this, "Please enter address or select options address " +
                            "OR your placed isn't supported by Google Places", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // else if (rdiShipToThis Address) coded above
            if (TextUtils.isEmpty(address)){  // this mean redioButton Ship to this address OR (Enter your address) was clicked and result is null or empty
                address = "";
            }

            // Create new Request  (copy start CODE 1)
            Request request = new Request(
                    Common.currentUser.getPhone(),
                    Common.currentUser.getName(),
                    address,
                    txtTotalPrice.getText().toString(),
                    "0",
                    comment,
                    "paypal",
                    "approved",   // we assume the client pay the order, so save "approved"
                    // by using paypal SDK, add this line instance of "approved"
                    // jsonObject.getJSONObject("response").getString("state")
                    (mLastLocation != null)?
                            String.format("%s,%s", mLastLocation.getLatitude(), mLastLocation.getLongitude()) : "36.192984,37.117703",
                    cart
            );
            // Submit to FireBase
            // We will using  System.currentMilli  to Key
            String order_number = String.valueOf(System.currentTimeMillis());
            requests.child(order_number)
                    .setValue(request);
            // Delete Cart
            new Database(getBaseContext()).cleanCart(Common.currentUser.getPhone());

            sendNotificationOrder(order_number);

            Toast.makeText(CartActivity.this, "Thank you , order placed", Toast.LENGTH_SHORT).show();
            finish();

            //Remove Fragment (Google Places API)
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.place_autocomplete_fragment)))
                    .commit();
        });

        alertDialog.setNegativeButton("NO", (dialog, which) -> {
            dialog.dismiss();
            // Remove Fragment (Google Places API)
            // Basically you're calling hide(), remove(), etc., with a null value
            // So we add this condition to be sure that (shippingAddress) is't null
            // if (shippingAddress != null)
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.place_autocomplete_fragment)))
                    .commit();
        });

        alertDialog.setCancelable(false); // Fix crush inFlate fragment
        alertDialog.show();
    }

    private void buildLocationCallBack() {
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                mLastLocation = locationResult.getLastLocation();
            }
        };
    }

    @SuppressLint("RestrictedApi")
    private void buildLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(Priority.PRIORITY_LOW_POWER);
        locationRequest.setSmallestDisplacement(10f);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
    }

    private void usingEatItBalanceForPayment() {
        double amount = 0;

        // First, we will get total price from txtTotalPrice
        try {
            amount = Common.formatCurrent(txtTotalPrice.getText().toString() , Locale.US).doubleValue();
        }
        catch (ParseException e) {
            e.printStackTrace();
        }

        // After receive total price of this order , just compare with user balance
        if (Double.parseDouble(Common.currentUser.getBalance().toString()) >= amount){
            // here we must paste Code that save order in FireBase's database (i mean CODE 1)
            // with change paymentMethod & paymentState & LatLng
            Request request = new Request(
                    Common.currentUser.getPhone(),
                    Common.currentUser.getName(),
                    address,
                    txtTotalPrice.getText().toString(),
                    "0",
                    comment,
                    "EatIt Balance",
                    "Paid",
                    (mLastLocation != null)?
                            String.format("%s,%s", mLastLocation.getLatitude(), mLastLocation.getLongitude()) : "36.192984,37.117703",  //Coordinates when user order
                    cart
            );
            // Submit to FireBase
            // We will using  System.currentMilli  to Key
            final String order_number = String.valueOf(System.currentTimeMillis());
            requests.child(order_number)
                    .setValue(request);
            // Delete Cart
            new Database(getBaseContext()).cleanCart(Common.currentUser.getPhone());

            // update balance
            double balance = Double.parseDouble(Common.currentUser.getBalance().toString()) - amount;
            Map<String , Object> update_balance = new HashMap<>();
            update_balance.put("balance" , balance);

            FirebaseDatabase.getInstance().getReference("User")
                    .child(Common.currentUser.getPhone())
                    .updateChildren(update_balance)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()){
                            // Refresh user
                            FirebaseDatabase.getInstance().getReference("User")
                                    .child(Common.currentUser.getPhone())
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            Common.currentUser = dataSnapshot.getValue(User.class);
                                            // Send Order to server
                                            sendNotificationOrder(order_number);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                        }
                    });


            Toast.makeText(CartActivity.this, "Thank you , order placed", Toast.LENGTH_SHORT).show();
            finish();

            //Remove Fragment (Google Places API)
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.place_autocomplete_fragment)))
                    .commit();
        }
        else {
            Toast.makeText(CartActivity.this, "Your balance not enough , Please choose other payment", Toast.LENGTH_SHORT).show();
        }
    }

    private void usingCODForPayment() {
        Request request = new Request(
                Common.currentUser.getPhone(),
                Common.currentUser.getName(),
                address,
                txtTotalPrice.getText().toString(),
                "0",
                comment,
                "COD",
                "Unpaid",
                (mLastLocation != null)?
                        String.format("%s,%s", mLastLocation.getLatitude(), mLastLocation.getLongitude()) : "36.192984,37.117703",  //Coordinates when user order
                cart
        );
        // Submit to FireBase
        // We will using  System.currentMilli to Key
        String order_number = String.valueOf(System.currentTimeMillis());
        requests.child(order_number)
                .setValue(request);
        // Delete Cart
        new Database(getBaseContext()).cleanCart(Common.currentUser.getPhone());

        sendNotificationOrder(order_number);

        Toast.makeText(CartActivity.this, "Thank you , order placed", Toast.LENGTH_SHORT).show();
        finish();

        //Remove Fragment (Google Places API)
        getSupportFragmentManager()
                .beginTransaction()
                .remove(Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.place_autocomplete_fragment)))
                .commit();
    }

    private void usingPaypalForPayment() {
        // Show PayPal to payment
        String formatAmount = txtTotalPrice.getText().toString()
                .replace("$", "")
                .replace(",", "");

        PayPalPayment payPalPayment = new PayPalPayment(new BigDecimal(formatAmount),
                "USD",
                "Eat It App Order",
                PayPalPayment.PAYMENT_INTENT_SALE);

        //First , get address and comment from Alert Dialog (we can't use paypal in syria , so i commented last line)
        Intent intent = new Intent(getApplicationContext(), PaymentActivity.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payPalPayment);
        startActivityForResult(intent, PAYPAL_REQUEST_CODE);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initMyLocation() {
        manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (Build.VERSION.SDK_INT >= 23)
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, CODE_REQUEST_PERMISSION_lOCATION);

            else{
                mLastLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (mLastLocation == null)
                    mLastLocation = manager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            }
        }
    }

    private void sendNotificationOrder(final String order_number) {
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query data = tokens.orderByChild("serverToken").equalTo(true);  //get all node with isServerToken is true
        data.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapShot : dataSnapshot.getChildren()){
                    Token serverToken = postSnapShot.getValue(Token.class);

                    // Create raw payload to send
                    Map<String , String> dataSend = new HashMap<>();
                    dataSend.put("title" , "ABD");
                    dataSend.put("message" , "You have new order " + order_number);
                    assert serverToken != null;
                    DataMessage dataMessage = new DataMessage(serverToken.getToken() , dataSend);

                    mService.sendNotification(dataMessage)
                            .enqueue(new Callback<kuvaev.mainapp.eatit.Model.Response>() {
                                @Override
                                public void onResponse(Call<kuvaev.mainapp.eatit.Model.Response> call, Response<kuvaev.mainapp.eatit.Model.Response> response) {
                                    //Only true when get result
                                    if (response.code() == 200){
                                        if (response.body().success == 1){
                                            Toast.makeText(CartActivity.this, "Thank you , order place", Toast.LENGTH_SHORT).show();
                                            finish();
                                        }
                                        else
                                            Toast.makeText(CartActivity.this, "Failed !!!", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<kuvaev.mainapp.eatit.Model.Response> call, Throwable t) {
                                    Log.e("ERROR", t.getMessage());
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadListFood() {
        cart = new Database(this).getCarts(Common.currentUser.getPhone());
        adapter = new CartAdapter(cart , this);
        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);

        //Calculate total price
        int total = 0;

        for (Order order : cart)
            total += (Integer.parseInt(order.getPrice()) * Integer.parseInt(order.getQuantity()));

        Locale locale = new Locale("en" , "US");
        NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);

        txtTotalPrice.setText(fmt.format(total));
    }

    private void deleteCart(int position) {
        // We will remove item at List<Order> by position
        cart.remove(position);
        // After that, we will delete all old data from SQLite
        new Database(this).cleanCart(Common.currentUser.getPhone());
        // And final, we will update new data from List<Order> to SQLite
        for (Order item : cart)
            new Database(this).addToCart(item);

        // Refresh
        loadListFood();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getTitle().equals(Common.DELETE))
            deleteCart(item.getOrder());

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PAYPAL_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                PaymentConfirmation confirmation = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
                if (confirmation != null) {
                    try {
                        String paymentDetail = confirmation.toJSONObject().toString(4);
                        // here we must paste Code that save order in FireBase's database (i mean CODE 1)
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else if (resultCode == RESULT_CANCELED)
                Toast.makeText(this, "Payment cancel", Toast.LENGTH_SHORT).show();

            else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID)
                Toast.makeText(this, "Invalid payment", Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODE_REQUEST_PERMISSION_lOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)
                initMyLocation();
            else
                Toast.makeText(this, "You can't use the location service", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof CartViewHolder){
            String name = ((CartAdapter) Objects.requireNonNull(recyclerView.getAdapter())).getItem(viewHolder.getBindingAdapterPosition()).getProductName();

            final Order deleteItem = ((CartAdapter)recyclerView.getAdapter()).getItem(viewHolder.getBindingAdapterPosition());

            final int deleteIndex = viewHolder.getBindingAdapterPosition();
            adapter.removeItem(deleteIndex);
            new Database(getBaseContext()).removeFromCart(deleteItem.getProductId() , Common.currentUser.getPhone());

            //Update txtTotalPrice
            //Calculate total price
            int total = 0;
            List<Order> orders = new Database(getBaseContext()).getCarts(Common.currentUser.getPhone());

            for (Order item : orders)
                total += (Integer.parseInt(item.getPrice()) * Integer.parseInt(item.getQuantity()));

            Locale locale = new Locale("en" , "US");
            NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);

            txtTotalPrice.setText(fmt.format(total));

            // Make Snackbar
            Snackbar snackbar = Snackbar.make(rootLayout , name + " is removed from cart !" , Snackbar.LENGTH_LONG );
            snackbar.setAction("UNDO", v -> {
                adapter.restoreItem(deleteItem ,deleteIndex);
                new Database(getBaseContext()).addToCart(deleteItem);

                // Update txtTotalPrice
                // Calculate total price
                int total1 = 0;
                List<Order> orders1 = new Database(getBaseContext()).getCarts(Common.currentUser.getPhone());

                for (Order item : orders1)
                    total1 += (Integer.parseInt(item.getPrice()) * Integer.parseInt(item.getQuantity()));

                Locale locale1 = new Locale("en" , "US");
                NumberFormat fmt1 = NumberFormat.getCurrencyInstance(locale1);

                txtTotalPrice.setText(fmt1.format(total1));
            });

            snackbar.setActionTextColor(Color.YELLOW);
            snackbar.show();
        }
    }
}