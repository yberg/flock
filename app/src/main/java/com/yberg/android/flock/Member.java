package com.yberg.android.flock;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;

import org.joda.time.DateTime;

/**
 * Created by Viktor on 2016-10-09.
 */

public class Member extends MapLocation {

    private static final float DEFAULT_MARKER_COLOR = BitmapDescriptorFactory.HUE_RED;
    private static int TYPE = MapLocation.MEMBER;

    private DateTime lastUpdated;

    public Member() {
        this.name = "";
        this._id = "";
        this.familyId = "";
    }

    public Member(String name, String _id, String familyId) {
        this(name, _id, familyId, null);
    }

    public Member(String name, String _id, String familyId, Marker marker) {
        this.name = name;
        this._id = _id;
        this.familyId = familyId;
        this.marker = marker;
        if (marker != null) {
            this.marker.setTitle(name);
            this.marker.setIcon(BitmapDescriptorFactory.defaultMarker(DEFAULT_MARKER_COLOR));
            this.marker.setTag(new Tag(TYPE, this));
        }
    }

    @Override
    public int getType() {
        return TYPE;
    }

    @Override
    public Drawable getIcon(Context context) {
        return ContextCompat.getDrawable(context, R.drawable.ic_account_circle_black_48dp);
    }

    @Override
    public void setDefaultMarkerColor() {
        marker.setIcon(BitmapDescriptorFactory.defaultMarker(DEFAULT_MARKER_COLOR));
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
        this.marker.setTitle(name);
        this.marker.setIcon(BitmapDescriptorFactory.defaultMarker(DEFAULT_MARKER_COLOR));
        this.marker.setTag(new Tag(TYPE, this));
    }

    public void setLastUpdated(DateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getLastUpdated() {
        StringBuilder s = new StringBuilder();
        if (lastUpdated.getHourOfDay() < 10) {
            s.append("0");
        }
        s.append(lastUpdated.getHourOfDay());
        s.append(":");
        if (lastUpdated.getMinuteOfHour() < 10) {
            s.append("0");
        }
        s.append(lastUpdated.getMinuteOfHour());
        return s.toString();
    }
}
