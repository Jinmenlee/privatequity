package aoa.tarot.privatequity;

import android.Manifest;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.CallLog;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.tasks.TasksScopes;
import com.google.api.services.tasks.model.Task;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import aoa.tarot.privatequity.tasks.AsyncLoadTasks;


public class MainActivity extends Activity {

    private final String TAG = "MainActivity";
    int MY_PERMISSIONS_REQUEST_READ_CALL_LOG = 1023;
    int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1024;
    int MY_PERMISSIONS_REQUEST_READ_SMS = 1025;

    TextView log;
    Button btnCall;
    Button btnLocate;
    Button btnSms;
    Button btnGmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        log = findViewById(R.id.log);
        log.setMovementMethod(new ScrollingMovementMethod());

        btnCall = findViewById(R.id.btn1);
        btnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log.setText(getCallDetails());
            }
        });

        btnLocate = findViewById(R.id.btn2);
        btnLocate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log.setText("loading ...");
                getLocateHistory();
            }
        });
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnSms = findViewById(R.id.btn3);
        btnSms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log.setText(getMessage());
            }
        });

        btnGmail = findViewById(R.id.btn4);
        btnGmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    log.setText(getGmail());
                } catch (IOException e) {
                    log.setText(e.toString());
                }
//                try {
//                    log.setText(getGmail2());
//                } catch (IOException e) {
//                    log.setText(e.toString());
//                } catch (GeneralSecurityException e) {
//                    log.setText(e.toString());
//                }
            }
        });
    }

    private static final String PREF_ACCOUNT_NAME = "n300.itri@gmail.com";
    GoogleAccountCredential credential;
    com.google.api.services.tasks.Tasks service;
    Gmail gmail;

    private FusedLocationProviderClient mFusedLocationClient;
    private String locate = "";
    private String getLocateHistory() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
            return "permission denied";
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
            return "permission denied";
        }

        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(final Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            locate = location.getLatitude() + ", " + location.getLongitude();
                        } else {
                            locate = "empty";
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Geocoder geocoder;
                                List<Address> addresses;
                                geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                                try {
                                    addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                                    //String address = addresses.get(0).getAddressLine(0);
                                    String city = addresses.get(0).getLocality();
                                    String name = addresses.get(0).getFeatureName();
                                    String country = addresses.get(0).getCountryName();
                                    String postalCode = addresses.get(0).getPostalCode();
                                    locate += "\npostalCode: " + postalCode;
                                    locate += "\ncountry: " + country;
                                    locate += "\ncity: " + city;
                                    locate += "\nknownName: " + name;

                                    String str = addresses.get(0).toString();
                                    System.out.println(str);
                                } catch (IOException e) {
                                }

                                log.setText(locate);
                            }
                        });
                    }
                });

        return locate;
    }

    private String getMessage() {

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.READ_SMS)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_SMS},
                        MY_PERMISSIONS_REQUEST_READ_SMS);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
            return "permission denied";
        }

        List<Sms> lst = getAllSms();
        
        if ((lst == null) || (lst.size() == 0))
            return "empty";

        StringBuilder sb = new StringBuilder();
        for (Sms sms: lst) {
            sb.append(sms.toString());
        }
        
        return sb.toString();
    }

    private String getCallDetails() {

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_CALL_LOG)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.READ_CALL_LOG)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_CALL_LOG},
                        MY_PERMISSIONS_REQUEST_READ_CALL_LOG);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
            return "permission denied";
        }

        StringBuffer sb = new StringBuffer();
        try (Cursor managedCursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, null,
                null, null, null)) {
            int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
            int type = managedCursor.getColumnIndex(CallLog.Calls.TYPE);
            int date = managedCursor.getColumnIndex(CallLog.Calls.DATE);
            int duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);
            sb.append("Call Details :");
            while (managedCursor.moveToNext()) {
                String phNumber = managedCursor.getString(number);
                String callType = managedCursor.getString(type);
                String callDate = managedCursor.getString(date);
                Date callDayTime = new Date(Long.valueOf(callDate));
                String callDuration = managedCursor.getString(duration);
                String dir = null;
                int dircode = Integer.parseInt(callType);
                switch (dircode) {
                    case CallLog.Calls.OUTGOING_TYPE:
                        dir = "OUTGOING";
                        break;

                    case CallLog.Calls.INCOMING_TYPE:
                        dir = "INCOMING";
                        break;

                    case CallLog.Calls.MISSED_TYPE:
                        dir = "MISSED";
                        break;
                }
                sb.append("\nPhone Number:--- " + phNumber + " \nCall Type:--- "
                        + dir + " \nCall Date:--- " + callDayTime
                        + " \nCall duration in sec :--- " + callDuration);
                sb.append("\n----------------------------------");
            }
            managedCursor.close();
        }
        return sb.toString();
    }

    private static final int REQUEST_ACCOUNT_PICKER = 10070;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 10080;
    private static final int REQUEST_AUTHORIZATION = 10090;

    private static final String APPLICATION_NAME = "Gmail API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_LABELS);
    private static final String CREDENTIALS_FILE_PATH = "google.json";

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
//        InputStream in = MainActivity.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        InputStream in = getResources().openRawResource(R.raw.google);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("n300.itri@gmail.com");
    }

    private String getGmail2() throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
