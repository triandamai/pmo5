package com.kelompok3.trian.rps5_10;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.PolyUtil;
import com.kelompok3.trian.rps5_10.network.ApiServices;
import com.kelompok3.trian.rps5_10.network.InitLibrary;
import com.kelompok3.trian.rps5_10.response.Distance;
import com.kelompok3.trian.rps5_10.response.Duration;
import com.kelompok3.trian.rps5_10.response.LegsItem;
import com.kelompok3.trian.rps5_10.response.ResponseRoute;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OjekActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Location lokasi_sekarang;
    private int REQUEST_CODE;
    private String API_KEY = "AIzaSyDjkDzp8QfPMPVKqn6gyFY3zflpZUsIFHw";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ojek);

        cek_pemission();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(OjekActivity.this);

        //placeauto complete
        PlaceAutocompleteFragment autoCompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        autoCompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(final Place place) {
                ruteaksi(place);
            }

            @Override
            public void onError(Status status) {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            Task lokasi = fusedLocationProviderClient.getLastLocation();
            lokasi.addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()) {
                       // gawe_toast("Berhasil mendapatkan Lokasi");
                        Toast.makeText(OjekActivity.this,"berhasil dapet lokasi",Toast.LENGTH_LONG).show();
                        lokasi_sekarang = (Location) task.getResult();
                        pindah_kamera(new LatLng(lokasi_sekarang.getLatitude(),
                                        lokasi_sekarang.getLongitude()),
                                "Lokasi Saya", false);
                    } else {
                       // gawe_toast("Gagal mendapatkan Lokasi");
                        Toast.makeText(OjekActivity.this,"Gagal dapet lokasi",Toast.LENGTH_LONG).show();
                    }
                }
            });

        } catch (SecurityException e) {
           // gawe_toast(e.getMessage());
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
    }
    // 5
    private void pindah_kamera(LatLng latLng, String title, boolean marker){
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,15f));
        if(marker){
            MarkerOptions markerOptions = new MarkerOptions().position(latLng).title(title);
            mMap.addMarker(markerOptions);
        }

    }


    private void ruteaksi(Place lokasi) {
        final LatLng lokasiAwal = new LatLng(lokasi_sekarang.getLatitude(),lokasi_sekarang.getLongitude());
        final LatLng lokasiAkhir = new LatLng(lokasi.getLatLng().latitude,lokasi.getLatLng().longitude);

        // Panggil Retrofit
        ApiServices api = InitLibrary.getInstance();

        // Siapkan request
        Call<ResponseRoute> routeRequest = api.request_route(
                lokasiAwal.latitude + "," +lokasiAwal.longitude,
                lokasiAkhir.latitude + "," + lokasiAkhir.longitude,
                API_KEY);

        // kirim request
        routeRequest.enqueue(new Callback<ResponseRoute>() {
            @Override
            public void onResponse(Call<ResponseRoute> call, Response<ResponseRoute> response) {
                if (response.isSuccessful()){
                    try{
                        // tampung response ke variable
                        ResponseRoute dataDirection = response.body();

                        LegsItem dataLegs = dataDirection.getRoutes().get(0).getLegs().get(0);

                        // Dapatkan garis polyline
                        String polylinePoint = dataDirection.getRoutes().get(0).getOverviewPolyline().getPoints();

                        // Decode
                        List<LatLng> decodePath = PolyUtil.decode(polylinePoint);
                        mMap.clear();
                        // Gambar garis ke maps
                        mMap.addPolyline(new PolylineOptions().addAll(decodePath)
                                .width(8f).color(Color.argb(255, 56, 167, 252)))
                                .setGeodesic(true);

                        // Tambah Marker
                        mMap.addMarker(new MarkerOptions().position(lokasiAwal).title("Lokasi Awal"));

                        mMap.addMarker(new MarkerOptions().position(lokasiAkhir).title("Lokasi Akhir"));

                        // Dapatkan jarak dan waktu
                        Distance dataDistance = dataLegs.getDistance();
                        Duration dataDuration = dataLegs.getDuration();

                        /** START
                         * Logic untuk membuat layar berada ditengah2 dua koordinat
                         */

                        LatLngBounds.Builder latLongBuilder = new LatLngBounds.Builder();
                        latLongBuilder.include(lokasiAwal);
                        latLongBuilder.include(lokasiAkhir);

                        // Bounds Coordinata
                        LatLngBounds bounds = latLongBuilder.build();

                        int width = getResources().getDisplayMetrics().widthPixels;
                        int height = getResources().getDisplayMetrics().heightPixels;
                        int paddingMap = (int) (width * 0.2); //jarak dari
                        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, paddingMap);
                        mMap.animateCamera(cu);

                        /** END
                         * Logic untuk membuat layar berada ditengah2 dua koordinat
                         */
                    } catch (Exception e){
                       // gawe_toast(e.getMessage());
                        Toast.makeText(OjekActivity.this,"Gagal dapet lokasi"+e.getMessage(),Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseRoute> call, Throwable throwable) {

            }

        });
    }

    private void cek_pemission() {
        String[] permission = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED){
            requestPermissions(permission, 1234);
        }
    }
    // 2
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case 1234 :
            {
                if(grantResults.length > 0){
                    for (int i = 0; i < grantResults.length ; i++) {
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            finish();
                            return;
                        }
                    }
                   // gawe_toast("Permission granted");
                }
            }
        }
    }


}