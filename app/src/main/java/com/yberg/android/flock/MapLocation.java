package com.yberg.android.flock;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by Viktor on 2016-10-09.
 */

/**
 * Describes a location on the map.
 */
public abstract class MapLocation {

    public static final int FAVORITE = 1;
    public static final int MEMBER = 2;

    String name, _id, familyId;
    Marker marker;

    /**
     * @return The name (member name or favorite name).
     */
    String getName() {
        return name;
    }

    /**
     * @return The location's marker.
     */
    Marker getMarker() {
        return marker;
    }

    /**
     * @return the location's family id.
     */
    String getFamilyId() {
        return familyId;
    }

    /**
     * Sets the color of the marker.
     * @param hue The hue of the marker. Value must be greater or equal to 0 and less than 360.
     */
    void setMarkerColor(float hue) {
        marker.setIcon(BitmapDescriptorFactory.defaultMarker(hue));
    }

    /**
     * @return The id of the location.
     */
    String getId() {
        return _id;
    }

    /**
     * @return The location type (FAVORITE/MEMBER).
     */
    abstract int getType();

    /**
     * Returns the icon of the location.
     * @param context The context.
     * @return The icon of the location.
     */
    abstract Drawable getIcon(Context context);

    /**
     * Sets the default color on the marker.
     */
    abstract void setDefaultMarkerColor();
}
