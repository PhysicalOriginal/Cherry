package com.sixin.library;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.IntRange;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class LockView extends View {

    //todo 当通过纯java代码创建该view对象的时候，也能够实现UI效果
    //TODO lOG日志最后全部撤销
    //TODO 提供get set方法 当提供这些方法后因为内部数据可能没有同步更新，会出现很多的bug.
    //todo 目前点的位置的计算方式对于开发者而言可能不友好，尤其定制化UI时存在很大问题，无法变动位置
    private static final String TAG = "LockView";

    //默认点的个数
    private static final int S_DEFAULT_DOT_COUNT = 3;
    //默认点被选中时的动画周期
    private static final int S_DEFAULT_DOT_SELECTED_ANIM_DURATION = 1500;
    //todo 该变量暂时不作为属性向外抛
    //todo 完善方法的文档说明
    private static final float S_GRID_SCALE_FACTOR = 0.6f;
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
     * 点被选中时的大小
     * */
    private int mDotSelectedSize;

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
     * 触摸点的时候是否震动反馈
     * */
    private boolean mFeedbackEnabled;

    /**
     * 点的画笔
     * */
    private Paint mDotPaint;

    /**
     * 绘制线条路径的画笔
     * */
    private Paint mLinePaint;

    /**
     * 线条路径
     * */
    private Path mLinePath;

    private Dot[][] mDots;
    /**
     * 已经被触摸的点的集合
     * */
    private List<Dot> mTouchedDots;

    private List<ObjectAnimator> objectAnimators = new ArrayList<>();

    /**
     * 是否绘制路径的标志
     * */
    private boolean mInvalidatePath;
    /**
     * 是否在绘制过程中
     * */
    private boolean mInvalidateProgress;
    /**
     * 手指滑动过程中的坐标位置
     * */
    private float xInProgress;
    private float yInProgress;

    private Rect mTempInvalidateRect = new Rect();
    private Rect mInvalidateRect = new Rect();

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
        mDotSelectedSize = typedArray.getDimensionPixelSize(R.styleable.LockView_dotSelectedSize,getResources().getDimensionPixelSize(R.dimen.dotSelectedSize));
        if (mDotSelectedSize <= 0) {
            throw new IllegalArgumentException("mDotSelectedSize must be >= 0");
        }
        mDotSelectedDuration = typedArray.getInteger(R.styleable.LockView_dotSelectedAnimDuration, S_DEFAULT_DOT_SELECTED_ANIM_DURATION);
        mDotNormalColor = typedArray.getColor(R.styleable.LockView_dotNormalColor, getResources().getColor(R.color.ColorDotNormal));
        mDotSelectedColor = typedArray.getColor(R.styleable.LockView_dotSelectedColor, getResources().getColor(R.color.ColorDotSelected));
        //todo 注意线条宽度比正常圆大的问题
        mPathWidth = typedArray.getDimensionPixelSize(R.styleable.LockView_pathWidth, getResources().getDimensionPixelSize(R.dimen.pathWidth));
        mColorWrong = typedArray.getColor(R.styleable.LockView_wrongColor, getResources().getColor(R.color.ColorWrong));
        mFeedbackEnabled = typedArray.getBoolean(R.styleable.LockView_feedbackEnabled, true);
        typedArray.recycle();
    }

    private void initView() {

        mDotPaint = new Paint();
        mDotPaint.setAntiAlias(true);
        mDotPaint.setDither(true);
        mDotPaint.setColor(mDotNormalColor);
        //todo 添加线条颜色属性
        mLinePath = new Path();
        mLinePaint = new Paint();
        mLinePaint.setAntiAlias(true);
        mLinePaint.setDither(true);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeJoin(Paint.Join.ROUND);
        mLinePaint.setStrokeCap(Paint.Cap.ROUND);
        mLinePaint.setColor(mDotNormalColor);
        mLinePaint.setStrokeWidth(mPathWidth);

        mTouchedDots = new ArrayList<>();

        initDots();
    }

    private void initDots() {
        float radius = mDotNormalSize/2f;
        mDots = new Dot[mDotCount][mDotCount];
        for (int i = 0; i < mDotCount; i++) {
            for (int j = 0; j < mDotCount; j++) {
                mDots[i][j] = new Dot(i,j,radius);
            }
        }
    }

    public int getDotCount() {
        return mDotCount;
    }
    //todo 考察开发者在任意地方调用该代码
    //1:在onCreate实例化之后调用
    //2：在回调接口中调用  绘制了若干次，再按按钮
    //todo 在碎片和activity的生命周期中回调
    /**
     * 设置LockView每行点的个数
     * @param dotCount 每行点的个数，注意使用了@IntRange限制了dotCount的大小。dotCount在0到9范围内
     * */
    public void setDotCount(@IntRange(from =0,to = 9) int dotCount) {
        this.mDotCount = dotCount;
        mDots = null;
        initDots();
        mInvalidatePath = false;
        mInvalidateProgress = false;
        if(mTouchedDots != null){
            mTouchedDots.clear();
        }
        cancelDotAnim();
        //todo 什么情况下requestLayout不进行重绘
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(resolveMeasureSpec(widthMeasureSpec,getPaddingStart()+getPaddingEnd())
                , resolveMeasureSpec(heightMeasureSpec,getPaddingTop()+getPaddingBottom()));
    }

    private int resolveMeasureSpec(int measureSpec,int padding) {
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
                newMeasureSpec = MeasureSpec.makeMeasureSpec(mDotNormalSize * mDotCount+padding, specMode);
                break;
        }
        return newMeasureSpec;
    }

    @Override
    public void layout(int l, int t, int r, int b) {
        super.layout(l, t, r, b);

        int adjustWidth = getWidth() - getPaddingStart() - getPaddingEnd();
        int adjustHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        mGridWidth = adjustWidth / (float)mDotCount;
        mGridHeight = adjustHeight / (float)mDotCount;

        float limitValue = Math.min(mGridHeight, mGridWidth);
        //todo 该异常提示不够清晰，需要重新提示
        if (mDotNormalSize > limitValue) {
            throw new IllegalArgumentException("Points are smaller than the shortest edge of a rectangle");
        }
        //todo 该异常提示不够清晰，需要重新提示
        if (mDotSelectedSize > limitValue) {
            throw new IllegalArgumentException("Points are smaller than the shortest edge of a rectangle");
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
//        Log.d(TAG, "onSizeChanged");
//        int adjustWidth = w - getPaddingStart() - getPaddingEnd();
//        int adjustHeight = h - getPaddingTop() - getPaddingBottom();
//        mGridWidth = adjustWidth / (float)mDotCount;
//        mGridHeight = adjustHeight / (float)mDotCount;
//
//        float limitValue = Math.min(mGridHeight, mGridWidth);
//        //todo 该异常提示不够清晰，需要重新提示
//        if (mDotNormalSize > limitValue) {
//            throw new IllegalArgumentException("Points are smaller than the shortest edge of a rectangle");
//        }
//        //todo 该异常提示不够清晰，需要重新提示
//        if (mDotSelectedSize > limitValue) {
//            throw new IllegalArgumentException("Points are smaller than the shortest edge of a rectangle");
//        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //todo 这个部分无限制的循环绘制，能不能控制绘制次数
        //todo path有rewind能提高性能，drawCricle有没有类似提高性能的API
        for (int i = 0; i < mDotCount; i++) {
            for (int j = 0; j < mDotCount; j++) {
                float cx = getCx(j);
                float cy = getCy(i);
                drawCircle(canvas, cx, cy,mDots[i][j]);
            }
        }

        if (mInvalidatePath) {
            float lastX = -1f;
            float lastY = -1f;

            if (mTouchedDots.size() == 0) {
                return;
            }

            for (int i = 0; i < mTouchedDots.size(); i++) {
                Dot touchedDot = mTouchedDots.get(i);
                if (touchedDot != null) {
                    float cX = getCx(touchedDot.getColumn());
                    float cY = getCy(touchedDot.getRow());
                    if(i != 0){
                        //rewind提高性能
                        mLinePath.rewind();
                        mLinePath.moveTo(lastX,lastY);
                        mLinePath.lineTo(cX,cY);
                        canvas.drawPath(mLinePath,mLinePaint);
                    }
                    lastX = cX;
                    lastY = cY;
                }
            }

            if (mInvalidateProgress) {
                mLinePath.rewind();
                mLinePath.moveTo(lastX, lastY);
                mLinePath.lineTo(xInProgress,yInProgress);
                canvas.drawPath(mLinePath,mLinePaint);
            }
        }
    }

    private float getCx(int column) {
        return getPaddingStart() + mGridWidth/2 + column * mGridWidth;
    }

    private float getCy(int row) {
        return getPaddingTop() + mGridHeight/2 + row * mGridHeight;
    }

    private void drawCircle(Canvas canvas, float cx, float cy,Dot dot) {
        if (dot != null) {
            canvas.drawCircle(cx,cy,dot.getRadius(),mDotPaint);
        }
    }

    //todo 考虑这个警告是否需要处理
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                handleActionDown(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                handleActionMove(event);
                return true;
            case MotionEvent.ACTION_UP:
                handleActionUp(event);
                return true;
            //描述一次事件流被中断
            //当手指触摸屏幕的过程中，息屏
            //当控件收到前驱事件（什么叫前驱事件？一个从DOWN一直到UP的所有事件组合称为完整的手势，中间的任
            // 意一次事件对于下一个事件而言就是它的前驱事件）之后，后面的事件如果被父控件拦截
            // ，那么当前控件就会收到一个CANCEL事件
            case MotionEvent.ACTION_CANCEL:
                Log.d(TAG, "onCancel");
                handleActionUp(event);
                return true;
        }
        return false;
    }

    private void handleActionUp(MotionEvent event) {
        if (!mTouchedDots.isEmpty()) {
            mInvalidateProgress = false;
            mInvalidateRect.setEmpty();
            cancelDotAnim();
            resetDots();
            invalidate();
        }
    }

    private void resetDots() {
        if (mDots != null && mDots.length > 0) {
            int row = mDots.length;
            int column = row;
            for (int i = 0; i < row; i++) {
                for (int j = 0; j < column; j++) {
                    Dot dot = mDots[i][j];
                    dot.setAnim(false);
                    dot.setRadius(mDotNormalSize/2f);
                }
            }
        }
    }

    private void handleActionMove(MotionEvent event) {
        mInvalidatePath = true;
        boolean invalidate = false;
        mTempInvalidateRect.setEmpty();
        int historySize = event.getHistorySize();

        for (int i = 0; i < historySize + 1; i++) {
            float eventX = i < historySize ? event.getHistoricalX(i) : event.getX();
            float eventY = i < historySize ? event.getHistoricalY(i) : event.getY();
            Dot dot = touchDotAndFeedback(eventX, eventY);

            int touchedDotSize = mTouchedDots.size();
            if (dot != null && touchedDotSize == 1) {
                mInvalidateProgress = true;
            }

            float dX = Math.abs(eventX - xInProgress);
            float dY = Math.abs(eventY - yInProgress);
            if (dX > 0.0f || dY > 0.0f) {
                invalidate = true;
            }

            if (mInvalidateProgress && touchedDotSize > 0) {
                Dot lastDot = mTouchedDots.get(touchedDotSize - 1);
                float lastCx = getCx(lastDot.getColumn());
                float lastCy = getCy(lastDot.getRow());
                float left = Math.min(lastCx,eventX) - mPathWidth;
                float right = Math.max(lastCx,eventX) + mPathWidth;
                float top = Math.min(lastCy,eventY) - mPathWidth;
                float bottom = Math.max(lastCy,eventY) + mPathWidth;

                if (dot != null) {
                    float gridWidthHalf = mGridWidth / 2.0f;
                    float gridHeightHalf = mGridHeight / 2.0f;
                    float cX = getCx(dot.getColumn());
                    float cY = getCy(dot.getRow());
                    left = Math.min(cX - gridWidthHalf, left);
                    right = Math.max(cX + gridWidthHalf, right);
                    top = Math.min(cY - gridHeightHalf, top);
                    bottom = Math.max(cY + gridHeightHalf, bottom);
                }

                mTempInvalidateRect.union(Math.round(left),Math.round(top),Math.round(right),Math.round(bottom));
            }
        }
        xInProgress = event.getX();
        yInProgress = event.getY();

        if (invalidate) {
            mInvalidateRect.union(mTempInvalidateRect);
            //todo 该方法已经过时
            invalidate(mInvalidateRect);
        }
    }

    private Dot touchDotAndFeedback(float xProgress, float yProgress) {
        Dot dot = getTouchDot(xProgress, yProgress);

        if (dot != null && dot.isAnim()) {
            return null;
        }

        if (dot != null && !dot.isAnim()) {
            dot.setAnim(true);
            startScaleAnimation(dot);
            if (mFeedbackEnabled) {
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            }
        }

        return dot;
    }

    private void handleActionDown(MotionEvent event) {
        resetTouchedDots();
        float xDown = event.getX();
        float yDown = event.getY();
        Dot dot = touchDotAndFeedback(xDown, yDown);
        mInvalidatePath = false;
        mInvalidateProgress = dot != null;
        xInProgress = xDown;
        yInProgress = yDown;
        invalidate();
    }

    private void resetTouchedDots() {
        if (!mTouchedDots.isEmpty()) {
            mTouchedDots.clear();
        }
    }

    private void startScaleAnimation(final Dot dot) {
        if (dot != null) {
            //todo 注意动画的内存泄漏问题
            //todo 测试选中view与原view一样的大情况和与网格一样大的情况
            float originRadius = mDotNormalSize/2f;
            float selectedRadius = mDotSelectedSize / 2f;
            ObjectAnimator scaleAnimator = ObjectAnimator
                                                .ofFloat(dot,"radius",originRadius,selectedRadius,originRadius)
                                                .setDuration(mDotSelectedDuration);
            scaleAnimator.setInterpolator(new LinearOutSlowInInterpolator());
            scaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    invalidate();
                }
            });

            objectAnimators.add(scaleAnimator);

            scaleAnimator.start();
        }
    }

    /**
     * 根据手指按下的坐标位置，确定触摸的具体Dot是哪一个
     * @param x 手指按下的横坐标
     * @param y 手指按下的纵坐标
     * @return 点的对象，可能为空
     * */
    private Dot getTouchDot(float x, float y) {
        int row = getRow(y);
        int column = getColumn(x);
        if (row < 0 || column < 0) {
            return null;
        }

        float cx = getCx(column);
        float cy = getCy(row);
        float responseAreaWidthHalf = (mGridWidth * S_GRID_SCALE_FACTOR)/2;
        float responseAreaHeightHalf = (mGridHeight * S_GRID_SCALE_FACTOR)/2;
        float radius = mDotNormalSize / 2f;
        boolean isDotBeyondResponseArea = radius > Math.min(responseAreaHeightHalf, responseAreaWidthHalf);
        if (isDotBeyondResponseArea) {
            boolean xInArea = x >= (cx - radius) && x <= (cx + radius);
            boolean yInArea = y >= (cy - radius) && y <= (cy + radius);
            if (xInArea && yInArea) {
                addToTouchedDots(mDots[row][column]);
                return mDots[row][column];
            }else{
                return null;
            }
        }else{
            boolean xInArea = x >= (cx - responseAreaWidthHalf) && x <= (cx + responseAreaWidthHalf);
            boolean yInArea = y >= (cy - responseAreaHeightHalf) && y <= (cy + responseAreaHeightHalf);
            if (xInArea && yInArea) {
                addToTouchedDots(mDots[row][column]);
                return mDots[row][column];
            }else{
                return null;
            }
        }
    }

    /**
     * 将触摸的点添加到集合中
     * @param dot 被触摸的点
     * */
    private void addToTouchedDots(Dot dot) {
        if (dot != null) {
            for (Dot d : mTouchedDots) {
                if (dot == d) {
                   return;
                }
            }
            mTouchedDots.add(dot);
        }
    }

    /**
     * 根据触摸点的纵坐标确定触摸区域所在的行号
     * @param y 触摸点的纵坐标
     * @return 行号 默认返回-1,表示不在触摸区域内
     * */
    private int getRow(float y) {
        int row = -1;
        for (int i = 0; i < mDotCount; i++) {
            if (y >= (getPaddingTop() + i * mGridHeight) && y <= (getPaddingTop() + (i + 1) * mGridHeight)) {
                row = i;
                break;
            }
        }
        return row;
    }

    /**
     * 根据触摸点的横坐标确定触摸区域所在的列号
     * @param x 触摸点的横坐标
     * @return 列号 默认返回-1,表示不在触摸区域内
     * */
    private int getColumn(float x){
        int column = -1;
        for (int i = 0; i < mDotCount; i++) {
            if (x >= (getPaddingStart() + i * mGridWidth) && x <= (getPaddingStart() + (i + 1) * mGridWidth)) {
                column = i;
                break;
            }
        }
        return column;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        //取消动画
        cancelDotAnim();
        //todo 考察在这其中还需要释放那些资源
    }

    private void cancelDotAnim() {
        if(!objectAnimators.isEmpty()){
            for (ObjectAnimator objectAnimator : objectAnimators) {
                if (objectAnimator != null && objectAnimator.isRunning()) {
                    objectAnimator.cancel();
                }
            }
            objectAnimators.clear();
        }

    }

    /**
     * lockView中点的抽象描述
     * */
    private static class Dot{

        private int row;
        private int column;
        private float radius;
        private boolean isAnim;

        Dot(int row, int column,float radius) {
            this.row = row;
            this.column = column;
            this.radius = radius;
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
    }
}
