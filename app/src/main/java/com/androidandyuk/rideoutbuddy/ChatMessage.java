package com.androidandyuk.rideoutbuddy;

/**
 * Created by AndyCr15 on 15/07/2017.
 */

public class ChatMessage {
    String ID;
    String name;
    String message;
    Long timestamp;

    public ChatMessage(String ID, String name, String message) {
        this.name = name;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public ChatMessage(String ID, String name, String message, String timestamp) {
        this.ID = ID;
        this.name = name;
        this.message = message;
        this.timestamp = Long.parseLong(timestamp);
    }
}
