package kuvaev.mainapp.eatit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import kuvaev.mainapp.eatit.Common.Common;
import kuvaev.mainapp.eatit.Interface.ItemClickListener;
import kuvaev.mainapp.eatit.Model.Request;
import kuvaev.mainapp.eatit.ViewHolder.OrderViewHolder;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class OrderStatusActivity extends AppCompatActivity {
    public RecyclerView recyclerView;

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

        // Note: add this code before setContentView method
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/restaurant_font.otf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        setContentView(R.layout.activity_order_status);

        // FireBase
        database = FirebaseDatabase.getInstance();
        requests = database.getReference("Requests");

        recyclerView = (RecyclerView)findViewById(R.id.listOrders);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // If we start OrderStatus activity from Home Activity
        // We will not put any exta , so we just loadOrder by phone  from Common
        if (Common.isConnectionToInternet(this)){
            if (getIntent().getStringExtra("userPhone").equals("-1"))
                loadOrders(Common.currentUser.getPhone());
            else {
                if (getIntent().getStringExtra("userPhone") == null)
                    loadOrders(Common.currentUser.getPhone());
                else
                    loadOrders(getIntent().getStringExtra("userPhone"));
            }
        }
        else
            Toast.makeText(this, "Please check your connection", Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadOrders(String phone) {
        adapter = new FirebaseRecyclerAdapter<Request, OrderViewHolder>(
                Request.class,
                R.layout.layout_order,
                OrderViewHolder.class,
                requests.orderByChild("phone").equalTo(phone)
        ) {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            private void populateViewHolder(OrderViewHolder viewHolder, Request model, final int position) {
                viewHolder.txtOrderId.setText(adapter.getRef(position).getKey());
                viewHolder.txtOrderStatus.setText(Common.convertCodeToStatus(model.getStatus()));
                viewHolder.txtOrderAddress.setText(model.getAddress());
                viewHolder.txtOrderPhone.setText(model.getPhone());

                viewHolder.btn_delete.setOnClickListener(v -> {
                    if (Common.isConnectionToInternet(getApplicationContext())){
                        if (adapter.getItem(position).getStatus().equals("0")){
                            deleteOrder(adapter.getRef(position).getKey());
                            adapter.notifyDataSetChanged();
                            recyclerView.setAdapter(adapter);
                        }
                        else
                            Toast.makeText(OrderStatusActivity.this, "You can't delete this order !", Toast.LENGTH_SHORT).show();
                    }
                    else
                        Toast.makeText(OrderStatusActivity.this, "Please check your connection !", Toast.LENGTH_SHORT).show();
                });

                viewHolder.setItemClickListener((view, position1, isLongClick) -> {
                    Common.currentKey = adapter.getRef(position1).getKey();
                    startActivity(new Intent(OrderStatusActivity.this, TrackingOrderActivity.class));
                });
            }
        };
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private void deleteOrder(final String key) {
        requests.child(key)
                .removeValue().addOnSuccessListener(aVoid -> Toast.makeText(OrderStatusActivity.this, new StringBuilder("Order ")
                    .append(key)
                    .append(" has been deleted !"), Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(OrderStatusActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }

}