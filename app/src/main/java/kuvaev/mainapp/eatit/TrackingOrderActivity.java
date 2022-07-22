package kuvaev.mainapp.eatit;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import kuvaev.mainapp.eatit.Common.Common;
import kuvaev.mainapp.eatit.Helper.JSONParser;
import kuvaev.mainapp.eatit.Model.Request;
import kuvaev.mainapp.eatit.Model.ShippingInformation;
import kuvaev.mainapp.eatit.Remote.GoogleServiceAction;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrackingOrderActivity extends FragmentActivity implements OnMapReadyCallback, ValueEventListener {
    private GoogleMap mMap;

    FirebaseDatabase database;
    DatabaseReference requests , shippingOrders;

    Request currentOrder;

    GoogleServiceAction mService;
    Marker shipperMarker;

    Polyline polyline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking_order);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        database = FirebaseDatabase.getInstance();
        requests = database.getReference("Requests");
        shippingOrders = database.getReference("ShippingOrders");

        mService = Common.getGoogleMapAPI();
    }

    @Override
    protected void onStop() {
        shippingOrders.removeEventListener(this);
        super.onStop();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        trackLocation();
    }

    private void trackLocation() {
        requests.child(Common.currentKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        currentOrder = dataSnapshot.getValue(Request.class);
                        // If order has address
                        assert currentOrder != null;
                        if (currentOrder.getAddress() != null && !currentOrder.getAddress().isEmpty()){
                            mService.getLocationFromAddress("............................." +
                                            currentOrder.getAddress(), Common.MAP_API_KEY)
                                    .enqueue(new Callback<String>() {
                                        @Override
                                        public void onResponse(Call<String> call, Response<String> response) {
                                            // Location client
                                            try {
                                                JSONObject jsonObject = new JSONObject(response.body());
                                                String lat = ((JSONArray)jsonObject.get("results"))
                                                        .getJSONObject(0)
                                                        .getJSONObject("geometry")
                                                        .getJSONObject("location")
                                                        .get("lat").toString();

                                                String lng = ((JSONArray)jsonObject.get("results"))
                                                        .getJSONObject(0)
                                                        .getJSONObject("geometry")
                                                        .getJSONObject("location")
                                                        .get("lng").toString();

                                                LatLng location;

                                                if (lat.isEmpty() || lng.isEmpty())
                                                    location = new LatLng(36.192984,37.117703);  //Default
                                                else
                                                    location = new LatLng(Double.parseDouble(lat) , Double.parseDouble(lng));

                                                mMap.addMarker(new MarkerOptions().position(location)
                                                        .title("Order destination")
                                                        .icon(BitmapDescriptorFactory.defaultMarker()));


                                                // Set Shipper Location
                                                shippingOrders.child(Common.currentKey)
                                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                                ShippingInformation shippingInformation = dataSnapshot.getValue(ShippingInformation.class);

                                                                LatLng shipperLocation;
                                                                assert shippingInformation != null;
                                                                String.valueOf(shippingInformation.getLat());
                                                                String.valueOf(shippingInformation.getLng());
                                                                shipperLocation = new LatLng(
                                                                        shippingInformation.getLat() , shippingInformation.getLng());

                                                                if (shipperMarker == null){
                                                                    shipperMarker = mMap.addMarker(
                                                                            new MarkerOptions().position(shipperLocation)
                                                                                    .title("Shipper #" + shippingInformation.getOrderId())
                                                                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                                                                    );
                                                                } else {
                                                                    shipperMarker.setPosition(shipperLocation);
                                                                }

                                                                // Update Camera
                                                                CameraPosition cameraPosition = new CameraPosition.Builder()
                                                                        .target(shipperLocation)
                                                                        .zoom(16)
                                                                        .bearing(0)
                                                                        .tilt(45)
                                                                        .build();

                                                                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                                                                // draw routes
                                                                if (polyline != null)
                                                                    polyline.remove();

                                                                mService.getDirection(shipperLocation.latitude + "," + shipperLocation.longitude ,
                                                                                currentOrder.getAddress() , Common.MAP_API_KEY)
                                                                        .enqueue(new Callback<String>() {
                                                                            @Override
                                                                            public void onResponse(Call<String> call, Response<String> response) {
                                                                                new ParseTask().execute(response.body());
                                                                            }

                                                                            @Override
                                                                            public void onFailure(Call<String> call, Throwable t) {

                                                                            }
                                                                        });
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                                            }
                                                        });

                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<String> call, Throwable t) {

                                        }
                                    });
                        }
                        // If order has latLng
                        else if (currentOrder.getLatLng() != null && !currentOrder.getLatLng().isEmpty()){

                            mService.getLocationFromAddress("......................." +
                                            currentOrder.getLatLng(), Common.MAP_API_KEY)
                                    .enqueue(new Callback<String>() {
                                        @Override
                                        public void onResponse(Call<String> call, Response<String> response) {

                                            try {

                                                JSONObject jsonObject = new JSONObject(response.body());

                                                String lat = ((JSONArray)jsonObject.get("results"))
                                                        .getJSONObject(0)
                                                        .getJSONObject("geometry")
                                                        .getJSONObject("location")
                                                        .get("lat").toString();

                                                String lng = ((JSONArray)jsonObject.get("results"))
                                                        .getJSONObject(0)
                                                        .getJSONObject("geometry")
                                                        .getJSONObject("location")
                                                        .get("lng").toString();

                                                LatLng location;

                                                if (lat.isEmpty() || lng.isEmpty())
                                                    location = new LatLng(36.192984,37.117703);
                                                else
                                                    location = new LatLng(Double.parseDouble(lat) , Double.parseDouble(lng));

                                                mMap.addMarker(new MarkerOptions().position(location)
                                                        .title("Order destination")
                                                        .icon(BitmapDescriptorFactory.defaultMarker()));

                                                //Set Shipper Location
                                                shippingOrders.child(Common.currentKey)
                                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                ShippingInformation shippingInformation = dataSnapshot.getValue(ShippingInformation.class);

                                                                LatLng shipperLocation;
                                                                assert shippingInformation != null;
                                                                String.valueOf(shippingInformation.getLat());
                                                                shipperLocation = new LatLng(
                                                                        shippingInformation.getLat() , shippingInformation.getLng());

                                                                if (shipperMarker == null){
                                                                    shipperMarker = mMap.addMarker(
                                                                            new MarkerOptions().position(shipperLocation)
                                                                                    .title("Shipper #" + shippingInformation.getOrderId())
                                                                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                                                                    );
                                                                } else {
                                                                    shipperMarker.setPosition(shipperLocation);
                                                                }

                                                                // Update Camera
                                                                CameraPosition cameraPosition = new CameraPosition.Builder()
                                                                        .target(shipperLocation)
                                                                        .zoom(16)
                                                                        .bearing(0)
                                                                        .tilt(45)
                                                                        .build();

                                                                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                                                                // draw routes
                                                                if (polyline != null)
                                                                    polyline.remove();

                                                                mService.getDirection(shipperLocation.latitude + "," + shipperLocation.longitude ,
                                                                                currentOrder.getLatLng() , Common.MAP_API_KEY)
                                                                        .enqueue(new Callback<String>() {
                                                                            @Override
                                                                            public void onResponse(Call<String> call, Response<String> response) {

                                                                                new ParseTask().execute(response.body());
                                                                            }

                                                                            @Override
                                                                            public void onFailure(Call<String> call, Throwable t) {

                                                                            }
                                                                        });
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                                            }
                                                        });

                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<String> call, Throwable t) {

                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        trackLocation();
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {

    }

    private class ParseTask extends AsyncTask<String , Integer , List<List<HashMap<String , String>>>> {
        AlertDialog progressDialog = new SpotsDialog(TrackingOrderActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog.setMessage("Please waiting...");
            progressDialog.show();
        }

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject jsonObject;
            List<List<HashMap<String , String>>> routes = null;

            try {
                jsonObject = new JSONObject(strings[0]);
                JSONParser parser = new JSONParser();
                routes =  parser.parse(jsonObject);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }

            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            super.onPostExecute(lists);

            progressDialog.dismiss();

            ArrayList<LatLng> points = new ArrayList<>();
            PolylineOptions lineOptions = new PolylineOptions();
            lineOptions.width(2);
            lineOptions.color(Color.RED);
            MarkerOptions markerOptions = new MarkerOptions();
            // Traversing through all the routes
            for(int i=0;i<lists.size();i++){
                // Fetching i-th route
                List<HashMap<String, String>> path = lists.get(i);
                // Fetching all the points in i-th route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);
                    double lat = Double.parseDouble(Objects.requireNonNull(point.get("lat")));
                    double lng = Double.parseDouble(Objects.requireNonNull(point.get("lng")));
                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                }
                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);

            }
            // Drawing polyline in the Google Map for the i-th route
            if(points.size()!=0)mMap.addPolyline(lineOptions);//to avoid crash
        }
    }
}