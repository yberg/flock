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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * The login activity.
 * Checks if the input is valid and sends a login request to the server.
 */
public class LoginActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    private static final String TAG = "FLOCK/LoginActivity";

    private static final int LOGIN_METHOD_EMAIL = 0;
    private static final int LOGIN_METHOD_GOOGLE = 1;

    private static final String LOGIN_URL = "http://192.168.0.105:3000/auth";
    private static final int RC_SIGN_IN = 9001;

    // TODO: hide secretwith ProGuard
    private String clientId;

    private RequestQueue requestQueue;

    private CoordinatorLayout coordinatorLayout;
    private ProgressBar spinner;
    private ImageView check;
    private EditText email, password;
    private RelativeLayout loginButton;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        SharedPreferences prefs = getSharedPreferences("com.yberg.android.life", Context.MODE_PRIVATE);
        //String storedId = prefs.getString("_id", "");
        //String storedName = prefs.getString("name", "");
        if (prefs.getBoolean("authenticated", false)) {
            startActivity(new Intent(this, MapActivity.class));
            finish();
        }

        // Get strings
        Resources res = getResources();
        clientId = res.getString(R.string.client_id_string);

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(clientId)
                .requestEmail()
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        requestQueue = Volley.newRequestQueue(this);

        spinner = (ProgressBar) findViewById(R.id.progressBar);
        spinner.getIndeterminateDrawable().setColorFilter(
                new LightingColorFilter(0xFF000000, Color.WHITE));
        spinner.setVisibility(View.INVISIBLE);

        check = (ImageView) findViewById(R.id.login_ok);

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);

        email = (EditText) findViewById(R.id.email);
        email.setText(prefs.getString("email", ""));
        email.requestFocus();
        email.selectAll();

        password = (EditText) findViewById(R.id.password);
        password.setOnEditorActionListener(editorActionListener);

        loginButton = (RelativeLayout) findViewById(R.id.email_sign_in_button);
        loginButton.setOnClickListener(this);

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    /**
     * Enables or disables login button clicks and shows or hides a loading spinner.
     * @param enabled enable or disable
     */
    public void setEnabled(boolean enabled) {
        if (enabled) {
            loginButton.setEnabled(true);
            loginButton.setBackgroundResource(R.drawable.button);
            spinner.setVisibility(View.INVISIBLE);
        }
        else {
            loginButton.setEnabled(false);
            loginButton.setBackgroundResource(R.drawable.button_pressed);
            spinner.setVisibility(View.VISIBLE);
        }
    }

    private void signInWithGoogle() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
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

    EditText.OnEditorActionListener editorActionListener = new EditText.OnEditorActionListener() {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (v.getId() == R.id.password)
                loginButton.performClick();
            return false;
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.email_sign_in_button:
                setEnabled(false);
                signIn(email.getText().toString());
                break;

            case R.id.google_sign_in_button:
                Log.d(TAG, "sign in with google");
                signInWithGoogle();
                break;
        }
    }

    public void signIn(String email) {
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            signIn(body);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void signIn(GoogleSignInAccount acct) {
        JSONObject body = new JSONObject();
        try {
            body.put("gmail", acct.getEmail());
            body.put("name", acct.getDisplayName());
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

                        check.setVisibility(View.VISIBLE);

                        // Store user information in Shared Preferences
                        SharedPreferences.Editor editor = LoginActivity.this.getSharedPreferences(
                                "com.yberg.android.life", Context.MODE_PRIVATE
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
                        password.setText("");
                        Snackbar.make(coordinatorLayout, response.getString("message"), Snackbar.LENGTH_LONG).show();
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
                        Snackbar.make(coordinatorLayout, "Request timed out",
                                Snackbar.LENGTH_LONG).show();
                    }
                }
            }
        });
        requestQueue.add(loginRequest);
    }
}
