package kuvaev.mainapp.eatit.Common;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import java.util.Calendar;
import java.util.Locale;

import kuvaev.mainapp.eatit.Model.User;
import kuvaev.mainapp.eatit.Remote.APIService;
import kuvaev.mainapp.eatit.Remote.GoogleRetrofitClient;
import kuvaev.mainapp.eatit.Remote.GoogleServiceAction;
import kuvaev.mainapp.eatit.Remote.RetrofitClient;

public class Common {
    public static String topicName = "News";

    public static User currentUser;
    public static String currentKey;
    public static final String SHIPPER_INFO_TABLE = "ShippingOrders";

    public static String DISTANCE= "";
    public static String DURATION= "";
    public static String ESTIMATED_TIME = "";

    public static String PHONE_TEXT = "userPhone";

    public static final String INTENT_FOOD_ID = "FoodId";
    public static final int PICK_IMAGE_REQUEST = 71;

    private static final String BASE_URL = "https://fcm.googleapis.com/";
    private static final String GOOGLE_API_URL = "https://maps.googleapis.com/";

    public static APIService getFCMService(){
        return RetrofitClient.getClient(BASE_URL).create(APIService.class);
    }

    public static GoogleServiceAction getGoogleMapAPI(){
        return GoogleRetrofitClient.getGoogleClient(GOOGLE_API_URL).create(GoogleServiceAction.class);
    }

    public static final String DELETE = "Delete";
    public static final String USER_KEY = "User";
    public static final String PWD_KEY = "password";

    public static String convertCodeToStatus(String status) {
        switch (status) {
            case "0":
                return "Placed";
            case "1":
                return "Preparing Orders";
            case "2":
                return "Shipping";
            default:
                return "Delivered";
        }
    }

    public static boolean isConnectedToInternet(Context context){
        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null){
            NetworkInfo[] info = connectivityManager.getAllNetworkInfo();
            if(info != null){
                for (NetworkInfo networkInfo : info) {
                    if (networkInfo.getState() == NetworkInfo.State.CONNECTED)
                        return true;
                }
            }
        }
        return false;
    }

    public static String getDate(long time) {
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        calendar.setTimeInMillis(time);
        return android.text.format.DateFormat.format("dd-MM-yyyy HH:mm", calendar).toString();
    }
}
