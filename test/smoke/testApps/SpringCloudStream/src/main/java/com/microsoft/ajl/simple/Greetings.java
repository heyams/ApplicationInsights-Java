package com.microsoft.ajl.simple;

public class Greetings {

    private final long timestamp;
    private final String message;

    public Greetings(long timestamp, String message) {
        this.timestamp = timestamp;
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }
}
