package com.niemisami.wedu.chat;

import static android.R.attr.type;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.T;

/**
 * Created by Sami on 21.2.2017.
 */

public class Message {

    public static final int TYPE_MESSAGE_QUESTION = 1;
    public static final int TYPE_MESSAGE_OWN = 2;
    public static final int TYPE_MESSAGE_FRIEND = 3;

    public static final int TYPE_LOG = 100;
    public static final int TYPE_ACTION = 101;

    private int mType;
    private String mId;
    private String mMessage;
    private String mUsername;

    protected Message(Builder builder) {
        mType = builder.mType;
        mId = builder.mId;
        mMessage = builder.mMessage;
        mUsername = builder.mUsername;
    }

    public int getType() {
        return mType;
    }

    public String getMessage() {
        return mMessage;
    }

    public String getUsername() {
        return mUsername;
    }

    public String getId() {
        return mId;
    }

    public Builder toBuilder() {
        return decorate(newBuilder());
    }

    protected Builder decorate(Builder builder) {
        return builder
                .username(getUsername())
                .message(getMessage());
    }

    protected Builder newBuilder() {
        return new Builder(getId(), getType());
    }


    public static class Builder {
        private final int mType;
        private String mId;
        private String mUsername;
        private String mMessage;


        public Builder(String id, int type) {
            mId = id;
            mType = type;
        }

        public Builder username(String username) {
            mUsername = username;
            return this;
        }

        public Builder message(String message) {
            mMessage = message;
            return this;
        }

        public Message build() {
            return new Message(this);
        }
    }

}