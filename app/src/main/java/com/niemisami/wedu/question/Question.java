package com.niemisami.wedu.question;

import com.niemisami.wedu.chat.Message;

/**
 * Created by Sami on 23.2.2017.
 */

public class Question extends Message {

    private String mCourseId = "koodikerho-id";
    private String mLecture = "Koodikerho";
    private long mCreated;
    private int mUpvotes;
    private boolean mIsSolved;

    private Question(Builder builder) {
        super(builder);
        //      question.mLecture = mLecture; // Currently testing with only one lecture "Koodikerho"
        mCourseId = builder.mCourseId;
        mUpvotes = builder.mUpvotes;
        mCreated = builder.mCreated;
        mIsSolved = builder.mIsSolved;

    }

    public String getCourseId() {
        return mCourseId;
    }

    public String getLecture() {
        return mLecture;
    }

    public long getCreated() {
        return mCreated;
    }

    public int getUpvotes() {
        return mUpvotes;
    }

    public boolean isSolved() {
        return mIsSolved;
    }


    @Override
    public Builder toBuilder() {
        return (Builder) super.toBuilder();
    }

    @Override
    protected Builder newBuilder() {
        return new Builder(getType());
    }

    protected Builder decorate(Builder builder) {
        super.decorate(builder);
        return builder
                .created(getCreated())
                .lecture(getLecture())
                .courseId(getCourseId())
                .upvotes(getUpvotes());
    }

    public static class Builder extends Message.Builder {
        private String mCourseId;
        private String mLecture;
        private long mCreated;
        private int mUpvotes;
        private boolean mIsSolved;

        public Builder(int type) {
            super(type);
        }

        public Builder courseId(String course) {
            mCourseId = course;
            return this;
        }

        public Builder lecture(String lecture) {
            mLecture = lecture;
            return this;
        }

        public Builder created(long created) {
            mCreated = created;
            return this;
        }

        public Builder upvotes(int upvotes) {
            mUpvotes = upvotes;
            return this;
        }

        public Builder solved(boolean isSolved) {
            mIsSolved = isSolved;
            return this;
        }

        @Override
        public Builder username(String username) {
            return (Builder) super.username(username);
        }

        @Override
        public Builder message(String message) {
            return (Builder) super.message(message);
        }

        @Override
        public Question build() {
            return new Question(this);
        }
    }
}
