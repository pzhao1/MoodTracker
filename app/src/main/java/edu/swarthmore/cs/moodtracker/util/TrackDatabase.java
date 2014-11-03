package edu.swarthmore.cs.moodtracker.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

import edu.swarthmore.cs.moodtracker.util.TrackContract.AppInfoSchema;
import edu.swarthmore.cs.moodtracker.util.TrackContract.AppUsageSchema;
import edu.swarthmore.cs.moodtracker.util.TrackContract.SurveyInfoSchema;

/**
 * Created by Peng on 10/19/2014.
 * Local SQLite Database that stores all tracking information.
 * Provides permanent storage, i.e. information is not lost when activity and service stop.
 */
public class TrackDatabase extends SQLiteOpenHelper {
    private static final String TAG = "TrackDatabase";

    // Private factory instance.
    private static TrackDatabase sInstance = null;

    // Application context of this database.
    private Context mContext = null;

    /**
     * Static factory method to create a TrackDatabase instance or retrieve the existing instance
     * @param context The context of the activity creating the database.
     * @return The TrackDatabase instance
     */
    public static TrackDatabase getInstance(Context context) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new TrackDatabase(context.getApplicationContext());
        }

        return sInstance;
    }

    /**
     * Private constructor for TrackDatabase, used by static getInstance() method.
     * @param context The application context this database lives in
     */
    private TrackDatabase(Context context) {
        super(context, TrackContract.DATABASE_NAME, null, TrackContract.DATABASE_VERSION);
        mContext = context;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create the AppUsage Table
        String CREATE_APP_USAGE_TABLE = "CREATE TABLE " + AppUsageSchema.TABLE_NAME + "("
                + AppUsageSchema.COLUMN_PACKAGE + " Text, "
                + AppUsageSchema.COLUMN_USAGE_SEC + " INTEGER, "
                + AppUsageSchema.COLUMN_DATE + " INTEGER, "
                + "PRIMARY KEY (" + AppUsageSchema.COLUMN_PACKAGE + ", " + AppUsageSchema.COLUMN_DATE + ")"
                + ")";
        db.execSQL(CREATE_APP_USAGE_TABLE);

        // Create the AppInfo Table.
        // We don't store AppName and Icon in AppUsage Table, because there might be multiple rows
        // of the same app in AppUsage, and we don't want duplicate information.
        String CREATE_APP_INFO_TABLE = "CREATE TABLE " + AppInfoSchema.TABLE_NAME + "("
                + AppInfoSchema.COLUMN_PACKAGE + " Text PRIMARY KEY, "
                + AppInfoSchema.COLUMN_APP_NAME + " Text, "
                + AppInfoSchema.COLUMN_APP_ICON + " BLOB"
                + ")";
        db.execSQL(CREATE_APP_INFO_TABLE);

        String CREATE_SURVEY_INFO_TABLE = "CREATE TABLE " + SurveyInfoSchema.TABLE_NAME + "("
                + SurveyInfoSchema.COLUMN_SURVEY_NUMBER + " INTEGER PRIMARY KEY, "
                + SurveyInfoSchema.COLUMN_QUESTIONS_ANSWERS + " Text"
                + SurveyInfoSchema.COLUMN_DATE + " INTEGER, "
                + ")";
        db.execSQL(CREATE_SURVEY_INFO_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + AppUsageSchema.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + AppInfoSchema.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + SurveyInfoSchema.TABLE_NAME);

        // Create tables again
        onCreate(db);
    }

    /**
     * Write an AppUsage entry into the database, overwriting any existing entries.
     * @param entry The AppUsage entry that we write into the database.
     */
    public void writeAppUsage(AppUsageEntry entry) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Insert app info (name and icon) if AppInfo table doesn't have this app yet.
        ContentValues appInfoValues = new ContentValues();
        appInfoValues.put(AppInfoSchema.COLUMN_PACKAGE, entry.PackageName);
        appInfoValues.put(AppInfoSchema.COLUMN_APP_NAME, entry.AppName);
        appInfoValues.put(AppInfoSchema.COLUMN_APP_ICON, entry.getIconInByteArray());
        db.insertWithOnConflict(AppInfoSchema.TABLE_NAME, null, appInfoValues, SQLiteDatabase.CONFLICT_IGNORE);

        // Insert app usage info, overwriting any existing entries.
        ContentValues appUsageValues = new ContentValues();
        appUsageValues.put(AppUsageSchema.COLUMN_PACKAGE, entry.PackageName);
        appUsageValues.put(AppUsageSchema.COLUMN_USAGE_SEC, entry.UsageTimeSec);
        appUsageValues.put(AppUsageSchema.COLUMN_DATE, entry.DaysSinceEpoch);
        db.insertWithOnConflict(AppUsageSchema.TABLE_NAME, null, appUsageValues, SQLiteDatabase.CONFLICT_REPLACE);

        db.close();
    }

    /**
     * Retrieve app usage entries from database satisfying certain conditions.
     * @param date Date of the app usage entries to retrieve, expressed in number of days
     *             from Epoch (midnight of 1970-1-1 GMT).
     * @return A list of app usage entries satisfying the given condition.
     */
    public ArrayList<AppUsageEntry> readAppUsage(long date) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Use a raw query to query appInfoTable and appUsageTable at the same time.
        String selections = " ";
        selections += AppInfoSchema.TABLE_NAME + "." + AppInfoSchema.COLUMN_PACKAGE + ", ";
        selections += AppInfoSchema.TABLE_NAME + "." + AppInfoSchema.COLUMN_APP_NAME + ", ";
        selections += AppInfoSchema.TABLE_NAME + "." + AppInfoSchema.COLUMN_APP_ICON + ", ";
        selections += AppUsageSchema.TABLE_NAME + "." + AppUsageSchema.COLUMN_USAGE_SEC + ", ";
        selections += AppUsageSchema.TABLE_NAME + "." + AppUsageSchema.COLUMN_DATE + " ";
        String tables = " " + AppInfoSchema.TABLE_NAME + ", " + AppUsageSchema.TABLE_NAME + " ";

        String conditions = " ";
        conditions += AppInfoSchema.TABLE_NAME + "." + AppInfoSchema.COLUMN_PACKAGE +
                " = " + AppUsageSchema.TABLE_NAME + "." + AppUsageSchema.COLUMN_PACKAGE;
        conditions += " and " + AppUsageSchema.TABLE_NAME + "." + AppUsageSchema.COLUMN_DATE +
                " = " + date;

        String rawQuery = "SELECT" + selections + "FROM" + tables + "WHERE" + conditions;

        // Query the database to get a cursor
        Cursor cursor = db.rawQuery(rawQuery, null);

        // Retrieve app usage entries from cursor and return it.
        ArrayList <AppUsageEntry> entries = new ArrayList<AppUsageEntry>();
        if (cursor.moveToFirst()){
            int numApps = cursor.getCount();
            for (int i=0; i<numApps; i++){
                entries.add(new AppUsageEntry(cursor));
            }
        }

        db.close();
        return entries;
    }
}
