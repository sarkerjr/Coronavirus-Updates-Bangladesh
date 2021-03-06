package com.covid19.bd;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class WidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        //For handling the progressBar
        final RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.appwidget);

        if(isNetworkConnected(context))
            //Make the progressBar visible at the beginning
            remoteViews.setViewVisibility(R.id.loadingIndicator, View.VISIBLE);
        else
            //Set Tap To Refresh to No Internet... if no internet available
            remoteViews.setTextViewText(R.id.tapToRefresh, "No Internet");

        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);

        //If multiple widgets are active, then update them all
        for (int appWidgetId : appWidgetIds) {
            updateWidgetView(context, appWidgetManager, appWidgetId);
        }

        //For showing the progressBar
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(isNetworkConnected(context))
                    //Remove the progressBar after the http request and a delay
                    remoteViews.setViewVisibility(R.id.loadingIndicator, View.GONE);
                else
                    remoteViews.setTextViewText(R.id.tapToRefresh, "Tap To Refresh");

                appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
            }
        }, 1000);
    }

    //Take the http response and update the UI
    static void updateWidgetView(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId){

        //Updating views of widget through RemoteViews
        final RemoteViews[] views = {new RemoteViews(context.getPackageName(), R.layout.appwidget)};

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(context);
        String url ="https://coronabdapi.herokuapp.com/api/?format=json";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Converting response to UTF-8
                        try {
                            response = new String(response.getBytes("ISO-8859-1"), "UTF-8");

                            //Create a JSONObject from the JSON_RESPONSE string
                            JSONObject baseJsonResponse = new JSONObject(response);
                            JSONObject data = baseJsonResponse.getJSONObject("data");

                            //set the views to be updated on the widget
                            views[0] = setViewGroup(views[0], data);

                            //Add tap-to-update option
                            Intent intentSync = new Intent(context, WidgetProvider.class);
                            intentSync.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                            intentSync.putExtra( AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { appWidgetId } );
                            PendingIntent pendingSync = PendingIntent.getBroadcast(context,0, intentSync, PendingIntent.FLAG_UPDATE_CURRENT);
                            views[0].setOnClickPendingIntent(R.id.update_view, pendingSync);

                            //Notify the widget to get updated
                            appWidgetManager.updateAppWidget(appWidgetId, views[0]);

                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Add tap-to-update option when get errors
                Intent intentSync = new Intent(context, WidgetProvider.class);
                intentSync.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                intentSync.putExtra( AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { appWidgetId } );
                PendingIntent pendingSync = PendingIntent.getBroadcast(context,0, intentSync, PendingIntent.FLAG_UPDATE_CURRENT);
                views[0].setOnClickPendingIntent(R.id.update_view, pendingSync);
                appWidgetManager.updateAppWidget(appWidgetId, views[0]);
                Log.e("Volley Error:", String.valueOf(error));
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    //Set all the views through RemoteViews
    static RemoteViews setViewGroup(RemoteViews views, JSONObject data){
        try {
            views.setTextViewText(R.id.new_infected, "নতুনঃ "+data.getString("new_infected"));
            views.setTextViewText(R.id.total_infected, data.getString("total_infected"));
            views.setTextViewText(R.id.new_cured, "নতুনঃ "+data.getString("new_cured"));
            views.setTextViewText(R.id.total_cured, data.getString("total_cured"));
            views.setTextViewText(R.id.new_death, "নতুনঃ "+data.getString("new_death"));
            views.setTextViewText(R.id.total_death, data.getString("total_death"));
            views.setTextViewText(R.id.new_test, "নতুনঃ "+data.getString("new_test"));
            views.setTextViewText(R.id.total_test, data.getString("total_test"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return views;
    }

    //Check if device is connected to a network
    private boolean isNetworkConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }
}
