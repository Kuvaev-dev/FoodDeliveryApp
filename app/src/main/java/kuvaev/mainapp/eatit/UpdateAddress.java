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

public class UpdateAddress extends AppCompatActivity {
    MaterialEditText address;
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

        setContentView(R.layout.activity_update_address);

        address = (MaterialEditText)findViewById(R.id.edtHomeAddress);
        confirm = (FButton)findViewById(R.id.btnConfirmAddress);

        confirm.setOnClickListener(v -> showConfirmDialog());
    }

    private void showConfirmDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(UpdateAddress.this);
        alertDialog.setTitle("Confirm Update Address?");

        LayoutInflater inflater = LayoutInflater.from(this);
        View layout_signout = inflater.inflate(R.layout.confirm_signout_layout, null);
        alertDialog.setView(layout_signout);
        alertDialog.setIcon(R.drawable.ic_priority_high_black_24dp);

        alertDialog.setPositiveButton("Confirm", (dialog, which) -> {
            dialog.dismiss();
            if (TextUtils.isEmpty(address.getText())) {
                Toast.makeText(UpdateAddress.this, "Home Address is Empty!", Toast.LENGTH_SHORT).show();
            } else {
                Common.currentUser.setHomeAddress(Objects.requireNonNull(address.getText()).toString());
                FirebaseDatabase.getInstance().getReference("User")
                        .child(Common.currentUser.getPhone())
                        .setValue(Common.currentUser)
                        .addOnCompleteListener(task -> {
                            Toast.makeText(UpdateAddress.this, "Update Address Successfully!", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(UpdateAddress.this, "Home Address Cannot Update!", Toast.LENGTH_SHORT).show());
            }
        });

        alertDialog.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }
}
