package com.niemisami.wedu.utils;

import android.accounts.NetworkErrorException;
import android.net.Uri;

import com.niemisami.wedu.question.Question;

import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.functions.Function;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by sakrnie on 26.6.2017.
 */

public class MessageApiService {

    private static final String SERVER_LOCAL_API_URL = "http://10.0.2.2:3000/";
    private static final String SERVER_HEROKU_API_URL = "https://hidden-everglades-97180.herokuapp.com/";
    private static final String ENDPOINT_QUESTIONS = "message/getQuestions";
    private static final String ENDPOINT_QUESTION_INFO = "message/getMessage";
    private static final String ENDPOINT_QUESTION_THREAD = "message/getMessages";
    public static final String SERVER_API_URL = SERVER_LOCAL_API_URL;

    public static Single<Question> getQuestionInfo(String mQuestionId) {
        return fetchWeduData(ENDPOINT_QUESTION_INFO + "/" + mQuestionId)
                .map(new Function<Response, Question>() {
                    @Override
                    public Question apply(Response response) throws Exception {
                        try {
                            String responseBodyString = response.body().string();
                            response.close();
                            return MessageJsonParser.parseQuestion(new JSONObject(responseBodyString));
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                });
    }

    /**
     * Each question has a thread of answers. These message are actually same objects
     * as questions so eventually answers might have more nested messages
     */
    public static Single<List<Question>> getQuestionThread(String mQuestionId) {
        return fetchWeduData(ENDPOINT_QUESTION_THREAD + "/" + mQuestionId)
                .map(new Function<Response, List<Question>>() {
                    @Override
                    public List<Question> apply(Response response) throws Exception {
                        try {
                            String responseBodyString = response.body().string();
                            response.close(); // Close the response to avoid memory leaks
                            return MessageJsonParser.parseMessageList(new JSONObject(responseBodyString));
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                        return new ArrayList<>();
                    }
                });
    }

    /**
     * Fetch questions from the API and convert string response into a list of Question object
     */
    public static Single<List<Question>> getQuestions() {
        return fetchWeduData(ENDPOINT_QUESTIONS)
                .map(new Function<Response, List<Question>>() {
                    @Override
                    public List<Question> apply(Response response) throws Exception {
                        try {
                            String responseBodyString = response.body().string();
                            response.close(); // Close the response to avoid memory leaks
                            return MessageJsonParser.parseQuestionList(responseBodyString);
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                        return new ArrayList<>();
                    }
                });
    }

    private static Single<Response> fetchWeduData(String requestEndpoint) {
        final OkHttpClient client = new OkHttpClient();
        Uri builtUri = Uri.parse(SERVER_API_URL).buildUpon()
                .appendEncodedPath(requestEndpoint)
                .build();
        URL url = null;
        try {
            url = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        final Request request = new Request.Builder()
                .url(url)
                .build();

        return Single.create(new SingleOnSubscribe<Response>() {
            @Override
            public void subscribe(SingleEmitter<Response> emitter) throws Exception {
                try {
                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        emitter.onSuccess(response);
                    } else {
                        emitter.onError(new NetworkErrorException("Couldn't load data from the server"));
                    }
                } catch (IOException e) {
                    emitter.onError(new NetworkErrorException("Couldn't load data from the server"));
                }
            }
        });
    }

}
