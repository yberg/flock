package com.yberg.android.flock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * The login activity.
 * Checks if the input is valid and sends a login request to the server.
 */
public class LoginActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    private static final String TAG = "FLOCK/LoginActivity";

    private static final String LOGIN_URL = MapActivity.BASE_URL + "auth";
    private static final int RC_SIGN_IN = 9001;

    private RequestQueue mRequestQueue;

    private CoordinatorLayout mCoordinatorLayout;
    private ProgressBar mSpinner;
    private ImageView mCheck;
    private EditText mEmail, mPassword;
    private RelativeLayout mSignInButton;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        SharedPreferences prefs = getSharedPreferences("com.yberg.android.flock", Context.MODE_PRIVATE);
        //String storedId = prefs.getString("_id", "");
        //String storedName = prefs.getString("name", "");
        if (prefs.getBoolean("authenticated", false)) {
            startActivity(new Intent(this, MapActivity.class));
            finish();
        }

        // Get strings
        Resources res = getResources();
        String clientId = res.getString(R.string.client_id);

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(clientId)
                .requestEmail()
                //.requestScopes(new Scope(Scopes.DRIVE_APPFOLDER))
                //.requestServerAuthCode(clientId, false)
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        mRequestQueue = Volley.newRequestQueue(this);

        mSpinner = (ProgressBar) findViewById(R.id.progressBar);
        mSpinner.getIndeterminateDrawable().setColorFilter(
                new LightingColorFilter(0xFF000000, Color.WHITE));
        mSpinner.setVisibility(View.INVISIBLE);

        mCheck = (ImageView) findViewById(R.id.login_ok);

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);

        mEmail = (EditText) findViewById(R.id.email);
        mEmail.setText(prefs.getString("email", ""));
        mEmail.requestFocus();
        mEmail.selectAll();

        mPassword = (EditText) findViewById(R.id.password);
        mPassword.setOnEditorActionListener(editorActionListener);

        mSignInButton = (RelativeLayout) findViewById(R.id.email_sign_in_button);
        mSignInButton.setOnClickListener(this);

        SignInButton googleSignInButton = ((SignInButton) findViewById(R.id.google_sign_in_button));
        googleSignInButton.setOnClickListener(this);

        for (int i = 0; i < googleSignInButton.getChildCount(); i++) {
            View v = googleSignInButton.getChildAt(i);

            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                Log.d(TAG, "text size: " + tv.getTextSize());
                tv.setText("Logga in med Google");
                tv.setPadding(0, 0, (int) MapActivity.dpToPixels(4, v), 0);
                return;
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void signInWithGoogle() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            //mStatusTextView.setText(getString(R.string.signed_in_fmt, acct.getDisplayName()));
            Log.d(TAG, acct.getDisplayName() + " " + acct.getEmail());
            signIn(acct);
            //updateUI(true);
        } else {
            // Signed out, show unauthenticated UI.
            //updateUI(false);
        }
    }

    /**
     * Enables or disables login button clicks and shows or hides a loading mSpinner.
     * @param enabled enable or disable
     */
    public void setEnabled(boolean enabled) {
        if (enabled) {
            mSignInButton.setEnabled(true);
            mSignInButton.setBackgroundResource(R.drawable.button);
            mSpinner.setVisibility(View.INVISIBLE);
        }
        else {
            mSignInButton.setEnabled(false);
            mSignInButton.setBackgroundResource(R.drawable.button_pressed);
            mSpinner.setVisibility(View.VISIBLE);
        }
    }

    EditText.OnEditorActionListener editorActionListener = new EditText.OnEditorActionListener() {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (v.getId() == R.id.password)
                mSignInButton.performClick();
            return false;
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.email_sign_in_button:
                setEnabled(false);
                signIn(mEmail.getText().toString(), mPassword.getText().toString());
                break;

            case R.id.google_sign_in_button:
                Log.d(TAG, "sign in with google");
                signInWithGoogle();
                break;
        }
    }

    public void signIn(String email, String password) {
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", password);
            signIn(body);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void signIn(GoogleSignInAccount acct) {
        JSONObject body = new JSONObject();
        try {
            body.put("gmail", acct.getEmail());
            body.put("idToken", acct.getIdToken());
            signIn(body);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void signIn(JSONObject body) {

        JsonObjectRequest loginRequest = new JsonObjectRequest(Request.Method.POST, LOGIN_URL, body, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, "login response: " + response);
                try {
                    if (response.getBoolean("success")) {

                        mCheck.setVisibility(View.VISIBLE);

                        // Store user information in Shared Preferences
                        SharedPreferences.Editor editor = LoginActivity.this.getSharedPreferences(
                                "com.yberg.android.flock", Context.MODE_PRIVATE
                        ).edit();
                        editor.putBoolean("authenticated", true);
                        editor.putString("_id", response.getString("_id"));
                        editor.putString("name", response.getString("name"));
                        if (!response.isNull("email")) {
                            editor.putString("email", response.getString("email"));
                        } else {
                            editor.remove("email");
                        }
                        if (!response.isNull("familyId")) {
                            editor.putString("familyId", response.getString("familyId"));
                        } else {
                            editor.remove("familyId");
                        }
                        editor.apply();

                        startActivity(new Intent(LoginActivity.this, MapActivity.class));
                        finish();
                    } else {
                        mPassword.setText("");
                        Snackbar.make(mCoordinatorLayout, response.getString("message"), Snackbar.LENGTH_LONG).show();
                    }
                    setEnabled(true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error.networkResponse == null) {
                    if (error.getClass().equals(TimeoutError.class)) {
                        setEnabled(true);
                        Snackbar.make(mCoordinatorLayout, "Request timed out",
                                Snackbar.LENGTH_LONG).show();
                    }
                }
            }
        }) {
            @Override
            public Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                /*try {
                    String jsonString = new String(response.data,
                            HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
                    JSONObject jsonResponse = new JSONObject(jsonString);
                    jsonResponse.put("headers", new JSONObject(response.headers));
                    return Response.success(jsonResponse,
                            HttpHeaderParser.parseCacheHeaders(response));
                } catch (UnsupportedEncodingException | JSONException e) {
                    return Response.error(new ParseError(e));
                }*/
                Map<String, String> headers = response.headers;
                String cookies = headers.get("Set-Cookie");
                setCookie(cookies, LoginActivity.this);
                return super.parseNetworkResponse(response);
            }
        };
        mRequestQueue.add(loginRequest);
    }

    public static void setCookie(String cookie, Context context) {
        // Store user information in Shared Preferences
        SharedPreferences.Editor editor = context.getSharedPreferences(
                "com.yberg.android.flock", Context.MODE_PRIVATE
        ).edit();
        Log.d(TAG, "Setting cookie " + cookie);
        editor.putString("session", cookie);
        editor.apply();
    }
}
