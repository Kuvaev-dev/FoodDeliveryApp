package kuvaev.mainapp.eatit.Remote;

import kuvaev.mainapp.eatit.Model.DataMessage;
import kuvaev.mainapp.eatit.Model.Response;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    @Headers(
            {
                    "Content-Type:application/json",
                    "Authorization:key=.........................................................."
            }
    )

    @POST("fcm/send")
    Call<Response> sendNotification(@Body DataMessage body);
}
