package com.yberg.android.flock;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by Viktor on 2016-10-09.
 */

public class Favorite extends MapLocation {

    private static final float DEFAULT_MARKER_COLOR = BitmapDescriptorFactory.HUE_ORANGE;
    private static int TYPE = MapLocation.FAVORITE;

    public Favorite(String name, String _id, Marker marker) {
        this.name = name;
        this._id = _id;
        this.marker = marker;
        this.marker.setTitle(name);
        this.marker.setIcon(BitmapDescriptorFactory.defaultMarker(DEFAULT_MARKER_COLOR));
        this.marker.setTag(new Tag(TYPE, this));
    }

    @Override
    public int getType() {
        return TYPE;
    }

    @Override
    public Drawable getIcon(Context context) {
        return ContextCompat.getDrawable(context, R.drawable.ic_star_black_48dp);
    }

    @Override
    public void setDefaultMarkerColor() {
        marker.setIcon(BitmapDescriptorFactory.defaultMarker(DEFAULT_MARKER_COLOR));
    }

}
