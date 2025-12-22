package com.example.doan_zaloclone.ui.location;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.doan_zaloclone.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationPickerActivity extends AppCompatActivity {
    
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    public static final String EXTRA_LOCATION_NAME = "location_name";
    public static final String EXTRA_LOCATION_ADDRESS = "location_address";
    public static final String EXTRA_IS_LIVE_LOCATION = "is_live_location";
    public static final String EXTRA_LIVE_DURATION = "live_duration";
    
    private static final int LOCATION_PERMISSION_REQUEST = 100;
    
    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private Marker selectedMarker;
    private GeoPoint selectedLocation;
    
    private TextView locationInfo;
    private Button btnSendLocation;
    private FloatingActionButton fabMyLocation;
    private android.widget.RadioGroup locationTypeGroup;
    private android.widget.LinearLayout durationPickerLayout;
    private android.widget.Spinner durationSpinner;
    
    private boolean isLiveLocation = false;
    private long selectedDuration = 15 * 60 * 1000; // Default: 15 minutes
    
    // Duration options in milliseconds
    private static final long DURATION_15_MIN = 15 * 60 * 1000;
    private static final long DURATION_1_HOUR = 60 * 60 * 1000;
    private static final long DURATION_8_HOURS = 8 * 60 * 60 * 1000;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Configure osmdroid with performance optimizations
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue(getPackageName());
        
        // Enable tile cache for better performance
        Configuration.getInstance().setTileFileSystemCacheMaxBytes(50L * 1024 * 1024); // 50MB cache
        Configuration.getInstance().setTileFileSystemCacheTrimBytes(40L * 1024 * 1024); // Trim to 40MB
        Configuration.getInstance().setExpirationExtendedDuration(1000L * 60 * 60 * 24 * 7); // 7 days
        
        setContentView(R.layout.activity_location_picker);
        
        // Setup toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setTitle("Chọn vị trí");
            }
        }
        
        // Initialize views
        mapView = findViewById(R.id.mapView);
        locationInfo = findViewById(R.id.locationInfo);
        btnSendLocation = findViewById(R.id.btnSendLocation);
        fabMyLocation = findViewById(R.id.fabMyLocation);
        locationTypeGroup = findViewById(R.id.locationTypeGroup);
        durationPickerLayout = findViewById(R.id.durationPickerLayout);
        durationSpinner = findViewById(R.id.durationSpinner);
        
        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        // Setup map
        setupMap();
        
        // Setup UI logic
        setupDurationSpinner();
        setupLocationTypeListeners();
        
        // Setup buttons
        btnSendLocation.setOnClickListener(v -> sendSelectedLocation());
        fabMyLocation.setOnClickListener(v -> getCurrentLocation());
        
        // Check permissions and get current location
        checkLocationPermission();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    private void setupMap() {
        // Set tile source
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        
        // Enable multitouch
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(false);
        
        // Performance optimizations
        mapView.setVerticalMapRepetitionEnabled(false);
        mapView.setHorizontalMapRepetitionEnabled(false);
        
        // Reduce loading indicator for smoother experience
        mapView.setTilesScaledToDpi(true);
        
        IMapController mapController = mapView.getController();
        mapController.setZoom(15.0);
        
        // Default location (Hanoi, Vietnam)
        GeoPoint defaultPoint = new GeoPoint(21.0285, 105.8542);
        mapController.setCenter(defaultPoint);
        
        // Place default marker at Hanoi
        placeMarker(21.0285, 105.8542);
        
        // Add map click listener
        MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                placeMarker(p.getLatitude(), p.getLongitude());
                return true;
            }
            
            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };
        
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(mapEventsReceiver);
        mapView.getOverlays().add(0, mapEventsOverlay);
    }
    
    private void setupDurationSpinner() {
        String[] durations = {"15 phút", "1 giờ", "8 giờ"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, durations);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        durationSpinner.setAdapter(adapter);
        
        durationSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                switch (position) {
                    case 0: selectedDuration = DURATION_15_MIN; break;
                    case 1: selectedDuration = DURATION_1_HOUR; break;
                    case 2: selectedDuration = DURATION_8_HOURS; break;
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                selectedDuration = DURATION_15_MIN;
            }
        });
    }
    
    private void setupLocationTypeListeners() {
        locationTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioLiveLocation) {
                isLiveLocation = true;
                durationPickerLayout.setVisibility(android.view.View.VISIBLE);
                btnSendLocation.setText("Chia sẻ trực tiếp");
            } else {
                isLiveLocation = false;
                durationPickerLayout.setVisibility(android.view.View.GONE);
                btnSendLocation.setText("Gửi vị trí");
            }
        });
    }
    
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            getCurrentLocation();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Cần quyền truy cập vị trí để sử dụng tính năng này",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            checkLocationPermission();
            return;
        }
        
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lng = location.getLongitude();
                        
                        // Move map to current location
                        GeoPoint point = new GeoPoint(lat, lng);
                        mapView.getController().animateTo(point);
                        
                        // Place marker
                        placeMarker(lat, lng);
                    } else {
                        Toast.makeText(this, "Không thể lấy vị trí hiện tại", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void placeMarker(double latitude, double longitude) {
        // Remove old marker if exists
        if (selectedMarker != null) {
            mapView.getOverlays().remove(selectedMarker);
        }
        
        // Create new marker
        selectedLocation = new GeoPoint(latitude, longitude);
        selectedMarker = new Marker(mapView);
        selectedMarker.setPosition(selectedLocation);
        selectedMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        selectedMarker.setTitle("Vị trí đã chọn");
        
        mapView.getOverlays().add(selectedMarker);
        mapView.invalidate();
        
        // Update location info
        updateLocationInfo(latitude, longitude);
        
        // Enable send button
        btnSendLocation.setEnabled(true);
    }
    
    private void updateLocationInfo(double latitude, double longitude) {
        // Try to get address from geocoder
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressText = address.getAddressLine(0);
                locationInfo.setText(addressText);
            } else {
                locationInfo.setText(String.format(Locale.getDefault(), 
                        "📍 %.4f, %.4f", latitude, longitude));
            }
        } catch (IOException e) {
            locationInfo.setText(String.format(Locale.getDefault(), 
                    "📍 %.4f, %.4f", latitude, longitude));
        }
    }
    
    private void sendSelectedLocation() {
        if (selectedLocation == null) {
            Toast.makeText(this, "Vui lòng chọn vị trí", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get address string
        String locationName = null;
        String locationAddress = locationInfo.getText().toString();
        
        // Return result
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_LATITUDE, selectedLocation.getLatitude());
        resultIntent.putExtra(EXTRA_LONGITUDE, selectedLocation.getLongitude());
        resultIntent.putExtra(EXTRA_LOCATION_NAME, locationName);
        resultIntent.putExtra(EXTRA_LOCATION_ADDRESS, locationAddress);
        resultIntent.putExtra(EXTRA_IS_LIVE_LOCATION, isLiveLocation);
        resultIntent.putExtra(EXTRA_LIVE_DURATION, selectedDuration);
        
        setResult(RESULT_OK, resultIntent);
        finish();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDetach();
        }
    }
}
