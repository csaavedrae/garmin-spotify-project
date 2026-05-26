package com.example.spotifyremote;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SpotifyRemote";

    // Garmin Watch App UUID
    private static final String GARMIN_APP_UUID =
            "bd3245ea-175c-451c-b85c-58e898904fb9";

    // Spotify Config
    private static final String SPOTIFY_CLIENT_ID = "c196a88a370e461c9687ae590b152674"; // harcoding for testing   BuildConfig.SPOTIFY_CLIENT_ID;

    private static final String SPOTIFY_REDIRECT_URI =
            "com.example.spotifyremote://callback";

    private ConnectIQ mConnectIQ;
    private IQApp mGarminApp;
    private SpotifyAppRemote mSpotifyAppRemote;
    private TextView mStatusTextView;
    private Button mSpotifyButton;
    private Button mGarminButton;
    private Button mRestartButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Basic UI Setup 
        setContentView(R.layout.activity_main);

        mStatusTextView = findViewById(R.id.statusText);

        mSpotifyButton = findViewById(R.id.connectSpotifyButton);
        mGarminButton = findViewById(R.id.connectGarminButton);
        mRestartButton = findViewById(R.id.restartButton);

        // Garmin Watch App Reference
        mGarminApp = new IQApp(GARMIN_APP_UUID);

        // THE FIX: ADD CLICK LISTENERS HERE
        mSpotifyButton.setOnClickListener(v -> connectToSpotify());
        
        mGarminButton.setOnClickListener(v -> connectToGarmin());
        
        mRestartButton.setOnClickListener(v -> {
            disconnectSDKs();
            updateUI("Ready - press a button to connect");
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "onStart called");

        updateUI("Starting connections...");
        
        //connectToSpotify();
        //connectToGarmin();
        //Commented out to split connections independently
        updateUI("Ready - press a button to connect");
    }

    @Override
    protected void onStop() {
        super.onStop();

        disconnectSDKs();
    }

    // ---------------------------------------------------
    // SPOTIFY CONNECTION
    // ---------------------------------------------------

    private void connectToSpotify() {

    Log.d(TAG, "Starting Spotify connection");
    updateUI("Connecting to Spotify...");

    ConnectionParams connectionParams =
            new ConnectionParams.Builder(SPOTIFY_CLIENT_ID)
                    .setRedirectUri(SPOTIFY_REDIRECT_URI)
                    .showAuthView(true)
                    .build();

    SpotifyAppRemote.connect(this, connectionParams,
            new Connector.ConnectionListener() {

                @Override
                public void onConnected(SpotifyAppRemote spotifyAppRemote) {

                    mSpotifyAppRemote = spotifyAppRemote;

                    Log.d(TAG, "Spotify connected");
                    updateUI("Connected to Spotify!");

                    listenToSpotifyTrackChanges();
                }

                @Override
                public void onFailure(Throwable throwable) {

                    Log.e(TAG, "Spotify connection failed", throwable);
                    String errorMessage = throwable.getMessage();

                    if (errorMessage != null) {
                        updateUI("Spotify error: " + errorMessage);
                    } else {
                        updateUI("Spotify connection failed");
                    }

                    throwable.printStackTrace();
                }
            });
}

    // ---------------------------------------------------
    // GARMIN CONNECTION
    // ---------------------------------------------------

    private void connectToGarmin() {

        mConnectIQ =
                ConnectIQ.getInstance(
                        this,
                        ConnectIQ.IQConnectType.WIRELESS
                );

        mConnectIQ.initialize(
                this,
                true,
                new ConnectIQ.ConnectIQListener() {

                    @Override
                    public void onSdkReady() {

                        updateUI("Garmin Connected");

                        registerWatchListeners();

                        try {

                            List<IQDevice> pairedDevices = mConnectIQ.getKnownDevices();

                            for (IQDevice device : pairedDevices) {

                                mConnectIQ.registerForDeviceEvents(device, (iqDevice, status) -> {

                                    Log.d("GARMIN", "Device status: " + status);

                                });

                            }

                        } catch (Exception e) {

                            Log.e("GARMIN", "ConnectIQ error", e);

                        }
                    }

                    @Override
                    public void onInitializeError(
                            ConnectIQ.IQSdkErrorStatus errorStatus
                    ) {

                        Log.e(
                                TAG,
                                "ConnectIQ Init Error: " + errorStatus
                        );
                    }

                    @Override
                    public void onSdkShutDown() {

                        Log.d(TAG, "ConnectIQ SDK Shutdown");
                    }
                }
        );
    }

    // ---------------------------------------------------
    // WATCH COMMAND LISTENERS
    // ---------------------------------------------------

    private void registerWatchListeners() {

        try {

            List<IQDevice> pairedDevices =
                    mConnectIQ.getKnownDevices();

            if (pairedDevices == null) {
                return;
            }

            for (IQDevice device : pairedDevices) {

                mConnectIQ.registerForAppEvents(
                        device,
                        mGarminApp,
                        (iqDevice, iqApp, list, status) -> {

                            if (
                                    list != null &&
                                    list.size() > 0 &&
                                    list.get(0) instanceof Map
                            ) {

                                Map<String, String> dataMap =
                                        (Map<String, String>) list.get(0);

                                String action = dataMap.get("action");

                                Log.d(TAG, "Received watch action: " + action);

                                executeWatchAction(action);
                            }
                        }
                );
            }

        } catch (Exception e) {

            Log.e(TAG, "Watch listener error", e);
        }
    }

    // ---------------------------------------------------
    // EXECUTE WATCH COMMANDS
    // ---------------------------------------------------

    private void executeWatchAction(String action) {

        if (action == null || mSpotifyAppRemote == null) {
            return;
        }

        switch (action) {

            case "PLAY_PAUSE":

                mSpotifyAppRemote
                        .getPlayerApi()
                        .getPlayerState()
                        .setResultCallback(state -> {

                            if (state.isPaused) {

                                mSpotifyAppRemote
                                        .getPlayerApi()
                                        .resume();

                            } else {

                                mSpotifyAppRemote
                                        .getPlayerApi()
                                        .pause();
                            }
                        });

                break;

            case "NEXT_TRACK":

                mSpotifyAppRemote
                        .getPlayerApi()
                        .skipNext();

                break;

            case "PREV_TRACK":

                mSpotifyAppRemote
                        .getPlayerApi()
                        .skipPrevious();

                break;
        }
    }

    // ---------------------------------------------------
    // TRACK CHANGE LISTENER
    // ---------------------------------------------------

    private void listenToSpotifyTrackChanges() {

        if (mSpotifyAppRemote == null) {
            return;
        }

        mSpotifyAppRemote
                .getPlayerApi()
                .subscribeToPlayerState()
                .setEventCallback(playerState -> {

                    if (playerState.track != null) {

                        String songInfo =
                                playerState.track.name
                                        + " - "
                                        + playerState.track.artist.name;

                        sendDataToWatch(songInfo);
                    }
                });
    }

    // ---------------------------------------------------
    // SEND SONG DATA TO WATCH
    // ---------------------------------------------------

    private void sendDataToWatch(String messagePayload) {

        try {

            List<IQDevice> devices =
                    mConnectIQ.getConnectedDevices();

            if (devices == null) {
                return;
            }

            for (IQDevice device : devices) {

                mConnectIQ.sendMessage(
                        device,
                        mGarminApp,
                        messagePayload,
                        (iqDevice, iqApp, status) -> {

                            Log.d(
                                    TAG,
                                    "Message status: " + status
                            );
                        }
                );
            }

        } catch (Exception e) {

            Log.e(TAG, "Failed sending to watch", e);
        }
    }

    // ---------------------------------------------------
    // CLEANUP
    // ---------------------------------------------------

    private void disconnectSDKs() {

        if (mSpotifyAppRemote != null) {

            SpotifyAppRemote.disconnect(mSpotifyAppRemote);

            mSpotifyAppRemote = null;
        }

        if (mConnectIQ != null) {

            try {

                mConnectIQ.shutdown(this);

            } catch (Exception e) {

                Log.e(TAG, "Garmin shutdown error", e);
            }
        }

        updateUI("Disconnected");
    }

    // ---------------------------------------------------
    // UI HELPER
    // ---------------------------------------------------

    private void updateUI(String message) {

        runOnUiThread(() ->
                mStatusTextView.setText("Status: " + message)
        );
    }
}