package kuvaev.mainapp.eatit.Common;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.ParseException;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import kuvaev.mainapp.eatit.Model.User;
import kuvaev.mainapp.eatit.Remote.APIService;
import kuvaev.mainapp.eatit.Remote.GoogleRetrofitClient;
import kuvaev.mainapp.eatit.Remote.GoogleServiceAction;
import kuvaev.mainapp.eatit.Remote.RetrofitClient;

public class Common {
    public static User currentUser;
    public static String currentKey;

    public static String topicName = "News";

    private static final String BASE_URL = "....................";
    private static final String GOOGLE_API_URL = "...................";

    public static String MAP_API_KEY = "........................";

    public static final String PHONE_TEXT = "userPhone";
    public static final String INTENT_FOOD_ID = "FoodId";
    public static final String DELETE = "Delete";
    public static final String USER_KEY = "User";
    public static final String PWD_KEY = "Password";

    public static String convertCodeToStatus(String code){
        switch (code) {
            case "0":
                return "Placed";
            case "1":
                return "On my way";
            case "2":
                return "Shipping";
            default:
                return "Shipped";
        }
    }

    public static boolean isConnectionToInternet(Context context){
        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo[] info = connectivityManager.getAllNetworkInfo();
            if (info != null) {
                for (NetworkInfo networkInfo : info) {
                    if (networkInfo.getState() == NetworkInfo.State.CONNECTED)
                        return true;
                }
            }
        }
        return false;
    }

    public static APIService getFCMService(){
        return RetrofitClient.getClient(BASE_URL).create(APIService.class);
    }

    public static GoogleServiceAction getGoogleMapAPI(){
        return GoogleRetrofitClient.getGoogleClient(GOOGLE_API_URL).create(GoogleServiceAction.class);
    }

    // This function will convert current to number base on local
    public static BigDecimal formatCurrent(String amount, Locale locale) throws ParseException, java.text.ParseException {
        NumberFormat format = NumberFormat.getCurrencyInstance(locale);
        if (format instanceof DecimalFormat)
            ((DecimalFormat)format).setParseBigDecimal(true);

        return (BigDecimal)format.parse(amount.replace("[^\\d.,]" , ""));
    }
}
