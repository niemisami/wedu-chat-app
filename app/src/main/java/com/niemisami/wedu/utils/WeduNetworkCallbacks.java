package com.niemisami.wedu.utils;

import com.niemisami.wedu.chat.Message;

import org.json.JSONObject;

/**
 * Created by Sami on 10.3.2017.
 */

public interface WeduNetworkCallbacks {

    void fetchBegin();
    void fetchFailed(Exception e);
    /**
     * By default all commands are run on background thread. Be careful
     * */
    void fetchComplete(String data);
}
