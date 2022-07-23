package kuvaev.mainapp.eatit.Remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface GoogleServiceAction {
    @GET
    Call<String> getLocationFromAddress(@Url String url);

    @GET("maps/api/directions/json?key=yourKey&sensor=true&language=en&mode=driving")
    Call<String> getDirections(@Query("origin") String origin, @Query("destination") String destination);
}
