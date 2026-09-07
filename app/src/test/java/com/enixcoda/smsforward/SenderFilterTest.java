package com.enixcoda.smsforward;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SenderFilterTest {
    // A stand-in for PhoneNumberUtils.formatNumberToE164: strips formatting and assumes a
    // 10-digit number is a US number missing its country code. Good enough to exercise the
    // filter's matching logic without touching the real Android API (which isn't usable
    // under plain JUnit - see PhoneNumberMatcherTest for the same approach).
    private static final PhoneNumberMatcher.Normalizer TEST_NORMALIZER = number -> {
        String digits = number.replaceAll("[^0-9]", "");
        if (digits.isEmpty())
            return null;
        if (digits.length() == 10)
            digits = "1" + digits;
        return "+" + digits;
    };

    @Test
    public void offModeForwardsEverything() {
        assertTrue(SenderFilter.shouldForward("+15550100", SenderFilter.MODE_OFF, "+15559999", TEST_NORMALIZER));
        assertTrue(SenderFilter.shouldForward("+15550100", null, "+15559999", TEST_NORMALIZER));
        assertTrue(SenderFilter.shouldForward("+15550100", "", "+15559999", TEST_NORMALIZER));
    }

    @Test
    public void whitelistOnlyForwardsListedNumbers() {
        String list = "+15550100, 5550101\n+1 (555) 010-2";

        assertTrue(SenderFilter.shouldForward("+15550100", SenderFilter.MODE_WHITELIST, list, TEST_NORMALIZER));
        assertTrue(SenderFilter.shouldForward("5550101", SenderFilter.MODE_WHITELIST, list, TEST_NORMALIZER));
        assertTrue(SenderFilter.shouldForward("+15550102", SenderFilter.MODE_WHITELIST, list, TEST_NORMALIZER));
        assertFalse(SenderFilter.shouldForward("+15559999", SenderFilter.MODE_WHITELIST, list, TEST_NORMALIZER));
    }

    @Test
    public void blacklistBlocksOnlyListedNumbers() {
        String list = "+15550100";

        assertFalse(SenderFilter.shouldForward("+15550100", SenderFilter.MODE_BLACKLIST, list, TEST_NORMALIZER));
        assertTrue(SenderFilter.shouldForward("+15559999", SenderFilter.MODE_BLACKLIST, list, TEST_NORMALIZER));
    }

    @Test
    public void emptyWhitelistFailsOpenInsteadOfBlockingEverything() {
        assertTrue(SenderFilter.shouldForward("+15550100", SenderFilter.MODE_WHITELIST, "", TEST_NORMALIZER));
        assertTrue(SenderFilter.shouldForward("+15550100", SenderFilter.MODE_WHITELIST, "   \n  ", TEST_NORMALIZER));
    }

    @Test
    public void emptyBlacklistBlocksNothing() {
        assertTrue(SenderFilter.shouldForward("+15550100", SenderFilter.MODE_BLACKLIST, "", TEST_NORMALIZER));
    }

    @Test
    public void matchesAlphanumericSenderIdsCaseInsensitively() {
        // "AMAZON", "HDFC-BK", etc. aren't phone numbers, so TEST_NORMALIZER returns null
        // for them (no digits) and matching falls back to case-insensitive text comparison.
        assertTrue(SenderFilter.shouldForward("AMAZON", SenderFilter.MODE_WHITELIST, "amazon", TEST_NORMALIZER));
        assertFalse(SenderFilter.shouldForward("RANDOM-SHOP", SenderFilter.MODE_WHITELIST, "amazon", TEST_NORMALIZER));
        assertFalse(SenderFilter.shouldForward("AMAZON", SenderFilter.MODE_BLACKLIST, "amazon", TEST_NORMALIZER));
    }
}
