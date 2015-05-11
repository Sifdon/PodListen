package com.einmalfel.podlisten;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;


public class Provider extends ContentProvider {
  public static final String T_EPISODE = "episode";
  public static final String T_PODCAST = "podcast";
  public static final String K_ID = "_ID";
  public static final String K_ENAME = "episode_name";
  public static final String K_EDATE = "publication_date";
  public static final String K_EDESCR = "episode_description";
  public static final String K_ESTATE = "episode_state";
  public static final String K_EAURL = "audio_url";
  public static final String K_EURL = "episode_url";
  public static final String K_EDFIN = "download_finished";
  public static final String K_EDATT = "download_attempts";
  public static final String K_EPID = "podcast_id";
  public static final String K_PNAME = "podcast_name";
  public static final String K_PDESCR = "podcast_description";
  public static final String K_PURL = "podcast_url";
  public static final String K_PFURL = "feed_url";
  public static final String K_PSTATE = "podcast_state";
  public static final int ESTATE_NEW = 0;
  public static final int ESTATE_SELECTED = 1;
  public static final int ESTATE_IN_PLAYLIST = 2;
  public static final int ESTATE_GONE = 3;
  public static final int PSTATE_NEW = 0;
  public static final int PSTATE_SEEN_ONCE = 1;

  public static final String authorityBase = "com.einmalfel.podlisten";
  public static final String commonUriString = ContentResolver.SCHEME_CONTENT + "://" + authorityBase;
  public static final Uri podcastUri = Uri.parse(commonUriString + '/' + T_PODCAST);
  public static final Uri episodeUri = Uri.parse(commonUriString + '/' + T_EPISODE);

  private static final String[] TABLES = {T_EPISODE, T_PODCAST};
  private static final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
  private static final String TAG = "PLP";
  private static HelperV1 helper;

  public static Uri getUri(String table, Long id) {
    Uri.Builder builder = new Uri.Builder()
        .scheme(ContentResolver.SCHEME_CONTENT)
        .authority(authorityBase)
        .appendPath(table);
    if (id != null) {
      builder.appendPath(id.toString());
    }
    return builder.build();
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    int code = matcher.match(uri);
    if (code == -1) {
      Log.e(TAG, "Wrong query uri " + uri + ". Code " + code);
      return 0;
    }
    if (code >= TABLES.length) {
      code -= TABLES.length;
      selection = "_ID = " + uri.getLastPathSegment();
    }

    SQLiteDatabase db = helper.getWritableDatabase();
    int result;
    try {
      result = db.delete(TABLES[code], selection, selectionArgs);
    } catch (RuntimeException ignored) {
      return 0;
    }
    return result;
  }

  @Override
  public String getType(Uri uri) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    int code = matcher.match(uri);
    if (code < 0 || code >= TABLES.length) {
      Log.e(TAG, "Wrong insert uri " + uri + ". Code " + code);
      return null;
    }
    SQLiteDatabase db = helper.getWritableDatabase();
    long id = db.insert(TABLES[code], null, values);

    if (id == -1) {
      Log.e(TAG, "SQLite insert failed " + uri + ". Values " + values);
      return null;
    }
    return getUri(TABLES[code], id);
  }

  @Override
  public boolean onCreate() {
    helper = new HelperV1(getContext(), authorityBase);
    for (int i = 0; i < TABLES.length; i++) {
      matcher.addURI(authorityBase, TABLES[i], i);
      matcher.addURI(authorityBase, TABLES[i] + "/#", TABLES.length + i);
    }
    return true;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection,
                      String[] selectionArgs, String sortOrder) {
    int code = matcher.match(uri);
    if (code == -1) {
      Log.e(TAG, "Wrong query uri " + uri + ". Code " + code);
      return null;
    }
    if (code >= TABLES.length) {
      code -= TABLES.length;
      selection = "_ID = " + uri.getLastPathSegment();
    }
    SQLiteDatabase db = helper.getReadableDatabase();
    return db.query(TABLES[code], projection, selection, selectionArgs, null, null, sortOrder);
  }


  @Override
  public int update(Uri uri, ContentValues values, String selection,
                    String[] selectionArgs) {
    int code = matcher.match(uri);
    if (code == -1) {
      Log.e(TAG, "Wrong query uri " + uri + ". Code " + code);
      return 0;
    }
    if (code >= TABLES.length) {
      code -= TABLES.length;
      selection = "_ID = " + uri.getLastPathSegment();
    }
    SQLiteDatabase db = helper.getWritableDatabase();
    return db.update(TABLES[code], values, selection, selectionArgs);
  }

  private static class HelperV1 extends SQLiteOpenHelper {
    HelperV1(Context context, String name) {
      super(context, name, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL("CREATE TABLE " + T_PODCAST + " (" +
          K_ID + " INTEGER PRIMARY KEY," +
          K_PNAME + " TEXT," +
          K_PDESCR + " TEXT," +
          K_PSTATE + " INTEGER," +
          K_PURL + " TEXT," +
          K_PFURL + " TEXT" +
          ')');
      db.execSQL("CREATE TABLE " + T_EPISODE + " (" +
          K_ID + " INTEGER PRIMARY KEY," +
          K_ENAME + " TEXT," +
          K_EDESCR + " TEXT," +
          K_EURL + " TEXT," +
          K_EAURL + " TEXT," +
          K_EDATE + " INTEGER," +
          K_EDFIN + " INTEGER," +
          K_EDATT + " INTEGER," +
          K_ESTATE + " INTEGER," +
          K_EPID + " INTEGER," +
          "FOREIGN KEY(" + K_EPID + ") REFERENCES " + T_PODCAST + '(' + K_ID + ')' +
          ')');
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      throw new UnsupportedOperationException("Not yet implemented");
    }
  }

}
