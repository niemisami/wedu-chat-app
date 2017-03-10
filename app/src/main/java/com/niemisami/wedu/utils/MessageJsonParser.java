package com.niemisami.wedu.utils;

import android.util.Log;

import com.niemisami.wedu.question.Question;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static android.content.ContentValues.TAG;

/**
 * Created by Sami on 10.3.2017.
 */

public class MessageJsonParser {

    public static Question parseQuestion(JSONObject data) throws NullPointerException {
        int type;
        String username;
        String message;
        String courseId;
        String id;
        long created;
        int upvotes;
        boolean isSolved;
        try {

            if (data.getInt("type") == Question.TYPE_MESSAGE_QUESTION) {

                Log.d(TAG, "run: " + data.toString());
                type = Question.TYPE_MESSAGE_QUESTION;
                username = data.getString("user");
                message = data.getString("message");
                id = data.getString("_id");
                created = data.getLong("created");
                courseId = data.getString("course");

                JSONArray upvotedUsers = data.getJSONObject("grade").getJSONArray("upvotes");
                JSONArray downvotedUsers = data.getJSONObject("grade").getJSONArray("downvotes");
                upvotes = upvotedUsers.length() - downvotedUsers.length();

                isSolved = data.getBoolean("solved");

            } else {
                throw new NullPointerException("JSON didn't contain Wedu Question type");
            }

        } catch (JSONException e) {
            throw new NullPointerException("Failed to parse Question JSON");
        }

        return new Question.Builder(id, type)
                .message(message)
                .username(username)
                .courseId(courseId)
                .solved(isSolved)
                .created(created)
                .upvotes(upvotes)
                .build();


    }
}
