package edu.swarthmore.cs.moodtracker;

import android.app.ActionBar;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Calendar;

import edu.swarthmore.cs.moodtracker.Fragments.AppUsageSectionFragment;
import edu.swarthmore.cs.moodtracker.Fragments.NavigationDrawerFragment;
import edu.swarthmore.cs.moodtracker.Fragments.SurveyQuestionFragment;
import edu.swarthmore.cs.moodtracker.Fragments.TextSectionFragment;


public class MainActivity extends FragmentActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    public static final String TAG = "MainActivity";
    public static final String EXTRA_DRAWER_SELECT = "SelectDrawerItem";

    // Navigation Drawer
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private Fragment mCurrentSectionFragment;
    private CharSequence mTitle;

    // Connection to TrackService
    private TrackService mService;
    private ServiceConnection mServiceConnection = new TrackServiceConnection();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Start and bind to the TrackService
        Intent intent = new Intent(this, TrackService.class);
        startService(intent);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);

        // Set up the drawer.
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));

        // If we started from notification, then the intent will contain a "SelectDrawerItem" extra
        // that points to the survey section. Select it. Otherwise, select 0.
        mNavigationDrawerFragment.selectItem(getIntent().getIntExtra(EXTRA_DRAWER_SELECT, 0));

        setNotificationForSurvey();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        mNavigationDrawerFragment.selectItem(intent.getIntExtra(EXTRA_DRAWER_SELECT, 0));
    }


    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();

        switch (position) {
            case 0:
                AppUsageSectionFragment usageFragment = new AppUsageSectionFragment();
                if (mService != null)
                    usageFragment.setService(mService);
                mTitle = getString(R.string.title_section1);
                mCurrentSectionFragment = usageFragment;
                break;
            case 1:
                mCurrentSectionFragment = new TextSectionFragment();
                mTitle = getString(R.string.title_section2);
                break;
            case 2:
                mCurrentSectionFragment = new SurveyQuestionFragment();
                mTitle = getString(R.string.title_section3);
                break;
        }

        fragmentManager.beginTransaction().replace(R.id.container, mCurrentSectionFragment).commit();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mService != null)
            mService.saveDataToDatabase();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unbindService(mServiceConnection);
    }

    /**
     * Show the notifications to remind users to take the survey.
     */
    private void setNotificationForSurvey() {

        Log.d("setNotificationForSurvey", "called");
        // Set alarm for survey
        Calendar mCalendar;
        PendingIntent mPendingIntent;
        AlarmManager mAlarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        Intent myIntent = new Intent(this, NotificationReceiver.class);
        int[] times = {9,15,22};

        for (int i=0; i<3; i++) {
            mCalendar = Calendar.getInstance();
            mCalendar.set(Calendar.HOUR_OF_DAY, times[i]);
            mCalendar.set(Calendar.MINUTE, 0);
            mCalendar.set(Calendar.SECOND, 0);
            mPendingIntent = PendingIntent.getBroadcast(this, i, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            //mAlarmManager.cancel(mPendingIntent);
            mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, mCalendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, mPendingIntent);
        }
    }

    /**
     * The ServiceConnection class used by this activity.
     * Handles the relationship between TrackService and different fragments.
     */
    private class TrackServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            TrackService.TrackBinder binder = (TrackService.TrackBinder) iBinder;
            mService = binder.getService();

            // Deliver service to fragment.
            if (mCurrentSectionFragment instanceof AppUsageSectionFragment)
                ((AppUsageSectionFragment) mCurrentSectionFragment).setService(mService);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;

            // Tell the fragments TrackService has disconnected.
            // Deliver service to fragment.
            if (mCurrentSectionFragment instanceof AppUsageSectionFragment)
                ((AppUsageSectionFragment) mCurrentSectionFragment).unsetService();
        }
    }
}
