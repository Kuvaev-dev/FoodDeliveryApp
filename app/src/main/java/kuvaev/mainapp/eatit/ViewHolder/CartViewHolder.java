package kuvaev.mainapp.eatit.ViewHolder;

import android.view.ContextMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.mcdev.quantitizerlibrary.HorizontalQuantitizer;

import kuvaev.mainapp.eatit.Common.Common;
import kuvaev.mainapp.eatit.Interface.ItemClickListener;
import kuvaev.mainapp.eatit.R;

public class CartViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
        View.OnCreateContextMenuListener {
    public TextView txt_cart_name , txt_price;
    public ImageView imgCart;
    public HorizontalQuantitizer btn_quantity;

    public RelativeLayout view_background;
    public LinearLayout view_foreground;

    private ItemClickListener itemClickListener;

    public void setItemClickListener(ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public CartViewHolder(View itemView) {
        super(itemView);

        txt_cart_name = (TextView)itemView.findViewById(R.id.card_item_name);
        txt_price = (TextView)itemView.findViewById(R.id.card_item_price);
        btn_quantity = (HorizontalQuantitizer) itemView.findViewById(R.id.btn_quantity);
        imgCart = (ImageView)itemView.findViewById(R.id.card_item_image);
        view_background = (RelativeLayout)itemView.findViewById(R.id.view_background);
        view_foreground = (LinearLayout)itemView.findViewById(R.id.view_foreground);

        itemView.setOnCreateContextMenuListener(this);
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.setHeaderTitle("Select action");
        menu.add(0 , 0 , getBindingAdapterPosition() , Common.DELETE);
    }
}
