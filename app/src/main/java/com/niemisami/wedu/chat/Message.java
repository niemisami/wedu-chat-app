package com.niemisami.wedu.chat;

/**
 * Created by Sami on 21.2.2017.
 */

public class Message {

    public static final int TYPE_MESSAGE_QUESTION = 0;
    public static final int TYPE_MESSAGE_OWN = 1;
    public static final int TYPE_MESSAGE_FRIEND = 2;

    public static final int TYPE_LOG = 100;
    public static final int TYPE_ACTION = 101;

    private int mType;
    private String mMessage;
    private String mUsername;

    protected Message() {
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


    public static class Builder {
        protected final int mType;
        protected String mUsername;
        protected String mMessage;

        public Builder(int type) {
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
            Message message = new Message();
            message.mType = mType;
            message.mUsername = mUsername;
            message.mMessage = mMessage;
            return message;
        }
    }
}