package kuvaev.mainapp.eatit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.Objects;

import kuvaev.mainapp.eatit.Model.User;

public class SignUpActivity extends AppCompatActivity {
    MaterialEditText editPhone, editName, editPassword;
    Button btnSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        editName = findViewById(R.id.editName);
        editPassword = findViewById(R.id.editPassword);
        editPhone = findViewById(R.id.editPhone);
        btnSignUp = findViewById(R.id.btnSignUp);

        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference table_user = database.getReference("User");

        btnSignUp.setOnClickListener(view -> {
            ProgressDialog mDialog = new ProgressDialog(SignUpActivity.this);
            mDialog.setMessage("Please wait...");
            mDialog.show();

            table_user.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.child(Objects.requireNonNull(editPhone.getText()).toString()).exists()) {
                        mDialog.dismiss();
                        Toast.makeText(SignUpActivity.this, "Phone number already exist!", Toast.LENGTH_SHORT).show();
                    } else {
                        mDialog.dismiss();
                        User user = new User(
                                Objects.requireNonNull(editName.getText()).toString(),
                                Objects.requireNonNull(editPassword.getText()).toString()
                        );
                        table_user.child(editPhone.getText().toString()).setValue(user);
                        Toast.makeText(SignUpActivity.this, "Sign up successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        });
    }
}