package com.njwyt.entity;

public class MessageEvent<T> {

    private int message;
    private T body;

    public MessageEvent(int message, T body) {
        this.message = message;
        this.body = body;
    }

    public int getMessage() {
        return message;
    }

    public T getBody() {
        return body;
    }
}