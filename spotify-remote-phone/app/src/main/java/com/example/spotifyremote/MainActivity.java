package com.example.spotifyremote;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.garmin.connectiq.ConnectIQ;
import com.garmin.connectiq.IQApp;
import com.garmin.connectiq.IQDevice;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Spotify-RemoteMain";
    
    // Configuration Variables (Update these with your specific IDs)
    private static final String GARMIN_APP_UUID = "bd3245ea-175c-451c-b85c-58e898904fb9";
    private static final String SPOTIFY_CLIENT_ID = "your-spotify-client-id";
    private static final String SPOTIFY_REDIRECT_URI = "com.example.spotifyremote://callback";

    private ConnectIQ mConnectIQ;
    private IQApp mGarminApp;
    private SpotifyAppRemote mSpotifyAppRemote;
    private TextView mStatusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Simple UI view just to show connection status on the phone screen
        mStatusTextView = new TextView(this);
        mStatusTextView.setText("Status: Starting On-Demand Bridge...");
        mStatusTextView.setTextSize(18);
        setContentView(mStatusTextView);

        mGarminApp = new IQApp(GARMIN_APP_UUID);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Connect to both SDKs ONLY when this activity is visible/opened
        connectToSpotify();
        connectToGarmin();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Clean up connections entirely when you leave or minimize the app
        disconnectSDKs();
    }

    private void connectToSpotify() {
        ConnectionParams connectionParams = new ConnectionParams.Builder(SPOTIFY_CLIENT_ID)
                .setRedirectUri(SPOTIFY_REDIRECT_URI)
                .showAuthView(true)
                .build();

        SpotifyAppRemote.connect(this, connectionParams, new Connector.ConnectionListener() {
            @Override
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                mSpotifyAppRemote = spotifyAppRemote;
                updateUI("Connected to Spotify!");
                listenToSpotifyTrackChanges();
            }

            @Override
            public void onFailure(Throwable throwable) {
                updateUI("Spotify Link Failed. Make sure Spotify is open!");
            }
        });
    }

    private void connectToGarmin() {
        mConnectIQ = ConnectIQ.getInstance(this, ConnectIQ.IQConnectType.WIRELESS);
        mConnectIQ.initialize(this, true, new ConnectIQ.ConnectIQListener() {
            @Override
            public void onSdkReady() {
                registerWatchListeners();
            }

            @Override
            public void onSdkShutDown() {}

            @Override
            public void onInitializationError(ConnectIQ.IQSdkErrorStatus errorStatus) {
                updateUI("Garmin SDK Error: " + errorStatus.name());
            }
        });
    }

    private void registerWatchListeners() {
        try {
            List<IQDevice> pairedDevices = mConnectIQ.getKnownDevices();
            if (pairedDevices == null) return;

            for (IQDevice device : pairedDevices) {
                // Listen to commands arriving from the watch app
                mConnectIQ.registerForAppMessages(device, mGarminApp, (iqDevice, iqApp, list, status) -> {
                    if (list != null && list.size() > 0 && list.get(0) instanceof Map) {
                        Map<String, String> dataMap = (Map<String, String>) list.get(0);
                        executeWatchAction(dataMap.get("action"));
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Garmin listener crash: " + e.getMessage());
        }
    }

    private void executeWatchAction(String action) {
        if (action == null || mSpotifyAppRemote == null) return;

        switch (action) {
            case "PLAY_PAUSE":
                mSpotifyAppRemote.getPlayerApi().getPlayerState().setEventCallback(state -> {
                    if (state.isPaused) { mSpotifyAppRemote.getPlayerApi().resume(); } 
                    else { mSpotifyAppRemote.getPlayerApi().pause(); }
                });
                break;
            case "NEXT_TRACK":
                mSpotifyAppRemote.getPlayerApi().skipNext();
                break;
            case "PREV_TRACK":
                mSpotifyAppRemote.getPlayerApi().skipPrevious();
                break;
            case "VOLUME_UP":
                mSpotifyAppRemote.getConnectApi().connectIncreaseVolume();
                break;
            case "VOLUME_DOWN":
                mSpotifyAppRemote.getConnectApi().connectDecreaseVolume();
                break;
        }
    }

    private void listenToSpotifyTrackChanges() {
        if (mSpotifyAppRemote == null) return;

        mSpotifyAppRemote.getPlayerApi().subscribeToPlayerState().setEventCallback(playerState -> {
            if (playerState.track != null) {
                String songInfo = playerState.track.name + " - " + playerState.track.artist.name;
                
                // Push song name metadata up to the watch's primary display screen
                sendDataToWatch(songInfo);
            }
        });
    }

    private void sendDataToWatch(String messagePayload) {
        try {
            List<IQDevice> devices = mConnectIQ.getConnectedDevices();
            if (devices == null) return;
            
            for (IQDevice device : devices) {
                mConnectIQ.sendMessage(device, mGarminApp, messagePayload, (iqDevice, iqApp, status) -> {
                    // Message confirmation channel
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed sending metadata to watch: " + e.getMessage());
        }
    }

    private void disconnectSDKs() {
        if (mSpotifyAppRemote != null) {
            SpotifyAppRemote.disconnect(mSpotifyAppRemote);
            mSpotifyAppRemote = null;
        }
        if (mConnectIQ != null) {
            try {
                mConnectIQ.shutdown(this);
            } catch (Exception e) {
                Log.e(TAG, "Error closing Garmin framework: " + e.getMessage());
            }
        }
        updateUI("Disconnected. Dormant until opened again.");
    }

    private void updateUI(String message) {
        runOnUiThread(() -> mStatusTextView.setText("Status: " + message));
    }
}