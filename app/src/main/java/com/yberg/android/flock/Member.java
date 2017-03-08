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

/**
 * Describes a family member and its marker on the map.
 */
public class Member extends MapLocation {

    private static final float DEFAULT_MARKER_COLOR = BitmapDescriptorFactory.HUE_RED;
    private static int TYPE = MapLocation.MEMBER;

    private DateTime lastUpdated;

    /**
     * Constructs an empty member.
     */
    public Member() {
        this.name = "";
        this._id = "";
        this.familyId = "";
    }

    /**
     * Constructs a member without a marker.
     * @param name The member's name
     * @param _id The member's id
     * @param familyId The member's family id.
     */
    public Member(String name, String _id, String familyId) {
        this(name, _id, familyId, null);
    }

    /**
     * Constructs a member with a marker.
     * @param name The member's name
     * @param _id The member's id
     * @param familyId The member's family id.
     * @param marker The member's map marker.
     */
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

    /**
     * Sets the marker.
     * @param marker The marker.
     */
    public void setMarker(Marker marker) {
        this.marker = marker;
        this.marker.setTitle(name);
        this.marker.setIcon(BitmapDescriptorFactory.defaultMarker(DEFAULT_MARKER_COLOR));
        this.marker.setTag(new Tag(TYPE, this));
    }

    /**
     * Sets the last updated timestamp.
     * @param lastUpdated The last update timestamp.
     */
    public void setLastUpdated(DateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * @return The time when the member's position was last updated.
     */
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
