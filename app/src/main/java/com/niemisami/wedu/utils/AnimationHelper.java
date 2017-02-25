package com.niemisami.wedu.utils;

import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

/**
 * Created by Sami on 25.2.2017.
 */

public class AnimationHelper {

    public static final int DEFAULT_ANIMATION_DURATION = 300;

    public static void alphaIn(final View animatedView) {
        AlphaAnimation animateAlphaIn = new AlphaAnimation(0f, 1.0f);

        animateAlphaIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                animatedView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        animateAlphaIn.setDuration(DEFAULT_ANIMATION_DURATION);
        animateAlphaIn.setFillAfter(true);
        animatedView.startAnimation(animateAlphaIn);
    }

    public static void alphaOut(final View animatedView) {
        AlphaAnimation animateAlphaOut = new AlphaAnimation(1.0f, 0f);

        animateAlphaOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                animatedView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        animateAlphaOut.setDuration(DEFAULT_ANIMATION_DURATION);
        animateAlphaOut.setFillAfter(true);
        animatedView.startAnimation(animateAlphaOut);
    }

}
