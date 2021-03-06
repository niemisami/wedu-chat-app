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

import static android.content.ContentValues.TAG;

/**
 * Created by Sami on 10.3.2017.
 */

public class MessageJsonParser {


    public static JSONObject parseStringToJSON(String data) {
        try {
            return new JSONObject(data);
        } catch (JSONException e) {
            Log.w(TAG, "parseStringToJSON: ", e);
        }
        return null;
    }

    public static List<Question> parseQuestionList(String data) throws NullPointerException {
        if (data == null) {
            throw new NullPointerException("No data provided");
        }
        List<Question> questions = new ArrayList<>();
        JSONArray questionJsons = null;
        try {
            questionJsons = new JSONArray(data);
        } catch (JSONException e) {
            throw new NullPointerException("Error parsing questions JSON");
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

    public static int parseMessageType(JSONObject data) {
        int type = -1;

        try {
            type = data.getInt("type");
        } catch (JSONException e) {
            Log.e(TAG, "parseQuestion: ", e);
            throw new NullPointerException("Failed to parse message type");
        }
        return type;

    }

    public static List<Question> parseMessageList(JSONObject data) throws NullPointerException {
        if (data == null) {
            throw new NullPointerException("Data don't have messages");
        }

        JSONArray array = null;
        try {
            array = data.getJSONObject("thread").getJSONArray("messages");


            List<Question> messages = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                messages.add(parseQuestion(array.getJSONObject(i)));
            }

            return messages;

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
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
