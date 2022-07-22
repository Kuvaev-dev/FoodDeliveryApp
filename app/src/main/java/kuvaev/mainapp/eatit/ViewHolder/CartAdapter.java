package kuvaev.mainapp.eatit.ViewHolder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import kuvaev.mainapp.eatit.CartActivity;
import kuvaev.mainapp.eatit.Common.Common;
import kuvaev.mainapp.eatit.Database.Database;
import kuvaev.mainapp.eatit.Model.Food;
import kuvaev.mainapp.eatit.Model.Order;
import kuvaev.mainapp.eatit.R;

public class CartAdapter extends RecyclerView.Adapter<CartViewHolder> {
    private final List<Order> listData;
    private final CartActivity cart;

    private String foodImage;
    Food currentFood;

    public CartAdapter(List<Order> listData, CartActivity cart) {
        this.listData = listData;
        this.cart = cart;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(cart).inflate(R.layout.layout_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        holder.txt_cart_name.setText(listData.get(holder.getBindingAdapterPosition()).getProductName());

        Picasso.get()
                .load(listData.get(holder.getBindingAdapterPosition()).getImage())
                .placeholder(android.R.color.holo_green_dark)
                .resize(70 , 70)
                .centerCrop()
                .into(holder.imgCart, new Callback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(cart, "SUCCESS", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(cart, "ERROR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        holder.btn_quantity.setNumber(listData.get(holder.getBindingAdapterPosition()).getQuantity());
        holder.btn_quantity.setOnValueChangeListener(new ElegantNumberButton.OnValueChangeListener() {
            @Override
            public void onValueChange(ElegantNumberButton view, int oldValue, int newValue) {

                Order order = listData.get(holder.getBindingAdapterPosition());
                order.setQuantity(String.valueOf(newValue));
                new Database(cart).updateCart(order);

                // Update txtTotalPrice
                // Calculate total price
                int total = 0;
                List<Order> orders = new Database(cart).getCarts(Common.currentUser.getPhone());

                for (Order item : orders)
                    total += (Integer.parseInt(order.getPrice()) * Integer.parseInt(item.getQuantity()));

                Locale locale = new Locale("en" , "US");
                NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);

                cart.txtTotalPrice.setText(fmt.format(total));
            }
        });

        Locale locale = new Locale("en" , "US");
        NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);
        int price = Integer.parseInt(listData.get(holder.getBindingAdapterPosition()).getPrice()) *
                    Integer.parseInt(listData.get(holder.getBindingAdapterPosition()).getQuantity());
        holder.txt_price.setText(fmt.format(price));
    }

    @Override
    public int getItemCount() {
        return listData.size();
    }

    public Order getItem(int position){
        return listData.get(position);
    }

    public void removeItem(int position){
        listData.remove(position);
        notifyItemRemoved(position);
    }

    public void restoreItem(Order item, int position){
        listData.add(position , item);
        notifyItemInserted(position);
    }
}
