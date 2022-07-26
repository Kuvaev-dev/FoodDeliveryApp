package kuvaev.mainapp.eatit;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.squareup.picasso.Picasso;

import java.util.UUID;

import info.hoang8f.widget.FButton;
import kuvaev.mainapp.eatit.Common.Common;
import kuvaev.mainapp.eatit.Model.User;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class Profile extends AppCompatActivity {
    public TextView profile_name,  profile_phone, profile_address;
    FButton btnUpdateUsername, btnUpdateHomeAddress,  btnSelect, btnUpload;
    CircularImageView profile_pic;
    Uri saveUri;
    FirebaseStorage storage;
    StorageReference storageReference;
    User newUser;
    FirebaseDatabase db;
    DatabaseReference user;

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
        setContentView(R.layout.activity_profile);

        db = FirebaseDatabase.getInstance();

        user = db.getReference("User").child(Common.currentUser.getPhone());
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        loadProfile();

        btnUpdateHomeAddress = findViewById(R.id.btn_updateAddress);
        btnUpdateUsername = findViewById(R.id.btn_updateUsername);
        profile_pic = findViewById(R.id.profile_picture);
        profile_pic.setBorderColor(getResources().getColor(R.color.fbutton_color_green_sea));
        profile_pic.setBorderWidth(2);

        btnUpdateUsername.setOnClickListener(v -> {
            Intent updateUsername = new Intent(Profile.this, UpdateUsername.class);
            startActivity(updateUsername);
        });
        btnUpdateHomeAddress.setOnClickListener(v -> {
            Intent updateAddress = new Intent(Profile.this, UpdateAddress.class);
            startActivity(updateAddress);
        });
        profile_pic.setOnClickListener(v -> showChangeProfileDialog());
    }


    private void showChangeProfileDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(Profile.this, androidx.appcompat.R.style.Theme_AppCompat_DayNight_Dialog_Alert);
        alertDialog.setTitle("Change Profile Picture");

        LayoutInflater inflater = this.getLayoutInflater();
        View change_profile = inflater.inflate(R.layout.change_profile_dialog,null);
        btnSelect = change_profile.findViewById(R.id.btnSelect);
        btnUpload = change_profile.findViewById(R.id.btnUpload);

        // Event for button
        btnSelect.setOnClickListener(v -> {
            // let users select image from gallery and save URL of this image
            chooseImage();
        });
        btnUpload.setOnClickListener(v -> {
            // upload image
            uploadImage();
        });

        alertDialog.setView(change_profile);
        alertDialog.setIcon(R.drawable.ic_contacts_black_24dp);

        // set Button
        alertDialog.setPositiveButton("YES", (dialog, which) -> {
            dialog.dismiss();

            //change profile
            user.child("images").setValue(newUser.getImage());
            Picasso.get().load(saveUri).into(profile_pic);
        });

        alertDialog.setNegativeButton("NO", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityIfNeeded(Intent.createChooser(intent, "Select Image"), Common.PICK_IMAGE_REQUEST);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Common.PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data !=null
                && data.getData()!= null){

            saveUri = data.getData();
            btnSelect.setText(R.string.image_selected_string);
        }
    }

    private void uploadImage() {
        if (saveUri != null){
            final ProgressDialog mDialog = new ProgressDialog(this);
            mDialog.setMessage("Uploading...");
            mDialog.show();

            String imageName = UUID.randomUUID().toString();
            final StorageReference imageFolder = storageReference.child("images/" + imageName);
            imageFolder.putFile(saveUri).addOnSuccessListener(taskSnapshot -> {
                mDialog.dismiss();
                Toast.makeText(Profile.this, "Uploaded!!!", Toast.LENGTH_SHORT).show();
                imageFolder.getDownloadUrl().addOnSuccessListener(uri -> {
                    // set value for newCategory if image upload and we can get download link
                    newUser = new User();
                    newUser.setImage(uri.toString());

                });
            }).addOnFailureListener(e -> {
                mDialog.dismiss();
                Toast.makeText(Profile.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }).addOnProgressListener(taskSnapshot -> {
                int progress = (int) (100 * taskSnapshot.getBytesTransferred() /taskSnapshot.getTotalByteCount());
                mDialog.setMessage("Uploading" + progress +" % ");
            });
        }
    }

    public void loadProfile(){
        ValueEventListener imageListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String images = dataSnapshot.child("images").getValue(String.class);
                Picasso.get().load(images).into(profile_pic);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        profile_name = findViewById(R.id.profile_name);
        profile_phone = findViewById(R.id.profile_phone);
        profile_address = findViewById(R.id.profile_address);

        profile_name.setText(Common.currentUser.getName());
        profile_address.setText(Common.currentUser.getHomeAddress());
        profile_phone.setText(Common.currentUser.getPhone());
        user.addValueEventListener(imageListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
    }
}
