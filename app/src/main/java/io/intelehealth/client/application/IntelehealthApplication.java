package io.intelehealth.client.application;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;


import com.parse.Parse;

import java.util.concurrent.TimeUnit;

import io.intelehealth.client.activities.setting_activity.SettingsActivity;
import io.intelehealth.client.database.DelayedJobQueueProvider;
import io.intelehealth.client.database.LocalRecordsDatabaseHelper;
import io.intelehealth.client.utilities.HelperMethods;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

public class IntelehealthApplication extends Application implements Application.ActivityLifecycleCallbacks{

    private static Context mContext;
    private Activity currentActivity;

    private static final String TAG = IntelehealthApplication.class.getSimpleName();
    private static String androidId;

    @Override
    public void onCreate() {
        super.onCreate();
        this.mContext = getApplicationContext();

        androidId = String
                .format("%16s", Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID))
                .replace(' ', '0');

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String url = sharedPreferences.getString(SettingsActivity.KEY_PREF_SERVER_URL, null);
        if(url==null){
            Log.i(TAG, "onCreate: Parse not init");
        }
        else {
            Dispatcher dispatcher = new Dispatcher();
            dispatcher.setMaxRequestsPerHost(1);
            dispatcher.setMaxRequests(4);
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(2, TimeUnit.MINUTES)
                    .writeTimeout(2,TimeUnit.MINUTES)
                    .readTimeout(2,TimeUnit.MINUTES);
            builder.dispatcher(dispatcher);

            Parse.initialize(new Parse.Configuration.Builder(this)
                    .clientBuilder(builder)
                    .applicationId(HelperMethods.IMAGE_APP_ID)
                    .server("http://"+url+":1337/parse/")
                    .build()
            );
            Log.i(TAG, "onCreate: Parse init");
        }

        LocalRecordsDatabaseHelper mDbHelper = new LocalRecordsDatabaseHelper(this);
        SQLiteDatabase localdb = mDbHelper.getWritableDatabase();

        localdb.execSQL("UPDATE "+DelayedJobQueueProvider.DELAYED_JOBS_TABLE_NAME +" SET "+ DelayedJobQueueProvider.SYNC_STATUS+"=0");




        registerActivityLifecycleCallbacks(this);
    }

    public static Context getAppContext() {
        return mContext;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    public static String getAndroidId() {
        return androidId;
    }

    public Activity getCurrentActivity() {
        return currentActivity;
    }
}
