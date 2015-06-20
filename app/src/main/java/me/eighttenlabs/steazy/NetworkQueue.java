package me.eighttenlabs.steazy;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Handles the Network Queue
 * <p/>
 * Created by Ian on 6/18/2015.
 */
public class NetworkQueue {

    private static NetworkQueue mInstance;
    private static Context context;
    private RequestQueue queue;


    public NetworkQueue(Context context) {
        NetworkQueue.context = context;
        queue = getRequestQueue();
    }

    public static synchronized NetworkQueue getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new NetworkQueue(context);
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if (queue == null) {
            queue = Volley.newRequestQueue(context.getApplicationContext());
        }
        return queue;
    }

    public <T> void addtToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }


}
