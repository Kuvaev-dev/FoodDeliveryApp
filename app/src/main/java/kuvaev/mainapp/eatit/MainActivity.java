package kuvaev.mainapp.eatit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.paperdb.Paper;
import kuvaev.mainapp.eatit.Common.Common;
import kuvaev.mainapp.eatit.Helper.UpdateHelper;
import kuvaev.mainapp.eatit.Model.User;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity implements UpdateHelper.OnUpdateCheckListener {
    Button btnSignUp , btnSignIn;
    TextView textSlogan;

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

        setContentView(R.layout.activity_main);

        Typeface typeface = Typeface.createFromAsset(getAssets() , "fonts/NABILA.TTF");

        btnSignUp = findViewById(R.id.btnSignUp);
        btnSignIn = findViewById(R.id.btnSignIn);
        textSlogan = findViewById(R.id.txtSlogan);
        textSlogan.setTypeface(typeface);

        // init Paper
        Paper.init(this);

        btnSignIn.setOnClickListener(v -> {
            Intent signIn = new Intent(MainActivity.this, SignInActivity.class);
            startActivity(signIn);
        });
        btnSignUp.setOnClickListener(v -> {
            Intent signUp = new Intent(MainActivity.this, SignUpActivity.class);
            startActivity(signUp);
        });

        // Check Remember Check Box
        String user = Paper.book().read(Common.USER_KEY);
        String pwd = Paper.book().read(Common.PWD_KEY);
        if (user != null && pwd != null){
            if (!user.isEmpty() && !pwd.isEmpty())
                login(user , pwd);
        }

        // Check any update version  to your app (Order Food Client Side)
        // If we change our parameters in Firebase Remote config , we can't see ant message for updating IF the app run
        // must cancel the app then run again we will see the message
        UpdateHelper.with(this)
                .onUpdateCheck(this)
                .check();

        printKeyHash();
    }

    private void printKeyHash() {
        try {
            @SuppressLint("PackageManagerGetSignatures")
            PackageInfo info = getPackageManager().getPackageInfo("kuvaev.mainapp.eatit",
                    PackageManager.GET_SIGNATURES);

            for (Signature signature : info.signatures){
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private void login(final String phone, final String pwd) {
        // Init Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference table_user = database.getReference("User");

        if (Common.isConnectionToInternet(MainActivity.this)) {
            final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
            dialog.setMessage("Please waiting...");
            dialog.show();

            table_user.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Check if user not exist in database
                    if (dataSnapshot.child(phone).exists()) {
                        // Get user information
                        dialog.dismiss();
                        User user = dataSnapshot.child(phone).getValue(User.class);
                        assert user != null;
                        user.setPhone(phone); // set Phone

                        if (user.getPassword().equals(pwd)) {
                            Intent homeIntent = new Intent(MainActivity.this, Home.class);
                            Common.currentUser = user;
                            startActivity(homeIntent);
                            finish();
                        } else {
                            Toast.makeText(MainActivity.this, "Wrong Password !!!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this, "User not exist in Database", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        else {
            Toast.makeText(MainActivity.this, "Plaese check your connection !!!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onUpdateCheckListener(final String urlApp) {
        // Create AlertDialog to waiting for checking any updates for the app
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("New Version Available")
                .setMessage("Please update to new version to continue use")
                .setPositiveButton("UPDATE", (dialog, which) -> Toast.makeText(MainActivity.this, "" + urlApp, Toast.LENGTH_SHORT).show())
                .setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss()).show();
        alertDialog.show();
    }
}