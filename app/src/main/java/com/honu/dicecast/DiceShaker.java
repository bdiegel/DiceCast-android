package com.honu.dicecast;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class DiceShaker implements SensorEventListener {

   // Minimum acceleration needed to register as a shake
   private static final int MIN_ACCELERATION = 5;

   // Minimum number of movements to register as a shake
   private static final int MIN_MOVEMENTS = 3;

   // Arrays to store gravity and linear acceleration values
   private float[] mGravity = {0.0f, 0.0f, 0.0f};
   private float[] mLinearAcceleration = {0.0f, 0.0f, 0.0f};

   // Indexes for x, y, and z values
   private static final int X = 0;
   private static final int Y = 1;
   private static final int Z = 2;

   // OnShakeListener that will be notified when the shake is detected
   private IDiceShakeListener mShakeListener;

   // Initialized to -1 to suppress the very first event when values are initialized
   int moveCount = -1;

   // Constructor that sets the shake listener
   public DiceShaker(IDiceShakeListener shakeListener) {
      mShakeListener = shakeListener;
   }

   @Override
   public void onSensorChanged(SensorEvent event) {
      // Update current values
      setCurrentAcceleration(event);

      // Get the max linear acceleration in any direction
      float maxLinearAcceleration = getMaxCurrentLinearAcceleration();

      // Check if the acceleration is greater than our minimum threshold
      if (maxLinearAcceleration > MIN_ACCELERATION) {
         moveCount++;

         // Enforce a minimum number of movements
         if (moveCount > MIN_MOVEMENTS) {
            mShakeListener.onShake();
            resetShakeDetection();
         }
      }
   }

   @Override
   public void onAccuracyChanged(Sensor sensor, int accuracy) {
      // Intentionally blank
   }

   private void setCurrentAcceleration(SensorEvent event) {
      final float alpha = 0.8f;

      // Gravity components of x, y, and z acceleration
      mGravity[X] = alpha * mGravity[X] + (1 - alpha) * event.values[X];
      mGravity[Y] = alpha * mGravity[Y] + (1 - alpha) * event.values[Y];
      mGravity[Z] = alpha * mGravity[Z] + (1 - alpha) * event.values[Z];

      // Linear acceleration along the x, y, and z axes (gravity effects removed)
      mLinearAcceleration[X] = event.values[X] - mGravity[X];
      mLinearAcceleration[Y] = event.values[Y] - mGravity[Y];
      mLinearAcceleration[Z] = event.values[Z] - mGravity[Z];
   }

   private float getMaxCurrentLinearAcceleration() {
      // Start by setting the value to the x value
      float maxLinearAcceleration = mLinearAcceleration[X];

      // Check if the y value is greater
      if (mLinearAcceleration[Y] > maxLinearAcceleration) {
         maxLinearAcceleration = mLinearAcceleration[Y];
      }

      // Check if the z value is greater
      if (mLinearAcceleration[Z] > maxLinearAcceleration) {
         maxLinearAcceleration = mLinearAcceleration[Z];
      }

      // Return the greatest value
      return maxLinearAcceleration;
   }

   private void resetShakeDetection() {
      moveCount = 0;
   }

}
