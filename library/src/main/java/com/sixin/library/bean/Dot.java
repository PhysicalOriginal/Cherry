package com.sixin.library.bean;

import android.os.Parcel;
import android.os.Parcelable;
/**
 * LockView中的点的抽象描述
 * */
public final class Dot implements Parcelable{

    private int row;
    private int column;
    private float radius;
    private boolean isAnim;
    private int dotValue;

    public Dot(int row, int column,float radius,int dotValue) {
        this.row = row;
        this.column = column;
        this.radius = radius;
        this.dotValue = dotValue;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public boolean isAnim() {
        return isAnim;
    }

    public void setAnim(boolean anim) {
        isAnim = anim;
    }

    public int getDotValue() {
        return dotValue;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.row);
        dest.writeInt(this.column);
        dest.writeFloat(this.radius);
        dest.writeByte(this.isAnim ? (byte) 1 : (byte) 0);
        dest.writeInt(this.dotValue);
    }

    private Dot(Parcel in) {
        this.row = in.readInt();
        this.column = in.readInt();
        this.radius = in.readFloat();
        this.isAnim = in.readByte() != 0;
        this.dotValue = in.readInt();
    }

    public static final Parcelable.Creator<Dot> CREATOR = new Parcelable.Creator<Dot>() {
        @Override
        public Dot createFromParcel(Parcel source) {
            return new Dot(source);
        }

        @Override
        public Dot[] newArray(int size) {
            return new Dot[size];
        }
    };
}
