package com.honu.dicecast;

/**
 * Sender application using the CastCompanionLibrary
 */

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.IntroductoryOverlay;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.common.GoogleApiAvailability;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static DiceViewModel diceViewModel = new DiceViewModel();
    private static RollDiceChannel rollDiceChannel;

    private CastContext castContext;
    private CastSession castSession;
    private CastStateListener castStateListener;
    private MenuItem mediaRouteMenuItem;
    private IntroductoryOverlay introductoryOverlay;

    private SensorManager sensorManager;
    private DiceShaker diceShaker;

    private final SessionManagerListener<CastSession> sessionManagerListener = new CastSessionManagerListener();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setTitle(R.string.app_name);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }

        // check that Google Play services is available and correct version
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        //GoogleApiAvailability.getInstance().verifyGooglePlayServicesIsAvailable(this);

        // tap to roll listener
        View content = findViewById(R.id.root_layout);
        content.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                updateDie(diceViewModel.rollDice());
            }
        });

        // shake to roll listener
        diceShaker = new DiceShaker(new IDiceShakeListener() {

            @Override
            public void onShake() {
                updateDie(diceViewModel.rollDice());
            }
        });

        // register dice shake listener fpr accelerometer events
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(
              diceShaker,
              sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
              SensorManager.SENSOR_DELAY_NORMAL);

        // show introductory overlay
        castStateListener = new CastStateListener() {

            @Override
            public void onCastStateChanged(int newState) {
                if (newState != CastState.NO_DEVICES_AVAILABLE) {
                    showIntroductoryOverlay();
                }
            }
        };

        castContext = CastContext.getSharedInstance(this);
    }


    private class CastSessionManagerListener implements SessionManagerListener<CastSession> {

        @Override
        public void onSessionEnded(CastSession session, int error) {
            if (session == castSession) {
                castSession = null;
            }
            invalidateOptionsMenu();
        }

        @Override
        public void onSessionResumed(CastSession session, boolean wasSuspended) {
            castSession = session;
            invalidateOptionsMenu();
        }


        @Override
        public void onSessionStarted(CastSession session, String sessionId) {
            castSession = session;
            invalidateOptionsMenu();
            startCustomMessageChannel();
        }

        @Override
        public void onSessionStarting(CastSession session) {
            Log.d(TAG, "onSessionStarting");
        }

        @Override
        public void onSessionStartFailed(CastSession session, int error) {
            Log.d(TAG, "onSessionStartFailed: " + error);
        }

        @Override
        public void onSessionEnding(CastSession session) {
        }

        @Override
        public void onSessionResuming(CastSession session, String sessionId) {
        }

        @Override
        public void onSessionResumeFailed(CastSession session, int error) {
        }

        @Override
        public void onSessionSuspended(CastSession session, int reason) {
        }
    }


    // create a custom channel to send data message to cast receiver
    private void startCustomMessageChannel() {

        if (castSession != null && rollDiceChannel == null) {
            rollDiceChannel = new RollDiceChannel(getString(R.string.namespace));

            try {
                castSession.setMessageReceivedCallbacks(
                      rollDiceChannel.getNamespace(),
                      rollDiceChannel);
                Log.d(TAG, "Message channel started");
            } catch (IOException e) {
                Log.d(TAG, "Error starting message channel", e);
                rollDiceChannel = null;
            }

            if (diceViewModel.getDice() != null) {
                updateDie(diceViewModel.getDice());
            }
        }
    }


    // closes the custom channel
    private void closeCustomMessageChannel() {
        if (castSession != null && rollDiceChannel != null) {
            try {
                castSession.removeMessageReceivedCallbacks(rollDiceChannel.getNamespace());
                Log.d(TAG, "Message channel closed");
            } catch (IOException e) {
                Log.d(TAG, "Error closing message channel", e);
            } finally {
                rollDiceChannel = null;
            }
        }
    }


    private void cleanupSession() {
        closeCustomMessageChannel();
        castSession = null;
    }


    private void updateDie(Pair<Integer, Integer> dice) {
        Drawable icon1 = ContextCompat.getDrawable(this, diceViewModel.getIconDrawable(dice.first));
        Drawable icon2 = ContextCompat.getDrawable(this, diceViewModel.getIconDrawable(dice.second));

        ImageView die1 = findViewById(R.id.imageViewDie1);
        ImageView die2 = findViewById(R.id.imageViewDie2);

        die1.setImageDrawable(icon1);
        die2.setImageDrawable(icon2);

        String message = String.format("You rolled: %d", dice.first + dice.second);
        TextView textMessage = findViewById(R.id.textMessage);
        textMessage.setText(message);

        String sum = String.format("%d + %d", dice.first, dice.second);
        TextView sumTextView = findViewById(R.id.dice_sum);
        sumTextView.setText(sum);

        // send message to cast receiver when session is available
        if (rollDiceChannel != null && castSession != null) {
            rollDiceChannel.sendDiceRoll(castSession, dice);
        }
    }

    /**
     * Add the Cast Button to toolbar menu
     * <p/>
     * The MediaRouter framework provides a Cast button and a list selection dialog for selecting a
     * route. The MediaRouter framework interfaces with the Cast SDK via a MediaRouteProvider
     * implementation to perform the discovery on behalf of the application.
     * <p/>
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.main, menu);

        // add MediaRoute button for casting
        mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(
              getApplicationContext(),
              menu,
              R.id.media_route_menu_item);

        showIntroductoryOverlay();

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.info_menu) {
            startActivity(new Intent(this, InfoActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {
        castContext.addCastStateListener(castStateListener);
        castContext.getSessionManager().addSessionManagerListener(
              sessionManagerListener,
              CastSession.class);

        if (castSession == null) {
            castSession = castContext.getSessionManager().getCurrentCastSession();
        }

        // register our shake listener
        sensorManager.registerListener(
              diceShaker,
              sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
              SensorManager.SENSOR_DELAY_NORMAL);

        super.onResume();
    }


    @Override
    protected void onPause() {
        castContext.removeCastStateListener(castStateListener);
        castContext.getSessionManager().removeSessionManagerListener(
              sessionManagerListener,
              CastSession.class
        );

        // de-register shake listener
        sensorManager.unregisterListener(diceShaker);

        super.onPause();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanupSession();
    }


    private void showIntroductoryOverlay() {

        // remove if already showing
        if (introductoryOverlay != null) {
            introductoryOverlay.remove();
        }

        // build and display the cast intro overlay
        if ((mediaRouteMenuItem != null) && mediaRouteMenuItem.isVisible()) {

            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    introductoryOverlay = new IntroductoryOverlay.Builder(
                          MainActivity.this, mediaRouteMenuItem)
                          .setTitleText(R.string.introducing_cast)
                          .setOverlayColor(R.color.primary)
                          .setSingleTime()
                          .setOnOverlayDismissedListener(
                                new IntroductoryOverlay.OnOverlayDismissedListener() {
                                    @Override
                                    public void onOverlayDismissed() {
                                        introductoryOverlay = null;
                                    }
                                })
                          .build();

                    introductoryOverlay.show();
                }
            });
        }
    }

}
