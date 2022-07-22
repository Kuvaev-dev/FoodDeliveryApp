package kuvaev.mainapp.eatit.ViewHolder;

import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import kuvaev.mainapp.eatit.R;

public class ShowCommentViewHolder extends RecyclerView.ViewHolder {
    public TextView txtUserPhone , txtComment;
    public RatingBar ratingBar;

    public ShowCommentViewHolder(View itemView) {
        super(itemView);

        txtUserPhone = itemView.findViewById(R.id.txtUserPhone);
        txtComment = itemView.findViewById(R.id.txtComment);
        ratingBar = itemView.findViewById(R.id.ratingBar);
    }
}
