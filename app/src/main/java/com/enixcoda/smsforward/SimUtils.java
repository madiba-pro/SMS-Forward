package com.enixcoda.smsforward;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

/**
 * Identifies which of the device's own phone lines (SIM / subscription) an incoming
 * SMS was received on. This is the "destination" side of a forward: useful on dual-SIM
 * phones so the forwarded message makes clear which of your numbers the text came in on.
 * <p>
 * Every lookup here is best-effort: phone-identity permissions are optional, carriers
 * frequently withhold the number, and the extras Android puts on the SMS_RECEIVED intent
 * to identify the subscription are not fully standardized across OEMs/versions. Any
 * failure degrades gracefully to a less specific label, or to {@code null}.
 */
class SimUtils {

    // Undocumented but widely-used extras older/OEM broadcasts have used to carry the
    // subscription id before SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX became public API.
    private static final String[] LEGACY_SUBSCRIPTION_EXTRAS = {
            "subscription", "simId", "simSlot", "slot", "slot_id", "simSlotIndex"
    };

    /** Returns a human-readable label like "SIM 1 (Carrier) +1 555 0100", or null if unknown. */
    static String getDestinationLabel(Context context, Intent intent) {
        int subscriptionId = resolveSubscriptionId(intent);
        if (subscriptionId == SubscriptionManager.INVALID_SUBSCRIPTION_ID)
            return null;

        SubscriptionInfo info = getSubscriptionInfo(context, subscriptionId);
        if (info == null)
            return null;

        String number = getPhoneNumber(context, subscriptionId, info);
        CharSequence carrierName = info.getDisplayName();
        int humanSlot = info.getSimSlotIndex() + 1; // 1-indexed for display

        StringBuilder label = new StringBuilder("SIM ").append(humanSlot);
        if (carrierName != null && carrierName.length() > 0)
            label.append(" (").append(carrierName).append(")");
        if (number != null && !number.trim().isEmpty())
            label.append(" ").append(number.trim());
        return label.toString();
    }

    private static int resolveSubscriptionId(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            int subId = intent.getIntExtra(SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID)
                return subId;
        }
        for (String key : LEGACY_SUBSCRIPTION_EXTRAS) {
            int subId = intent.getIntExtra(key, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID)
                return subId;
        }
        return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    private static SubscriptionInfo getSubscriptionInfo(Context context, int subscriptionId) {
        if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
            return null;
        try {
            SubscriptionManager subscriptionManager = context.getSystemService(SubscriptionManager.class);
            if (subscriptionManager == null)
                return null;
            return subscriptionManager.getActiveSubscriptionInfo(subscriptionId);
        } catch (SecurityException e) {
            return null;
        }
    }

    private static String getPhoneNumber(Context context, int subscriptionId, SubscriptionInfo info) {
        if (context.checkSelfPermission(Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED)
            return null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                SubscriptionManager subscriptionManager = context.getSystemService(SubscriptionManager.class);
                if (subscriptionManager != null) {
                    String number = subscriptionManager.getPhoneNumber(subscriptionId);
                    if (number != null && !number.isEmpty())
                        return number;
                }
            } else {
                //noinspection deprecation
                String number = info.getNumber();
                if (number != null && !number.isEmpty())
                    return number;
            }
        } catch (SecurityException | IllegalStateException ignored) {
            // Carrier withheld the number, or the permission isn't actually usable on this device.
        }
        return null;
    }
}
