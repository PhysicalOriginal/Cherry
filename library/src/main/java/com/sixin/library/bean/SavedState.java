package com.sixin.library.bean;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
/*
* 用户操作数据保存类
* */
public class SavedState extends View.BaseSavedState {
    //todo 完善的点，对于非法参数进行校验，不进行序列化和反序列化
    //todo 随着后期的扩展，这个类的构造函数将会变的非常的庞大，如何解决，这个类的变动将会让引起LockView内部代码变化很大
    private int dotCount;
    private List<Dot> touchedDots = new ArrayList<>();
    private boolean invalidatePath;
    private int verifyMode;
    private int normalColor;
    private int correctColor;
    private int dotNormalSize;
    private int animDuration;
    private int dotSelectedSize;
    private int pathWidth;
    private boolean feedbackEnabled;

    public SavedState(Parcelable source, int dotCount, List<Dot> touchedDots
            , boolean invalidatePath, int verifyMode, int normalColor
            , int correctColor, int dotNormalSize, int animDuration
            , int dotSelectedSize, int pathWidth, boolean feedbackEnabled) {
        super(source);
        this.dotCount = dotCount;
        this.touchedDots.addAll(touchedDots);
        this.invalidatePath = invalidatePath;
        this.verifyMode = verifyMode;
        this.normalColor = normalColor;
        this.correctColor = correctColor;
        this.dotNormalSize = dotNormalSize;
        this.animDuration = animDuration;
        this.dotSelectedSize = dotSelectedSize;
        this.pathWidth = pathWidth;
        this.feedbackEnabled = feedbackEnabled;
    }

    private SavedState(Parcel source) {
        super(source);
        dotCount = source.readInt();
        source.readTypedList(touchedDots,Dot.CREATOR);
        this.invalidatePath = source.readByte() != 0;
        this.verifyMode = source.readInt();
        this.normalColor = source.readInt();
        this.correctColor = source.readInt();
        this.dotNormalSize = source.readInt();
        this.animDuration = source.readInt();
        this.dotSelectedSize = source.readInt();
        this.pathWidth = source.readInt();
        this.feedbackEnabled = source.readByte() != 0;
    }

    public int getDotCount() {
        return dotCount;
    }

    public List<Dot> getTouchedDots() {
        return touchedDots;
    }

    public boolean isInvalidatePath() {
        return invalidatePath;
    }

    public int getVerifyMode() {
        return verifyMode;
    }

    public int getNormalColor() {
        return normalColor;
    }

    public int getCorrectColor() {
        return correctColor;
    }

    public int getDotNormalSize() {
        return dotNormalSize;
    }

    public int getAnimDuration() {
        return animDuration;
    }

    public int getDotSelectedSize() {
        return dotSelectedSize;
    }

    public int getPathWidth() {
        return pathWidth;
    }

    public boolean isFeedbackEnabled() {
        return feedbackEnabled;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(dotCount);
        out.writeTypedList(touchedDots);
        out.writeByte(this.invalidatePath?(byte)1:(byte)0);
        out.writeInt(verifyMode);
        out.writeInt(normalColor);
        out.writeInt(correctColor);
        out.writeInt(dotNormalSize);
        out.writeInt(animDuration);
        out.writeInt(dotSelectedSize);
        out.writeInt(pathWidth);
        out.writeByte(this.feedbackEnabled?(byte)1:(byte)0);
    }

    public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {
        @Override
        public SavedState createFromParcel(Parcel source) {
            return new SavedState(source);
        }

        @Override
        public SavedState[] newArray(int size) {
            return new SavedState[size];
        }
    };
}
