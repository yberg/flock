package com.yberg.android.flock;

/**
 * Created by Viktor on 2016-10-10.
 */

public class Tag {

    private int type;
    private MapLocation associated;

    public Tag(int type, MapLocation associated) {
        this.type = type;
        this.associated = associated;
    }

    public int getType() {
        return type;
    }

    public MapLocation getAssociated() {
        return associated;
    }
}
