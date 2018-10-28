package com.honu.dicecast;

import android.support.annotation.DrawableRes;
import android.util.Pair;
import android.util.SparseIntArray;

import java.util.Random;


public class DiceViewModel {

    private Pair<Integer, Integer> dice;

    private static SparseIntArray icons = new SparseIntArray();

    static {
        icons.put(1, R.drawable.dice_1);
        icons.put(2, R.drawable.dice_2);
        icons.put(3, R.drawable.dice_3);
        icons.put(4, R.drawable.dice_4);
        icons.put(5, R.drawable.dice_5);
        icons.put(6, R.drawable.dice_6);
    }

    public Pair<Integer, Integer> getDice() {
        return dice;
    }

    public Pair<Integer, Integer> rollDice() {
        dice = roll();
        return dice;
    }

    public @DrawableRes int getIconDrawable(int value) {
        return icons.get(value);
    }

    /**
     * Generate a pair of random integers between 1 and 6.
     */
    private Pair<Integer, Integer> roll() {
        Random random = new Random();
        int x1 = random.nextInt(6) + 1;
        int x2 = random.nextInt(6) + 1;

        return new Pair<Integer, Integer>(x1, x2);
    }
}
