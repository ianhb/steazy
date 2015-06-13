package me.eighttenlabs.steazy;

import android.os.AsyncTask;

import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.Request;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

/**
 * Class to hold all functions related to Soundcloud
 * <p/>
 * Created by Ian on 2/11/2015.
 */
public class SoundCloud {

    public static class Redirect extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                ApiWrapper wrapper = new ApiWrapper(MainActivity.SOUNDCLOUD_CLIENT_ID, MainActivity.SOUNDCLOUD_PRIVATE_ID, null, null);
                HttpResponse response = wrapper.get(Request.to(params[0]));
                JSONObject streamObj = new JSONObject(EntityUtils.toString(response.getEntity()));
                return streamObj.getString("location");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return params[0];
        }
    }

}
