package kuvaev.mainapp.eatit;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import kuvaev.mainapp.eatit.Common.Common;
import kuvaev.mainapp.eatit.Model.Request;
import kuvaev.mainapp.eatit.ViewHolder.OrderViewHolder;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;

public class OrderStatus extends AppCompatActivity {
    public RecyclerView recyclerView;
    public RecyclerView.LayoutManager layoutManager;

    FirebaseDatabase database;
    DatabaseReference requests;

    FirebaseRecyclerAdapter<Request, OrderViewHolder> adapter;

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

        setContentView(R.layout.activity_order_status);

        // Firebase
        database = FirebaseDatabase.getInstance();
        requests = database.getReference("Requests");

        recyclerView = (RecyclerView)findViewById(R.id.listOrders);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        if (getIntent().getExtras() != null)
            loadOrders(Common.currentUser.getPhone());
        else {
            if(getIntent().getStringExtra("userPhone") == null)
                loadOrders(Common.currentUser.getPhone());
             else
                loadOrders(getIntent().getStringExtra("userPhone"));

        }
    }

    private void loadOrders(String phone) {
        Query getOrderByUser = requests.orderByChild("phone").equalTo(phone);
        FirebaseRecyclerOptions<Request> orderOptions = new FirebaseRecyclerOptions.Builder<Request>()
                .setQuery(getOrderByUser, Request.class)
                .build();

        adapter = new FirebaseRecyclerAdapter<Request, OrderViewHolder>(orderOptions) {
            @Override
            protected void onBindViewHolder(@NonNull OrderViewHolder holder, int position, @NonNull Request model) {
                holder.txtOrderId.setText(adapter.getRef(holder.getBindingAdapterPosition()).getKey());
                holder.txtOrderStatus.setText(Common.convertCodeToStatus(model.getStatus()));
                holder.txtOrderPhone.setText(model.getPhone());
                holder.txtOrderAddress.setText(model.getAddress());
                holder.txtOrderDate.setText(Common.getDate(Long.parseLong(Objects.requireNonNull(adapter.getRef(holder.getBindingAdapterPosition()).getKey()))));
                holder.txtOrderName.setText(model.getName());
                holder.txtOrderPrice.setText(model.getTotal());

                holder.btnDirection.setOnClickListener(v -> {
                    Common.currentKey = adapter.getRef(position).getKey();
                    if (adapter.getItem(position).getStatus().equals("2"))
                        startActivity(new Intent(OrderStatus.this,TrackingOrder.class));
                    else
                        Toast.makeText(OrderStatus.this, "You cannot track this Order!", Toast.LENGTH_SHORT).show();
                });

                holder.btnDeleteOrder.setOnClickListener(v -> {
                    if (adapter.getItem(position).getStatus().equals("0"))
                        deleteOrder(adapter.getRef(position).getKey());
                    else
                        Toast.makeText(OrderStatus.this, "You cannot delete this Order!", Toast.LENGTH_SHORT).show();
                });

                holder.btnConfirmShip.setOnClickListener(v -> {
                    if (adapter.getItem(position).getStatus().equals("03"))
                        ConfirmReceiveOrder(adapter.getRef(position).getKey());
                    else
                        Toast.makeText(OrderStatus.this, "You cannot confirm receive this Order!", Toast.LENGTH_SHORT).show();
                });
            }

            @NonNull
            @Override
            public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.order_layout, parent, false);
                return new OrderViewHolder(itemView);
            }
        };
        adapter.startListening();
        recyclerView.setAdapter(adapter);
    }

    private void ConfirmReceiveOrder(String key) {
        showConfirmReceiveOrder(key);
    }


    private void deleteOrder(final String key) {
        showConfirmDeleteDialog(key);
    }

    private void showConfirmDeleteDialog(final String key) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(OrderStatus.this, androidx.appcompat.R.style.Theme_AppCompat_DayNight_Dialog_Alert);
        alertDialog.setTitle("Confirm Delete?");

        LayoutInflater inflater = this.getLayoutInflater();
        View confirm_delete_layout = inflater.inflate(R.layout.confirm_signout_layout, null);
        alertDialog.setView(confirm_delete_layout);
        alertDialog.setIcon(R.drawable.ic_delete_black_24dp);

        alertDialog.setPositiveButton("DELETE", (dialog, which) -> {
            dialog.dismiss();
            requests.child(key).removeValue();
            Toast.makeText(OrderStatus.this, "Order" + " " +
                    key +
                    " " + "has been deleted", Toast.LENGTH_SHORT).show();

        });

        alertDialog.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    private void showConfirmReceiveOrder(final String key) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(OrderStatus.this, androidx.appcompat.R.style.Theme_AppCompat_DayNight_Dialog_Alert);
        alertDialog.setTitle("Confirm Receive?");

        LayoutInflater inflater = this.getLayoutInflater();
        View confirm_delete_layout = inflater.inflate(R.layout.confirm_signout_layout, null);
        alertDialog.setView(confirm_delete_layout);
        alertDialog.setIcon(R.drawable.ic_local_shipping_black_24dp);

        alertDialog.setPositiveButton("CONFIRM", (dialog, which) -> {
            dialog.dismiss();
            requests.child(key).removeValue();
            Toast.makeText(OrderStatus.this, "Order" + " " +
                    key +
                    " " + "has been confirm received", Toast.LENGTH_SHORT).show();

        });
        alertDialog.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }


    @Override
    protected void onResume() {
        super.onResume();
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }
}
