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

    private Question() {
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

    public static class Builder extends Message.Builder {
        private String mCourseId;
        private String mLecture;
        private long mCreated;
        private int mUpvotes;
        private boolean mIsSolved;


        public Builder(int type) {
            super(type);
        }

        public Question.Builder courseId(String course) {
            mCourseId = course;
            return this;
        }

        public Question.Builder lecture(String lecture) {
            mLecture = lecture;
            return this;
        }

        public Question.Builder created(long created) {
            mCreated = created;
            return this;
        }

        public Question.Builder upvote() {
            mUpvotes++;
            return this;
        }

        public Question.Builder downvote() {
            mUpvotes--;
            return this;
        }

        public Question.Builder solved(boolean isSolved) {
            mIsSolved = isSolved;
            return this;
        }

        public Question build() {
            Question question = new Question();
            question.mCourseId = mCourseId;
//            question.mLecture = mLecture; // Currently testing with only one lecture "Koodikerho"
            question.mUpvotes = mUpvotes;
            question.mCreated = mCreated;
            question.mIsSolved = mIsSolved;

            return question;
        }
    }
}
