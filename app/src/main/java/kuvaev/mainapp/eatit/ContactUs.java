package kuvaev.mainapp.eatit;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class ContactUs extends AppCompatActivity {
    ImageView image_call,image_mail,image_facebook;
    public static String FACEBOOK_URL = "https://www.facebook.com/borenos";
    public static String FACEBOOK_PAGE_ID = "Borenos";

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

        setContentView(R.layout.activity_contact_us);

        image_call = findViewById(R.id.image_call);
        image_mail = findViewById(R.id.image_mail);
        image_facebook = findViewById(R.id.image_facebook);

        image_call.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:088486623"));
            ActivityCompat.checkSelfPermission(ContactUs.this,
                    Manifest.permission.CALL_PHONE);
            startActivity(intent);
        });

        image_mail.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"syetchau@hotmail.com"});
            emailIntent.putExtra(Intent.EXTRA_CC, new String[]{""});

            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "");

            emailIntent.setType("message/rfc822");
            startActivity(Intent.createChooser(emailIntent,"Choose email..."));
        });

        image_facebook.setOnClickListener(v -> {
            Intent facebookIntent = new Intent(Intent.ACTION_VIEW);
            String facebookUrl = getFacebookPageURL(getBaseContext());
            facebookIntent.setData(Uri.parse(facebookUrl));
            startActivity(facebookIntent);
        });
    }

    public String getFacebookPageURL(Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            int versionCode = packageManager.getPackageInfo("com.facebook.katana", 0).versionCode;
            if (versionCode >= 3002850) { //newer versions of fb app
                return "fb://facewebmodal/f?href=" + FACEBOOK_URL;
            } else { //older versions of fb app
                return "fb://page/" + FACEBOOK_PAGE_ID;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return FACEBOOK_URL; //normal web url
        }
    }
}
