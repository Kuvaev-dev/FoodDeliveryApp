package kuvaev.mainapp.eatit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.accountkit.Account;
import com.facebook.accountkit.AccountKit;
import com.facebook.accountkit.AccountKitCallback;
import com.facebook.accountkit.AccountKitError;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.Objects;

import io.paperdb.Paper;
import kuvaev.mainapp.eatit.Common.Common;
import kuvaev.mainapp.eatit.Model.User;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class SignInActivity extends AppCompatActivity {
    MaterialEditText edtPhone, edtPassword;
    CheckBox ckbRemember;
    Button btnSignIn;
    TextView txtForgotPwd;
    LinearLayout facebookSignIn;

    FirebaseDatabase database;
    DatabaseReference table_user;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Note: add this code before setContentView method
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/restaurant_font.otf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        setContentView(R.layout.activity_sign_in);

        edtPhone = findViewById(R.id.edtPhone);
        edtPassword = findViewById(R.id.edtPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        ckbRemember = findViewById(R.id.ckbRemember);
        txtForgotPwd = findViewById(R.id.txtForgotPwd);
        facebookSignIn = findViewById(R.id.SignInFaceBook);

        //Fix showing characters in MaterialEditText password
        Typeface typeface = Typeface.DEFAULT;
        edtPassword.setTypeface(typeface);

        //Init Paper
        Paper.init(this);

        //Init Firebase
        database = FirebaseDatabase.getInstance();
        table_user = database.getReference("User");

        txtForgotPwd.setOnClickListener(v -> showForgotPwdDialog());

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (Common.isConnectionToInternet(SignInActivity.this)) {
                    //Save user & password
                    if (ckbRemember.isChecked()){
                        Paper.book().write(Common.USER_KEY , Objects.requireNonNull(edtPhone.getText()).toString());
                        Paper.book().write(Common.PWD_KEY , Objects.requireNonNull(edtPassword.getText()).toString());
                    }

                    final AlertDialog dialog = new SpotsDialog(SignInActivity.this);
                    dialog.show();
                    dialog.setMessage("Please waiting...");


                    table_user.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            //Check if user not exist in database
                            if (dataSnapshot.child(Objects.requireNonNull(edtPhone.getText()).toString()).exists()) {

                                //Get user information
                                dialog.dismiss();
                                User user = dataSnapshot.child(edtPhone.getText().toString()).getValue(User.class);
                                assert user != null;
                                user.setPhone(edtPhone.getText().toString()); //set Phone

                                if (user.getPassword().equals(Objects.requireNonNull(edtPassword.getText()).toString())) {
                                    Intent homeIntent = new Intent(SignInActivity.this, Home.class);
                                    Common.currentUser = user;
                                    startActivity(homeIntent);
                                    finish();

                                    Toast.makeText(SignInActivity.this, "Sign In Successfully !", Toast.LENGTH_SHORT).show();

                                    table_user.removeEventListener(this);
                                }
                                else {
                                    Toast.makeText(SignInActivity.this, "Wrong Password !!!", Toast.LENGTH_SHORT).show();
                                }

                            }
                            else {
                                dialog.dismiss();
                                Toast.makeText(SignInActivity.this, "User not exist in Database", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
                else {
                    Toast.makeText(SignInActivity.this, "Please check your connection !!!", Toast.LENGTH_SHORT).show();
                }
            }
        });


        facebookSignIn.setOnClickListener(v -> {
            if (AccountKit.getCurrentAccessToken() == null){   //he doesn't have an account in Facebook Account kit
                Toast.makeText(SignInActivity.this, "You don't have an account , Please Sign up by your facebook account", Toast.LENGTH_SHORT).show();
                finish();
            } else { //he have an account
                final AlertDialog dialog = new SpotsDialog(SignInActivity.this);
                dialog.show();
                dialog.setCancelable(false);
                dialog.setMessage("Please waiting...");

                AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                    @Override
                    public void onSuccess(final Account account) {

                        table_user.child(account.getPhoneNumber().toString())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        User localUser = dataSnapshot.getValue(User.class);

                                        Intent homeIntent = new Intent(SignInActivity.this, Home.class);
                                        Common.currentUser = localUser;
                                        assert Common.currentUser != null;
                                        Common.currentUser.setPhone(account.getPhoneNumber().toString());
                                        startActivity(homeIntent);

                                        dialog.dismiss();
                                        finish();

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                    }
                    @Override
                    public void onError(AccountKitError accountKitError) {

                    }
                });
            }

        });

    }

    private void showForgotPwdDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Forgot Password")
                .setMessage("Enter your secure code")
                .setIcon(R.drawable.ic_security_black_24dp);

        View forgot_view = getLayoutInflater().inflate(R.layout.layout_forgot_password , null);
        builder.setView(forgot_view);

        final MaterialEditText edtPhone = forgot_view.findViewById(R.id.edtPhone);
        final MaterialEditText edtSecureCode = forgot_view.findViewById(R.id.edtSecureCode);

        builder.setPositiveButton("YES", (dialog, which) -> {
            //Check if user available
            table_user.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    User user = dataSnapshot.child(Objects.requireNonNull(edtPhone.getText()).toString())
                            .getValue(User.class);

                    assert user != null;
                    if (user.getSecureCode().equals(Objects.requireNonNull(edtSecureCode.getText()).toString()))
                        Toast.makeText(SignInActivity.this, "Your password : " + user.getPassword(), Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(SignInActivity.this, "Wrong secure code !", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        });

        builder.setNegativeButton("NO", (dialog, which) -> {

        });

        builder.show();
    }
}