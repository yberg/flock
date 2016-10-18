package com.yberg.android.flock;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by Viktor on 2016-10-09.
 */

public abstract class MapLocation {

    public static final int FAVORITE = 1;
    public static final int MEMBER = 2;

    String name, _id, familyId;
    Marker marker;

    String getName() {
        return name;
    }
    Marker getMarker() {
        return marker;
    }

    String getFamilyId() {
        return familyId;
    }

    void setMarkerColor(float hue) {
        marker.setIcon(BitmapDescriptorFactory.defaultMarker(hue));
    }

    String getId() {
        return _id;
    }

    abstract int getType();
    abstract Drawable getIcon(Context context);
    abstract void setDefaultMarkerColor();
}
