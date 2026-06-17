package org.telegram.messenger;

import android.content.Intent;

import androidx.annotation.NonNull;


import org.telegram.ui.LaunchActivity;
import org.telegram.ui.WearAuthSheet;

public class WearAuthListenerService {

    public static final String PATH_OFFER = "/tg-wear-auth/offer";
    public static final String PATH_CANCEL = "/tg-wear-auth/cancel";

    public void onMessageReceived(@NonNull Object event) {
    }
}
