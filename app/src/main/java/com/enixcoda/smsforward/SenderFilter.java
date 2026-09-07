package com.enixcoda.smsforward;

import android.telephony.PhoneNumberUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Decides whether an incoming SMS should be forwarded at all, based on the user's
 * sender allow-list / block-list setting. This runs before any of the SMS/Telegram/Web
 * forward channels, so a filtered-out sender is never forwarded by any of them.
 */
final class SenderFilter {
    static final String MODE_OFF = "off";
    static final String MODE_WHITELIST = "whitelist";
    static final String MODE_BLACKLIST = "blacklist";

    private SenderFilter() {
    }

    /**
     * @param senderNumber     the number the SMS came from
     * @param mode             one of MODE_OFF / MODE_WHITELIST / MODE_BLACKLIST
     * @param rawFilterList    numbers from settings, one per line and/or comma-separated
     * @param defaultCountryIso used to normalize numbers written in different formats before comparing
     */
    static boolean shouldForward(String senderNumber, String mode, String rawFilterList,
                                 String defaultCountryIso) {
        return shouldForward(senderNumber, mode, rawFilterList,
                number -> PhoneNumberUtils.formatNumberToE164(number, defaultCountryIso));
    }

    static boolean shouldForward(String senderNumber, String mode, String rawFilterList,
                                 PhoneNumberMatcher.Normalizer normalizer) {
        if (mode == null || mode.isEmpty() || MODE_OFF.equals(mode))
            return true;

        List<String> filterNumbers = parseNumbers(rawFilterList);

        // An empty list under "only forward from these numbers" would silently forward
        // nothing, which is almost certainly not what the user wants if they haven't
        // filled the list in yet - treat it the same as filtering being off.
        if (filterNumbers.isEmpty())
            return true;

        boolean matchesFilterList = matchesAny(senderNumber, filterNumbers, normalizer);

        if (MODE_WHITELIST.equals(mode))
            return matchesFilterList;
        if (MODE_BLACKLIST.equals(mode))
            return !matchesFilterList;

        return true; // unknown/future mode value: fail open rather than silently drop messages
    }

    private static boolean matchesAny(String senderNumber, List<String> filterNumbers,
                                      PhoneNumberMatcher.Normalizer normalizer) {
        for (String filterNumber : filterNumbers) {
            if (PhoneNumberMatcher.areSame(senderNumber, filterNumber, normalizer))
                return true;
        }
        return false;
    }

    private static List<String> parseNumbers(String rawFilterList) {
        List<String> numbers = new ArrayList<>();
        if (rawFilterList == null)
            return numbers;
        for (String piece : rawFilterList.split("[,\\n]"))
            if (!piece.trim().isEmpty())
                numbers.add(piece.trim());
        return numbers;
    }
}
