package com.yberg.android.flock;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Viktor on 2016-10-31.
 */

public class FavoriteDialog extends DialogFragment {

    private static final String TAG = "FLOCK/FavoriteDialog";

    private static final String ADD_FAVORITE_URL = MapActivity.BASE_URL + "family/_id/addFavorite";

    private LatLng position;
    private RequestQueue mRequestQueue;
    private View view;
    private SharedPreferences mPrefs;
    private TextView mNewFavoriteName;
    private TextView mNewFavoriteRadius;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        position = getArguments().getParcelable("position");

        // Volley setup
        mRequestQueue = Volley.newRequestQueue(getActivity());

        mPrefs = getActivity().getSharedPreferences("com.yberg.android.flock", Context.MODE_PRIVATE);
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.dialog_favorite, null);

        mNewFavoriteName = (TextView) view.findViewById(R.id.posLat);
        mNewFavoriteRadius = (TextView) view.findViewById(R.id.posLong);

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Lägg till ny favorit")
                .setPositiveButton("OK", positiveListener)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });

        mNewFavoriteName.setText("" + position.latitude);
        mNewFavoriteRadius.setText("" + position.longitude);

        // Create the AlertDialog object and return it
        builder.setView(view);
        return builder.create();
    }

    private DialogInterface.OnClickListener positiveListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {

            if ("".equals(mNewFavoriteName.getText().toString()) ||
                "".equals(mNewFavoriteRadius.getText().toString())) {
                return;
            }

            JSONObject body = new JSONObject();
            try {
                body.put("_id", mPrefs.getString("_id", ""));
                body.put("lat", position.latitude);
                body.put("long", position.longitude);
                body.put("name", ((TextView) view.findViewById(R.id.newFavoriteName)).getText());
                body.put("radius", Integer.parseInt(((TextView) view.findViewById(R.id.newFavoriteRadius)).getText().toString()));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            JsonObjectRequest addFavoriteRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    ADD_FAVORITE_URL.replace("_id", mPrefs.getString("familyId", "0")),
                    body,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d(TAG, "addFavorite response: " + response);
                            try {
                                // TODO Notify MapActivity to refresh
                                Snackbar.make(view, response.getString("message"), Snackbar.LENGTH_LONG).show();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            if (error.networkResponse == null) {
                                if (error.getClass().equals(TimeoutError.class)) {
                                    Snackbar.make(view, "Request timed out", Snackbar.LENGTH_LONG).show();
                                }
                            }
                        }
                    }
            );
            mRequestQueue.add(addFavoriteRequest);
        }
    };

}
