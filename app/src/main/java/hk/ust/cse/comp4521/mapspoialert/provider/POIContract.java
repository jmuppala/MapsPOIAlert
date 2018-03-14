package hk.ust.cse.comp4521.mapspoialert.provider;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by muppala on 14/3/15.
 */
public final class POIContract {

    public POIContract() { }

    public static abstract class POIEntry implements BaseColumns {

        public static final String COLUMN_POI = "place";
        public static final String COLUMN_LATITUDE = "latitude";
        public static final String COLUMN_LONGITUDE = "longitude";
        public static final String DATABASE_TABLE_NAME = "POIgeocode";
    }

    public static abstract class POIProvider {

        public static final String AUTHORITY = "hk.ust.cse.comp4521.mapspoialert.provider";

        public static final String BASE_PATH = "POIDbProvider";
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
                + "/" + BASE_PATH);

    }
}
