package kuvaev.mainapp.eatit;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.FirebaseDatabase;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.Objects;

import info.hoang8f.widget.FButton;
import kuvaev.mainapp.eatit.Common.Common;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class UpdateUsername extends AppCompatActivity {
    MaterialEditText username;
    FButton confirm;

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

        setContentView(R.layout.activity_update_username);

        username = findViewById(R.id.edtUsername);
        confirm = findViewById(R.id.btnConfirm);

        confirm.setOnClickListener(v -> showConfirmDialog());
    }

    private void showConfirmDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(UpdateUsername.this);
        alertDialog.setTitle("Confirm Update Username?");

        LayoutInflater inflater = LayoutInflater.from(this);
        View layout_signout = inflater.inflate(R.layout.confirm_signout_layout, null);
        alertDialog.setView(layout_signout);
        alertDialog.setIcon(R.drawable.ic_priority_high_black_24dp);

        alertDialog.setPositiveButton("Confirm", (dialog, which) -> {
            dialog.dismiss();

            if (TextUtils.isEmpty(username.getText())) {
                Toast.makeText(UpdateUsername.this, "Username is Empty!", Toast.LENGTH_SHORT).show();
            } else {
                Common.currentUser.setname(Objects.requireNonNull(username.getText()).toString());
                FirebaseDatabase.getInstance().getReference("User")
                        .child(Common.currentUser.getPhone())
                        .setValue(Common.currentUser)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful())
                                Toast.makeText(UpdateUsername.this, "Username was updated!", Toast.LENGTH_SHORT).show();
                                finish();
                        });
            }
        });

        alertDialog.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }
}
