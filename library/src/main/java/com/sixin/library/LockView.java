package com.sixin.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class LockView extends View {

    //todo 当通过纯java代码创建该view对象的时候，也能够实现UI效果
    //TODO lOG日志最后全部撤销
    private static final String TAG = "LockView";

    //默认点的个数
    private static final int S_DEFAULT_DOT_COUNT = 3;
    //默认点被选中时的动画周期
    private static final int S_DEFAULT_DOT_SELECTED_ANIM_DURATION = 1500;

    /**
     * 点的个数，LockView中点的总数是mDotCount*mDotCount;
     * 注意mDotCount的个数被限制在大于0小于等于10的区间内，否则会抛出异常
     * */
    private int mDotCount;

    /**
     * 正常状态时点的大小
     * */
    private int mDotNormalSize;

    /**
     * 点被选中时的动画周期
     * */
    private int mDotSelectedDuration;

    /**
     * 正常状态时点的颜色
     * */
    private int mDotNormalColor;

    /**
     * 点被选中时的颜色
     * */
    private int mDotSelectedColor;

    /**
     * 路径的宽度
     * */
    private int mPathWidth;

    /**
     * 路径绘制错误的颜色
     * */
    private int mColorWrong;

    /**
     * 每个Dot所在区域的宽和高
     * */
    private float mGridWidth;
    private float mGridHeight;

    /**
     * 点的半径
     * */
    private float mRadius;

    /**
     * 点的画笔
     * */
    private Paint mDotPaint;

    public LockView(Context context) {
        this(context, null);
    }

    public LockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //读取自定义属性
        readCustomAttrs(context,attrs);

        initView();
    }

    private void readCustomAttrs(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LockView);
        mDotCount = typedArray.getInteger(R.styleable.LockView_dotCount,S_DEFAULT_DOT_COUNT);
        if (mDotCount <= 0 || mDotCount >10) {
            throw new IllegalArgumentException("dotCount must be > 0 and <= 10");
        }
        mDotNormalSize = typedArray.getDimensionPixelSize(R.styleable.LockView_dotNormalSize,getResources().getDimensionPixelSize(R.dimen.dotNormalSize));
        if (mDotNormalSize <= 0) {
            throw new IllegalArgumentException("mDotNormalSize must be >= 0");
        }
        mDotSelectedDuration = typedArray.getInteger(R.styleable.LockView_dotSelectedAnimDuration, S_DEFAULT_DOT_SELECTED_ANIM_DURATION);
        mDotNormalColor = typedArray.getColor(R.styleable.LockView_dotNormalColor, getResources().getColor(R.color.ColorDotNormal));
        mDotSelectedColor = typedArray.getColor(R.styleable.LockView_dotSelectedColor, getResources().getColor(R.color.ColorDotSelected));
        //todo 注意线条宽度比正常圆大的问题
        mPathWidth = typedArray.getDimensionPixelSize(R.styleable.LockView_pathWidth, getResources().getDimensionPixelSize(R.dimen.pathWidth));
        mColorWrong = typedArray.getColor(R.styleable.LockView_wrongColor, getResources().getColor(R.color.ColorWrong));
        typedArray.recycle();
    }

    private void initView() {
        mRadius = mDotNormalSize / 2f;

        mDotPaint = new Paint();
        mDotPaint.setAntiAlias(true);
        mDotPaint.setDither(true);
        mDotPaint.setColor(mDotNormalColor);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(resolveMeasureSpec(widthMeasureSpec), resolveMeasureSpec(heightMeasureSpec));
    }

    private int resolveMeasureSpec(int measureSpec) {
        int newMeasureSpec = measureSpec;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        //todo 点的大小过大，会超出父view限定的尺寸  当父view的高度是100dp，子view依据内容测算高度，可能会超出去，看android自身view的计算方式
        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
            case MeasureSpec.EXACTLY:
                newMeasureSpec = measureSpec;
                break;
            case MeasureSpec.AT_MOST:
                newMeasureSpec = MeasureSpec.makeMeasureSpec(mDotNormalSize * mDotCount, specMode);
                break;
        }
        return newMeasureSpec;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        int adjustWidth = w - getPaddingStart() - getPaddingEnd();
        int adjustHeight = h - getPaddingTop() - getPaddingBottom();
        mGridWidth = adjustWidth / (float)mDotCount;
        mGridHeight = adjustHeight / (float)mDotCount;

        float limitValue = Math.min(mGridHeight, mGridWidth);
        //todo 该异常提示不够清晰，需要重新提示
        if (mDotNormalSize > limitValue) {
            throw new IllegalArgumentException("Points are smaller than the shortest edge of a rectangle");
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //todo canvas的save和restore   h5画布的学习
        float offsetWidth = mGridWidth;
        float offsetHeight = mGridHeight;
        for (int i = 0; i < mDotCount; i++) {
            for (int j = 0; j < mDotCount; j++) {
                float cx = getPaddingStart() + mGridWidth/2 + j * offsetWidth;
                float cy = getPaddingTop() + mGridHeight/2 + i * offsetHeight;
                drawCircle(canvas, cx, cy);
            }
        }
    }

    private void drawCircle(Canvas canvas, float cx, float cy) {
        canvas.drawCircle(cx,cy,mRadius,mDotPaint);
    }
}
