package com.example.doan_zaloclone.ui.location;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.LiveLocation;
import com.example.doan_zaloclone.repository.ChatRepository;
import com.example.doan_zaloclone.services.LocationSharingService;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class LiveLocationViewActivity extends AppCompatActivity {

    public static final String EXTRA_SESSION_ID = "session_id";
    public static final String EXTRA_IS_SENDER = "is_sender";

    private MapView mapView;
    private TextView titleText;
    private TextView statusText;
    private Button btnStopSharing;
    private View btnClose;

    private String sessionId;
    private boolean isSender;

    private ListenerRegistration liveLocationListener;
    private Marker userMarker;
    private CountDownTimer countDownTimer;
    private ChatRepository chatRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize OSMDroid configuration
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_live_location_view);

        // Get Intent Extras
        sessionId = getIntent().getStringExtra(EXTRA_SESSION_ID);
        isSender = getIntent().getBooleanExtra(EXTRA_IS_SENDER, false);

        if (sessionId == null) {
            finish();
            return;
        }

        chatRepository = new ChatRepository();

        initViews();
        setupMap();
        startListening();
    }

    private void initViews() {
        mapView = findViewById(R.id.mapView);
        titleText = findViewById(R.id.titleText);
        statusText = findViewById(R.id.statusText);
        btnStopSharing = findViewById(R.id.btnStopSharing);
        btnClose = findViewById(R.id.btnClose);

        btnClose.setOnClickListener(v -> finish());

        if (isSender) {
            btnStopSharing.setVisibility(View.VISIBLE);
            btnStopSharing.setOnClickListener(v -> stopSharing());
        } else {
            btnStopSharing.setVisibility(View.GONE);
        }
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(18.0);
    }

    private void startListening() {
        liveLocationListener = FirebaseFirestore.getInstance()
                .collection("liveLocations")
                .document(sessionId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) {
                        statusText.setText("Phiên chia sẻ đã kết thúc");
                        btnStopSharing.setVisibility(View.GONE);
                        return;
                    }

                    LiveLocation liveLocation = snapshot.toObject(LiveLocation.class);
                    if (liveLocation != null) {
                        updateUI(liveLocation);
                    }
                });
    }

    private void updateUI(LiveLocation liveLocation) {
        // Update Marker
        GeoPoint point = new GeoPoint(liveLocation.getLatitude(), liveLocation.getLongitude());
        if (userMarker == null) {
            userMarker = new Marker(mapView);
            userMarker.setTitle("Vị trí hiện tại");
            userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            // Default icon is fine, or set custom if needed
            mapView.getOverlays().add(userMarker);
        }
        userMarker.setPosition(point);
        mapView.getController().animateTo(point); // Smooth animate
        mapView.invalidate();

        // Update Status & Timer
        boolean isActive = liveLocation.isActive();
        long remainingTime = liveLocation.getEndTime() - System.currentTimeMillis();

        if (!isActive || remainingTime <= 0) {
            statusText.setText("Đã dừng chia sẻ");
            btnStopSharing.setVisibility(View.GONE);
            cleanupTimer();
        } else {
            startTimer(remainingTime);
            if (isSender) {
                btnStopSharing.setVisibility(View.VISIBLE);
            }
        }
    }

    private void startTimer(long durationMillis) {
        cleanupTimer();
        if (durationMillis <= 0) return;

        countDownTimer = new CountDownTimer(durationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = millisUntilFinished / 1000 / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                statusText.setText(String.format("Đang chia sẻ • Còn %02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                statusText.setText("Đã hết thời gian chia sẻ");
                btnStopSharing.setVisibility(View.GONE);
            }
        }.start();
    }

    private void cleanupTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private void stopSharing() {
        // Update Firestore
        chatRepository.stopLiveLocation(sessionId);

        // Stop Service
        Intent intent = new Intent(this, LocationSharingService.class);
        intent.setAction(LocationSharingService.ACTION_STOP_SHARING);
        startService(intent);

        Toast.makeText(this, "Đã dừng chia sẻ vị trí", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (liveLocationListener != null) {
            liveLocationListener.remove();
        }
        cleanupTimer();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }
}
