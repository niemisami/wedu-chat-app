package com.niemisami.wedu.utils;

import android.util.Log;

import com.niemisami.wedu.chat.Message;
import com.niemisami.wedu.question.Question;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.R.attr.data;
import static android.content.ContentValues.TAG;

/**
 * Created by Sami on 10.3.2017.
 */

public class MessageJsonParser {

    public static List<Question> parseQuestionList(String data) throws NullPointerException {
        if (data == null) {
            throw new NullPointerException("Error parsing questions JSON");
        }
        List<Question> questions = new ArrayList<>();
        JSONArray questionJsons = null;
        try {
            questionJsons = new JSONArray(data);
        } catch (JSONException e) {
            throw new NullPointerException("parseQestionsList");
        }

        for (int i = 0; i < questionJsons.length(); i++) {
            try {
                questions.add(parseQuestion(questionJsons.getJSONObject(i)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return questions;
    }

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

            if (data.getInt("type") == Question.TYPE_MESSAGE_QUESTION || data.getInt("type") == Message.TYPE_MESSAGE_OWN) {

                Log.d(TAG, "run: " + data.toString());
                type = data.getInt("type");
                username = data.getString("user");
                message = data.getString("message");
                id = data.getString("_id");

                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                Date date = format.parse(data.getString("created"));
                created = date.getTime();

                courseId = data.getString("course");

                JSONArray upvotedUsers = data.getJSONObject("grade").getJSONArray("upvotes");
                JSONArray downvotedUsers = data.getJSONObject("grade").getJSONArray("downvotes");
                upvotes = upvotedUsers.length() - downvotedUsers.length();

                isSolved = data.getBoolean("solved");

            } else {
                throw new NullPointerException("JSON didn't contain Wedu Question type");
            }

        } catch (JSONException e) {
            Log.e(TAG, "parseQuestion: ", e);
            throw new NullPointerException("Failed to parse Question JSON");
        } catch (ParseException e) {
            e.printStackTrace();
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
