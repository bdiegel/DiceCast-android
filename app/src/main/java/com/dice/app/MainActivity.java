package com.dice.app;

/**
 * Sender application following the Android sender tutorial.
 * </p>
 * - code is annotated with comments from this tutorial
 * - each task or sub-task has been numbered to help navigate the code
 * - this example uses a custom channel
 * - @see: https://developers.google.com/cast/docs/android_sender
 * </p>
 *
 * <p/>
 * The Sender application flow:
 * <p/>
 * - Sender app starts MediaRouter device discovery: MediaRouter.addCallback
 * - MediaRouter informs sender app of the route the user selected: MediaRouter.Callback.onRouteSelected
 * - Sender app retrieves CastDevice instance: CastDevice.getFromBundle
 * - Sender app creates a GoogleApiClient: GoogleApiClient.Builder
 * - Sender app connects the GoogleApiClient: GoogleApiClient.connect
 * - SDK confirms that GoogleApiClient is connected: GoogleApiClient.ConnectionCallbacks.onConnected
 * - Sender app launches the receiver app: Cast.CastApi.launchApplication
 * - SDK confirms that the receiver app is connected: ResultCallback<Cast.ApplicationConnectionResult>
 * - Sender app creates a communication channel: Cast.CastApi.setMessageReceivedCallbacks
 * - Sender sends a message to the receiver over the communication channel: Cast.CastApi.sendMessage
 * </p>
 */

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class MainActivity extends ActionBarActivity {

   // application identifier
   private String APPLICATION_ID;

   // starts Cast device discovery
   private MediaRouter mMediaRouter;

   // filters discovery to Cast devices that can launch required receiver
   private MediaRouteSelector mMediaRouteSelector;

   // handles device selections (launches receiver)
   private MyMediaRouterCallback mMediaRouterCallback;

   // Cast device selected by user
   private CastDevice mSelectedDevice;

   // client used to invoke Cast API calls (from Google Play services)
   private GoogleApiClient mApiClient;

   // handles when client is connected/disconnected from service
   private GoogleApiClient.ConnectionCallbacks mConnectionCallbacks;

   // handles failure to connect client to the service
   private GoogleApiClient.OnConnectionFailedListener mConnectionFailedListener;

   // handles application status changes, disconnects and volume changes
   private Cast.Listener mCastClientListener;

   // connection was suspended and is waiting to reconnect (see ConnectionCallbacks)
   private boolean mWaitingForReconnect;

   // application launched successfully
   private boolean mApplicationStarted;

   // custom channel for application
   private RollDiceChannel mDiceRollerChannel;

   // identifies current application's session when connection established with receiver
   private String mSessionId;

   // identifier for source of log messages
   private static final String TAG = MainActivity.class.getSimpleName();

   static Map<Integer, Integer> icons = new HashMap<Integer, Integer>();

   static {
      icons.put(1, R.drawable.dice_1);
      icons.put(2, R.drawable.dice_2);
      icons.put(3, R.drawable.dice_3);
      icons.put(4, R.drawable.dice_4);
      icons.put(5, R.drawable.dice_5);
      icons.put(6, R.drawable.dice_6);
   }


   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);

      // set our application id
      APPLICATION_ID = getString(R.string.app_id);
      Log.d(TAG, APPLICATION_ID);

      // 1.2 obtain instance of MediaRouter to hold onto for lifetime of the sender application
      mMediaRouter = MediaRouter.getInstance(getApplicationContext());

      // 1.3 filter discovery for Cast devices that can launch the associated receiver application
      mMediaRouteSelector = new MediaRouteSelector.Builder()
            .addControlCategory(CastMediaControlIntent.categoryForCast(APPLICATION_ID))
            .build();

      // 2.1 create callback to handle device selection
      mMediaRouterCallback = new MyMediaRouterCallback();

      // button user presses to roll the dice
      Button rollButton = (Button) findViewById(R.id.rollButton);

      // generate two random integers when button is clicked
      rollButton.setOnClickListener(new View.OnClickListener() {

         @Override
         public void onClick(View v) {
            Pair<Integer, Integer> dice = rollDice();

            Resources res = getResources();
            Drawable icon1 = res.getDrawable(icons.get(dice.first));
            Drawable icon2 = res.getDrawable(icons.get(dice.second));

            ImageView die1 = (ImageView) findViewById(R.id.imageViewDie1);
            ImageView die2 = (ImageView) findViewById(R.id.imageViewDie2);

            die1.setImageDrawable(icon1);
            die2.setImageDrawable(icon2);

            String message = String.format("You rolled (%d, %d): %d", dice.first, dice.second, dice.first + dice.second);

            JSONObject jsonMsg = new JSONObject();

            try {
               jsonMsg.put("text", message);
               jsonMsg.put("die1", dice.first);
               jsonMsg.put("die2", dice.second);
            } catch (JSONException e) {
               Log.e(TAG, e.getMessage(), e);
            }

            sendMessage(jsonMsg.toString());

            TextView textMessage = (TextView) findViewById(R.id.textMessage);
            textMessage.setText(message);

            Log.d(TAG, message);
         }
      });
   }

   /**
    * 1. Add the Cast Button:
    * <p/>
    * The MediaRouter framework provides a Cast button and a list selection dialog for selecting a
    * route. The MediaRouter framework interfaces with the Cast SDK via a MediaRouteProvider
    * implementation to perform the discovery on behalf of the application.
    * <p/>
    * There are three ways to support a Cast button:
    * - Using the MediaRouter ActionBar provider: android.support.v7.app.MediaRouteActionProvider
    * - Using the MediaRouter Cast button: android.support.v7.app.MediaRouteButton
    * - Developing a custom UI with the MediaRouter API’s and MediaRouter.Callback
    * <p/>
    * Note: This example uses the MediaRouteActionProvider to add the Cast button to the ActionBar.
   *
    * 1.1 The MediaRouter ActionBar provider needs to be added to the application’s menu hierarchy
    *     defined in XMl. The application Activity needs to extend ActionBarActivity.
    *     @link res/menu/main.xml
    *     @see MainActivity
    *
    * 1.2 The application needs to obtain an instance of the MediaRouter and needs to hold onto
    *     that instance for the lifetime of the sender application:
    *     @see #onCreate(Bundle savedInstanceState)
    *
    * 1.3 The MediaRouter needs to filter discovery for Cast devices that can launch the
    *     receiver application associated with the sender app. For that a MediaRouteSelector
    *     is created by calling MediaRouteSelector.Builder
    *     @see #onCreate(Bundle savedInstanceState)
    *
    * 1.4 Assign the MediaRouteSelector to the MediaRouteActionProvide in the ActionBar menu.
    *     Now MediaRouter will use the selector to filter the devices that are displayed to the
    *     user when the Cast button in the ActionBar is pressed.
    */
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {

      // Inflate the menu; this adds items to the action bar if it is present.
      getMenuInflater().inflate(R.menu.main, menu);

      // 1.4 assign handler for route selections
      MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
      MediaRouteActionProvider mediaRouteActionProvider =
            (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
      mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);

      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      // Handle action bar item clicks here. The action bar will
      // automatically handle clicks on the Home/Up button, so long
      // as you specify a parent activity in AndroidManifest.xml.
      int id = item.getItemId();

      if (id == R.id.action_settings) {
         return true;
      }
      return super.onOptionsItemSelected(item);
   }

   @Override
   protected void onResume() {
      super.onResume();

      // 2.2 assign callback when the application Activity is active
      mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
            MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
   }

   @Override
   protected void onPause() {

      // 2.3 remove callback when the Activity goes into the background:
      if (isFinishing()) {
         mMediaRouter.removeCallback(mMediaRouterCallback);
      }
      super.onPause();
   }

   /**
    * 3 Launch the receiver application on the user selected device
    *
    * Once the application knows which Cast device the user selected, the sender application can
    * launch the receiver application on that device.
    *
    * The Cast SDK API’s are invoked using GoogleApiClient. A GoogleApiClient instance is created
    * using the GoogleApiClient.Builder and requires various callbacks discussed later.
    *
    * 3.1 Create GoogleApiClient for invoking Cast SDK APIs
    *     @see MainActivity#mApiClient
    *
    * 3.2 The application can then establish a connection using the GoogleApiClient instance
    *     @see ConnectionCallbacks#onConnected(Bundle)
    *
    * 3.3 The application needs to declare GoogleApiClient.ConnectionCallbacks and
    *     GoogleApiClient.OnConnectionFailedListener callbacks to be informed of the connection
    *     status. All of the Google Play services callbacks run on the main UI thread. Once the
    *     connection is confirmed, the application can launch the application by specifying the
    *     application ID issued for your app upon Registration.
    *     @see ConnectionCallbacks
    *
    * 3.4 If GoogleApiClient.ConnectionCallbacks.onConnectionSuspended is invoked when the client
    *     is temporarily in a disconnected state, your application needs to track the state, so
    *     that if GoogleApiClient.ConnectionCallbacks.onConnected is subsequently invoked when
    *     the connection is established again, the application should be able to distinguish this
    *     from the initial connected state. It is important to re-create any channels when the
    *     connection is re-established.
    *     @see ConnectionFailedListener
    *
    * 3.5 The Cast.Listener callbacks are used to inform the sender application about receiver
    *     application events.
    *     @see MainActivity#mCastClientListener
    *
    *
    * Note: In addition to Cast, Google Play Services includes:
    *       Wallet, Plus. Address, Google+, Games, Drive, Maps, Location, Ads, ...
    */
   private void launchReceiver() {

      // 3.5 handle receiver application events (status changes, volume changes and disconnects)
      mCastClientListener = new Cast.Listener() {

         @Override
         public void onApplicationDisconnected(int errorCode) {
            teardown();
         }
      };

      // Create the API configuration parameters for Cast. Specify the selected Cast device
      // returned by the MediaRouteProvider and a listener for Cast events
      Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
            .builder(mSelectedDevice, mCastClientListener);

      // 3.3 Create callback listener to handle when client is connected/disconnected
      mConnectionCallbacks = new ConnectionCallbacks();

      // 3.4 Create callback listner to handle failures to connect client to the service
      mConnectionFailedListener = new ConnectionFailedListener();

      // 3.1 Use the builder to create a the API client configured for Cast
      mApiClient = new GoogleApiClient.Builder(this)
            .addApi(Cast.API, apiOptionsBuilder.build())
            .addConnectionCallbacks(mConnectionCallbacks)
            .addOnConnectionFailedListener(mConnectionFailedListener)
            .build();

      // 3.2 Establish a connection (asynchronously). The client is not considered
      //     connected until the onConnected(Bundle) callback has been called.
      mApiClient.connect();
   }

   /**
    * 4.3 Sends receiver messages using our custom channel
    * <p/>
    * Once the custom channel is created, the sender can use that to send String messages
    * to the receiver over that channel:
    * <p/>
    * The application can encode JSON messages into a String, if needed, and then decode the
    * JSON String in the receiver.
    * <p/>
    * RemoteMediaPlayer: The Google Cast SDK supports a media channel to play media on a
    * receiver application. The media channel has a well-known namespace of
    * urn:x-cast:com.google.cast.media.
    *
    * @param message
    */
   private void sendMessage(String message) {
      if (mApiClient != null && mDiceRollerChannel != null) {
         try {
            Cast.CastApi.sendMessage(mApiClient, mDiceRollerChannel.getNamespace(), message)
                  .setResultCallback(

                        new ResultCallback<Status>() {
                           @Override
                           public void onResult(Status result) {
                              if (!result.isSuccess()) {
                                 Log.e(TAG, "Sending message failed");
                              }
                           }
                        }
                  );
         } catch (Exception e) {
            Log.e(TAG, "Exception while sending message", e);
         }
      }
   }

   /**
    * Tear down the connection to the receiver
    *
    * Error handling: It is very important for sender applications to handle all error callbacks
    * and decide the best response for each stage of the Cast life cycle. The application can
    * display error dialogs to the user or it can decide to tear down the connection to the
    * receiver. Tearing down the connection has to be done in a particular sequence
    */
   private void teardown() {
      Log.d(TAG, "teardown");
      if (mApiClient != null) {
         if (mApplicationStarted) {
            if (mApiClient.isConnected()) {
               try {
                  Cast.CastApi.stopApplication(mApiClient, mSessionId);
                  if (mDiceRollerChannel != null) {
                     Cast.CastApi.removeMessageReceivedCallbacks(
                           mApiClient,
                           mDiceRollerChannel.getNamespace());
                     mDiceRollerChannel = null;
                  }
               } catch (IOException e) {
                  Log.e(TAG, "Exception while removing channel", e);
               }
               mApiClient.disconnect();
            }
            mApplicationStarted = false;
         }
         mApiClient = null;
      }
      mSelectedDevice = null;
      mWaitingForReconnect = false;
      mSessionId = null;
   }

   /**
    * Generate a pair of random integers between 1 and 6.
    */
   private Pair<Integer, Integer> rollDice() {
      Random random = new Random();
      int x1 = random.nextInt(6) + 1;
      int x2 = random.nextInt(6) + 1;

      Pair<Integer, Integer> roll = new Pair(x1, x2);
      return roll;
   }

   /**
    * Restoring sessions
    *
    * According to the UX Guidelines, if the sender application becomes disconnected from the
    * media route, such as when the user or the operating system kills the application without
    * the user first disconnecting from the Cast device, then the application must restore the
    * session with the receiver when the sender application starts again.
    */
   private void reconnectChannels() {
      // TODO:
   }

   /**
    * 2. Handling device selection
    *
    * 2.1 When the user selects a device from the Cast button device list, the application is
    *     informed of the selected device by extending MediaRouter.Callback.
    *     @see MyMediaRouterCallback
    *
    * 2.2 The application needs to trigger the discovery of devices by adding MediaRouter.Callback
    *     to the MediaRouter instance. Typically this callback is assigned when the application
    *     Activity is active and then removed when the Activity goes into the background:
    *     @see #onResume()
    *     @see #onPause()
    *
    * The selected device will be used to launch the receiver.
    */
   private class MyMediaRouterCallback extends MediaRouter.Callback {

      @Override
      public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
         mSelectedDevice = CastDevice.getFromBundle(info.getExtras());
         String routeId = info.getId();
         launchReceiver();
      }

      @Override
      public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
         teardown();
         mSelectedDevice = null;
      }
   }


   private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {

      /**
       * Invoked asynchronously when the connect request has successfully completed (after client
       * calling connect()). After this callback, the application can make requests on other
       * methods provided by the client and expect that no user intervention is required to call
       * methods that use account and scopes provided to the client constructor.
       */
      @Override
      public void onConnected(Bundle connectionHint) {
         if (mWaitingForReconnect) {
            mWaitingForReconnect = false;
            reconnectChannels();
         } else {
            try {
               Cast.CastApi.launchApplication(mApiClient, APPLICATION_ID, false)
                     .setResultCallback(
                           new ResultCallback<Cast.ApplicationConnectionResult>() {

                              @Override
                              public void onResult(Cast.ApplicationConnectionResult result) {
                                 Status status = result.getStatus();
                                 if (status.isSuccess()) {
                                    ApplicationMetadata applicationMetadata =
                                          result.getApplicationMetadata();
                                    mSessionId = result.getSessionId();
                                    String applicationStatus = result.getApplicationStatus();
                                    boolean wasLaunched = result.getWasLaunched();

                                    mApplicationStarted = true;

                                    // 4.2 Create the channel instance when you get the
                                    //     callback notification that the connection
                                    //     was successfully established.
                                    mDiceRollerChannel = new RollDiceChannel();

                                    try {
                                       Cast.CastApi.setMessageReceivedCallbacks(mApiClient,
                                             mDiceRollerChannel.getNamespace(),
                                             mDiceRollerChannel);
                                    } catch (IOException e) {
                                       Log.e(TAG, "Exception while creating channel", e);
                                    }

                                 } else {
                                    teardown();
                                 }
                              }
                           }
                     );

            } catch (Exception e) {
               Log.e(TAG, "Failed to launch application", e);
            }
         }
      }

      @Override
      public void onConnectionSuspended(int cause) {
         mWaitingForReconnect = true;
      }
   }

   private class ConnectionFailedListener implements
         GoogleApiClient.OnConnectionFailedListener {
      @Override
      public void onConnectionFailed(ConnectionResult result) {
         teardown();
      }
   }

   /**
    * 4 Define a Custom Channel
    * <p/>
    * 4.1 For the sender application to communicate with the receiver application, a custom channel
    *     needs to be created. The sender can use the custom channel to send String messages to the
    *     receiver. Each custom channel is defined by a unique namespace and must start with the
    *     prefix urn:x-cast:,
    *
    * 4.2 Once the sender application is connected to the receiver application, the custom channel
    *     can be created using Cast.CastApi.setMessageReceivedCallbacks.
    *     @see ConnectionCallbacks#onConnected(android.os.Bundle)
    *
    * 4.3 Once the custom channel is created, the sender can use that to send String messages to
    *     the receiver over that channel:
    *     @see #sendMessage(String)
    */
   private class RollDiceChannel implements Cast.MessageReceivedCallback {

      public String getNamespace() {
         return getString(R.string.namespace);
      }

      @Override
      public void onMessageReceived(CastDevice castDevice, String namespace,
                                    String message) {
         Log.d(TAG, "onMessageReceived: " + message);
      }
   }
}
