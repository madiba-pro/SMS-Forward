package com.enixcoda.smsforward;

import android.telephony.SmsManager;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Forwarder {
    static final int MAX_SMS_LENGTH = 120;

    public static void sendSMS(String number, String content) {
        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> fragments = smsManager.divideMessage(content);
        if (fragments.size() > 1)
            smsManager.sendMultipartTextMessage(number, null, fragments, null, null);
        else
            smsManager.sendTextMessage(number, null, content, null, null);
    }

    public static void forwardViaSMS(String senderNumber, String forwardContent, String forwardNumber,
                                     boolean keepTogether) {
        forwardViaSMS(senderNumber, forwardContent, forwardNumber, keepTogether, null);
    }

    public static void forwardViaSMS(String senderNumber, String forwardContent, String forwardNumber,
                                     boolean keepTogether, String destinationLabel) {
        try {
            for (String message : buildSmsMessages(senderNumber, forwardContent, keepTogether, destinationLabel))
                sendSMS(forwardNumber, message);
        } catch (RuntimeException e) {
            Log.d(Forwarder.class.toString(), e.toString());
        }
    }

    static ArrayList<String> buildSmsMessages(String senderNumber, String forwardContent,
                                              boolean keepTogether) {
        return buildSmsMessages(senderNumber, forwardContent, keepTogether, null);
    }

    static ArrayList<String> buildSmsMessages(String senderNumber, String forwardContent,
                                              boolean keepTogether, String destinationLabel) {
        String forwardPrefix = (destinationLabel == null || destinationLabel.isEmpty())
                ? String.format("From %s:\n", senderNumber)
                : String.format("From %s\nVia %s:\n", senderNumber, destinationLabel);
        ArrayList<String> messages = new ArrayList<>();

        if (!keepTogether && (forwardPrefix + forwardContent)
                .getBytes(StandardCharsets.UTF_8).length > MAX_SMS_LENGTH) {
            messages.add(forwardPrefix);
            messages.add(forwardContent);
        } else {
            messages.add(forwardPrefix + forwardContent);
        }
        return messages;
    }

    public static void forwardViaTelegram(String senderNumber, String message, String targetTelegramID, String telegramToken) {
        forwardViaTelegram(senderNumber, message, targetTelegramID, telegramToken, null);
    }

    public static void forwardViaTelegram(String senderNumber, String message, String targetTelegramID,
                                          String telegramToken, String destinationLabel) {
        new ForwardTaskForTelegram(senderNumber, message, targetTelegramID, telegramToken, destinationLabel).execute();
    }

    public static void forwardViaWeb(String senderNumber, String message, String endpoint) {
        forwardViaWeb(senderNumber, message, endpoint, null);
    }

    public static void forwardViaWeb(String senderNumber, String message, String endpoint, String destinationLabel) {
        new ForwardTaskForWeb(senderNumber, message, endpoint, destinationLabel).execute();
    }
}
