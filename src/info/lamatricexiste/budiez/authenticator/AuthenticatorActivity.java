package info.lamatricexiste.budiez.authenticator;

import info.lamatricexiste.budiez.Constants;
import info.lamatricexiste.budiez.Network;
import info.lamatricexiste.budiez.R;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;

public class AuthenticatorActivity extends AccountAuthenticatorActivity {

    private static final String TAG = "AuthenticatorActivity";
    private AccountManager mAccountManager;
    private String mUsername;
    private boolean mRequestNewAccount = false;

    @Override
    public void onCreate(Bundle icicle) {
        Log.e(TAG, "onCreate()");
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_LEFT_ICON);

        final Intent intent = getIntent();
        mAccountManager = AccountManager.get(this);
        mUsername = intent.getStringExtra(Constants.PARAM_USERNAME);
        mRequestNewAccount = mUsername == null;

        if (mRequestNewAccount) {
            // Register
            setContentView(R.layout.register_activity);
            setTitle("Register");
        }
        else {
            // Login
            setContentView(R.layout.login_activity);
            setTitle("Log-In");
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Authenticating");
        dialog.setIndeterminate(true);
        dialog.setCancelable(true);
        dialog.setOnCancelListener(null);
        return dialog;
    }

    public void handleLogin(View view) {
        Log.e(TAG, "handleLogin()");
        mUsername = ((EditText) findViewById(R.id.username_edit)).getText().toString();
        String password = ((EditText) findViewById(R.id.password_edit)).getText().toString();
        new AuthTask().execute(mUsername, password);
    }

    public void handleRegister(View view) {
        Log.e(TAG, "handleRegister()");
        // mUsername = ((EditText) findViewById(R.id.username_edit)).getText().toString();
        // String password = ((EditText) findViewById(R.id.password_edit)).getText().toString();
    }

    public void switchForm(View view) {
        int layout = view.getId();
        switch (layout) {
            case R.id.switch_login:
                setContentView(R.layout.login_activity);
                setTitle("Log-In");
                break;
            case R.id.switch_register:
            default:
                setContentView(R.layout.register_activity);
                setTitle("Register");
                break;
        }
    }

    private class AuthTask extends AsyncTask<String, Void, String> {

        private String mServerUrl;

        @Override
        protected void onPreExecute() {
            showDialog(0);
            mServerUrl = getString(R.string.server_master);
        }

        @Override
        protected String doInBackground(String... params) {
            String token = null;
            final String user = params[0];
            final String pass = params[1];
            try {
                // Make request
                final URL url = new URL(String.format(mServerUrl, user, pass, "_session"));
                final HashMap<String, String> headers = new HashMap<String, String>(1);
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                Network res = Network.request(url, "POST", "name=" + user + "&password=" + pass,
                        headers);
                // Get Cookie
                if (res != null && res.headers != null) {
                    Iterator<Entry<String, List<String>>> it = res.headers.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, List<String>> pairs = it.next();
                        if ("set-cookie".equals(pairs.getKey().toLowerCase())) {
                            token = pairs.getValue().get(0);
                        }
                    }
                }
            }
            catch (MalformedURLException e) {}
            return token;
        }

        @Override
        protected void onPostExecute(String token) {
            // Create new account or update password token of existing one
            final Account account = new Account(mUsername, Constants.ACCOUNT_TYPE);
            if (!mAccountManager.addAccountExplicitly(account, token, null)) {
                mAccountManager.setPassword(account, token);
            }
            // ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);

            // Set result intention
            final Intent intent = new Intent();
            intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUsername);
            intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
            intent.putExtra(AccountManager.KEY_AUTHTOKEN, token);
            setAccountAuthenticatorResult(intent.getExtras());
            setResult(RESULT_OK, intent);

            // Close Authenticator
            finish();
            dismissDialog(0);
        }

        @Override
        protected void onCancelled() {
            dismissDialog(0);
            // TODO: handle error
        }

    }

}
