package com.honu.dicecast;

import android.util.Log;
import android.util.Pair;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Custom data channel to send/receive messages from receiver app.
 */
public class RollDiceChannel implements Cast.MessageReceivedCallback {

    private static final String TAG = RollDiceChannel.class.getSimpleName();

    private String namespace;

    public RollDiceChannel(String namespace) {
        this.namespace = namespace;
    }

    public void sendDiceRoll(CastSession session, Pair<Integer, Integer> dice) {
        String text = String.format("You rolled (%d, %d): %d", dice.first, dice.second, dice.first + dice.second);

        JSONObject jsonMsg = new JSONObject();
        try {
            jsonMsg.put("text", text);
            jsonMsg.put("die1", dice.first);
            jsonMsg.put("die2", dice.second);

            session.sendMessage(getNamespace(), jsonMsg.toString()).setResultCallback(

                  new ResultCallback<Status>() {

                      @Override
                      public void onResult(Status result) {
                          if (!result.isSuccess()) {
                              Log.e(TAG, "Sending message failed");
                          }
                      }
                  });
        } catch (JSONException e) {
            Log.e(TAG, "Exception creating message", e);
        }
    }

    public String getNamespace() {
        return namespace;
    }

    @Override
    public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
        Log.d(TAG, "onMessageReceived: " + message);
    }
}