//        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        final NetHttpTransport HTTP_TRANSPORT = new com.google.api.client.http.javanet.NetHttpTransport();
        Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Print the labels in the user's account.
        StringBuilder sb = new StringBuilder();
        String user = "me";
        ListLabelsResponse listResponse = service.users().labels().list(user).execute();
        List<Label> labels = listResponse.getLabels();
        if (labels.isEmpty()) {
            sb.append("No labels found.");
        } else {
            sb.append("Labels:");
            for (Label label : labels) {
                sb.append(String.format("- %s\n", label.getName()));
            }
        }

        return sb.toString();
    }

    private String getGmail() throws IOException {
        credential = GoogleAccountCredential.usingOAuth2(this, Collections.singleton(TasksScopes.TASKS));
        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        credential.setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, null));
        if (credential.getSelectedAccountName() == null) {
            chooseAccount();
        }

        HttpTransport httpTransport = new com.google.api.client.http.javanet.NetHttpTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
//        service = new com.google.api.services.tasks.Tasks.Builder(httpTransport, jsonFactory, credential)
//                .setApplicationName(getApplicationName()).build();
        gmail = new Gmail.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName(getApplicationName()).build();

        final StringBuilder sb =  new StringBuilder();
        String user = "me";
        try {
            Gmail.Users.Labels labels = gmail.users().labels();
            final Gmail.Users.Labels.List list = labels.list(user);
            AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {

                ListLabelsResponse listResponse;

                @Override
                protected String doInBackground(Void... voids) {
                    log.setText("loading...");
                    try {
                        listResponse = list.execute();
                    } catch (IOException e) {
                        return e.toString();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(String token) {
                    if (listResponse == null) {
                        log.setText(token);
                        return;
                    }
                    List<Label> labels = listResponse.getLabels();
                    if (labels.isEmpty()) {
                        sb.append("No labels found.");
                    } else {
                        sb.append("Labels:");
                        for (Label label : labels) {
                            sb.append(String.format("- %s\n", label.getName()));
                        }
                    }
                }

            };
            task.execute();
        } catch (Exception e) {
            sb.append(e.toString());
        }
//        ListLabelsResponse listResponse = gmail.users().labels().list(user).execute();
//        List<Label> labels = listResponse.getLabels();
//        if (labels.isEmpty()) {
//            sb.append("No labels found.");
//        } else {
//            sb.append("Labels:");
//            for (Label label : labels) {
//                sb.append(String.format("- %s\n", label.getName()));
//            }
//        }

        return sb.toString();
    }

    private void chooseAccount() {
        startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    private String getApplicationName() {
        String name = BuildConfig.APPLICATION_ID;
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            name += "/" + version;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return name;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == 1) {
//            if (resultCode == RESULT_OK) {
//
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                    final String myPackageName = getPackageName();
//                    if (Telephony.Sms.getDefaultSmsPackage(this).equals(myPackageName)) {
//
//                        List<Sms> lst = getAllSms();
//                    }
//                }
//            }
//        }
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
//            case REQUEST_GOOGLE_PLAY_SERVICES:
//                if (resultCode == Activity.RESULT_OK) {
//                    haveGooglePlayServices();
//                } else {
//                    checkGooglePlayServicesAvailable();
//                }
//                break;
//            case REQUEST_AUTHORIZATION:
//                if (resultCode == Activity.RESULT_OK) {
//                    AsyncLoadTasks.run(this);
//                } else {
//                    chooseAccount();
//                }
//                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        credential.setSelectedAccountName(accountName);
                        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.commit();
                        //AsyncLoadTask.run(this);
                        //new GetAuthTokenCallback().run(null);
                    }
                }
                break;
        }
    }

    public List<Sms> getAllSms() {
        List<Sms> lstSms = new ArrayList<>();
        Sms objSms;

        Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                objSms = new Sms();
                objSms.setId(cursor.getString(cursor.getColumnIndexOrThrow("_id")));
                objSms.setAddress(cursor.getString(cursor
                        .getColumnIndexOrThrow("address")));
                objSms.setMsg(cursor.getString(cursor.getColumnIndexOrThrow("body")));
                objSms.setReadState(cursor.getString(cursor.getColumnIndex("read")));
                objSms.setTime(cursor.getString(cursor.getColumnIndexOrThrow("date")));
                if (cursor.getString(cursor.getColumnIndexOrThrow("type")).contains("1")) {
                    objSms.setFolderName("inbox");
                } else {
                    objSms.setFolderName("sent");
                }

                lstSms.add(objSms);

            } while (cursor.moveToNext());
        }
        cursor.close();

        return lstSms;
    }

    private class GetAuthTokenCallback implements AccountManagerCallback<Bundle> {

        Activity context = MainActivity.this;

        @Override
        public void run(AccountManagerFuture<Bundle> accountManagerFuture) {
            Bundle bundle;
            try {
                bundle = accountManagerFuture.getResult();
                Intent intent = (Intent)bundle.get(AccountManager.KEY_INTENT);
                if(intent != null) {
                    // User input required
                    context.startActivity(intent);
                } else {
                    AccountManager.get(context).invalidateAuthToken(bundle.getString(AccountManager.KEY_ACCOUNT_TYPE), bundle.getString(AccountManager.KEY_AUTHTOKEN));
                    AccountManager.get(context).invalidateAuthToken("ah", bundle.getString(AccountManager.KEY_AUTHTOKEN));
                    onGetAuthToken(bundle);
                }
            } catch (AuthenticatorException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (OperationCanceledException e) {
                e.printStackTrace();
            }
        }

        protected void onGetAuthToken(Bundle bundle) {
            String auth_token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
            new GetCookieTask().execute(auth_token);
        }

        private class GetCookieTask extends AsyncTask<String, Void, Boolean> {
            protected Boolean doInBackground(String... tokens) {

                return false;
            }

            protected void onPostExecute(Boolean result) {

                Thread myThread = new Thread(new Runnable(){
                    @Override
                    public void run() {

                    }
                });
                myThread.start();

            }
        }

    }
}

class AsyncLoadTask extends AsyncTask<Void, Void, Boolean> {

    MainActivity activity;
    com.google.api.services.tasks.Tasks client;

    AsyncLoadTask(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        List<String> result = new ArrayList<String>();
        List<Task> tasks =
                null;
        try {
            tasks = client.tasks().list("@default").setFields("items/title").execute().getItems();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (tasks != null) {
            for (Task task : tasks) {
                result.add(task.getTitle());
            }
        } else {
            result.add("No tasks.");
        }

        return null;
    }

    static void run(MainActivity activity) {
        new AsyncLoadTask(activity).execute();
    }
}


