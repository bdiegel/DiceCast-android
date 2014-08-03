package com.honu.dicecast;

/**
 * Sender application using the CastCompanionLibrary
 */

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.common.api.Status;
import com.google.sample.castcompanionlibrary.cast.BaseCastManager;
import com.google.sample.castcompanionlibrary.cast.DataCastManager;
import com.google.sample.castcompanionlibrary.cast.callbacks.DataCastConsumerImpl;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class MainActivity extends ActionBarActivity {

   // application identifier
   private static String APPLICATION_ID;

   // custom channel for application
   private static RollDiceChannel mDiceRollerChannel;

   // identifier for source of log messages
   private static final String TAG = MainActivity.class.getSimpleName();

   // sensor manager
   private SensorManager mSensorManager;

   // shake event detector
   private DiceShaker mDiceShaker;

   // cast manager
   private static DataCastManager mCastMgr;

   // dice images
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

      // check that Google Play services is available and correct version
      BaseCastManager.checkGooglePlayServices(this);

      // set our application id
      APPLICATION_ID = getString(R.string.app_id);
      Log.d(TAG, APPLICATION_ID);

      // create data channel
      mDiceRollerChannel = new RollDiceChannel();

      // initialize the cast manager
      createCastManager(getApplicationContext());

      // button user presses to roll the dice
      Button rollButton = (Button) findViewById(R.id.rollButton);

      // generate two random integers when button is clicked
      rollButton.setOnClickListener(new View.OnClickListener() {

         @Override
         public void onClick(View v) {
            handleDiceRoll();
         }
      });

      // create dice shake listener
      mDiceShaker = new DiceShaker(new IDiceShakeListener() {
         @Override
         public void onShake() {
            handleDiceRoll();
         }
      });

      // register dice shake listener fpr accelerometer events
      mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
      mSensorManager.registerListener(mDiceShaker,
            mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
               SensorManager.SENSOR_DELAY_NORMAL);
   }

   private static DataCastManager createCastManager(Context ctx) {
      if (null == mCastMgr) {
         mCastMgr = DataCastManager.initialize(ctx, APPLICATION_ID, mDiceRollerChannel.getNamespace());
         mCastMgr.incrementUiCounter();
      }
      mCastMgr.setContext(ctx);

      return mCastMgr;
   }

   private void handleDiceRoll() {
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

   /**
    * Add the Cast Button
    * <p/>
    * The MediaRouter framework provides a Cast button and a list selection dialog for selecting a
    * route. The MediaRouter framework interfaces with the Cast SDK via a MediaRouteProvider
    * implementation to perform the discovery on behalf of the application.
    * <p/>
    */
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {

      // Inflate the menu; this adds items to the action bar if it is present.
      getMenuInflater().inflate(R.menu.main, menu);

      // Add the MediaRouter via our cast manager
      mCastMgr.addMediaRouterButton(menu, R.id.media_route_menu_item);

      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      // Handle action bar item clicks here. The action bar will
      // automatically handle clicks on the Home/Up button, so long
      // as you specify a parent activity in AndroidManifest.xml.
      int id = item.getItemId();

      if (id == R.id.info_menu) {
         startActivity(new Intent(this, InfoActivity.class));
      }
      return super.onOptionsItemSelected(item);
   }

   @Override
   protected void onResume() {
      super.onResume();

      mCastMgr.incrementUiCounter();

      // register our shake listener
      mSensorManager.registerListener(mDiceShaker,
            mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL);
   }

   @Override
   protected void onPause() {

      mCastMgr.decrementUiCounter();

      // de-register shake listener
      mSensorManager.unregisterListener(mDiceShaker);

      super.onPause();
   }

   /**
    * Send receiver string messages (JSON) using our custom channel
    *
    * @param message
    */
   private void sendMessage(String message) {
      try {
         mCastMgr.sendDataMessage(message, mDiceRollerChannel.getNamespace());
         } catch (Exception e) {
            Log.e(TAG, "Exception while sending message", e);
         }
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
    * Custom Channel to send messages between client and receiver
    */
   private class RollDiceChannel extends DataCastConsumerImpl {

      public String getNamespace() {
         return getString(R.string.namespace);
      }

      @Override
      public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
         Log.d(TAG, "onMessageReceived: " + message);
      }

      @Override
      public void onMessageSendFailed(Status status) {
         Log.d(TAG, "onMessageSendFailed: " + status);
      }
   }

}
