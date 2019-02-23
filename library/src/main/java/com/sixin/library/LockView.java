package com.sixin.library;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import static com.sixin.library.LockView.VerifyMode.CORRECT;
import static com.sixin.library.LockView.VerifyMode.NORMAL;
import static com.sixin.library.LockView.VerifyMode.WRONG;

public class LockView extends View {
    //todo 测试内存占用，防止横竖屏有些对象无法释放
    //todo 中途没有触摸的点暂时不提供自动响应功能
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
     * 正常状态时的颜色
     * */
    private int mNormalColor;

    /**
     * 验证正确的颜色
     * */
    private int mCorrectColor;

    /**
     * 验证错误的颜色
     * */
    private int mColorWrong;

    /**
     * 路径的宽度
     * */
    private int mPathWidth;

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
    private boolean[][] mDotDown;
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

    private LockViewListener mLockViewListener;

    private int mVerifyMode = NORMAL;

    @IntDef({NORMAL,CORRECT,WRONG})
    @Retention(RetentionPolicy.SOURCE)
    public @interface VerifyMode {

        int NORMAL = 0;

        int CORRECT = 1;

        int WRONG = 2;

    }

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
        mNormalColor = typedArray.getColor(R.styleable.LockView_normalColor, getResources().getColor(R.color.ColorNormal));
        mCorrectColor = typedArray.getColor(R.styleable.LockView_correctColor, getResources().getColor(R.color.ColorCorrect));
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
        mDotPaint.setColor(mNormalColor);
        //todo 添加线条颜色属性
        mLinePath = new Path();
        mLinePaint = new Paint();
        mLinePaint.setAntiAlias(true);
        mLinePaint.setDither(true);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeJoin(Paint.Join.ROUND);
        mLinePaint.setStrokeCap(Paint.Cap.ROUND);
        mLinePaint.setColor(mNormalColor);
        mLinePaint.setStrokeWidth(mPathWidth);

        mTouchedDots = new ArrayList<>();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        initDots();
        configDotDown();
    }

    private void initDots() {
        float radius = mDotNormalSize/2f;
        mDots = new Dot[mDotCount][mDotCount];
        mDotDown = new boolean[mDotCount][mDotCount];
        int dotValue =0;
        for (int i = 0; i < mDotCount; i++) {
            for (int j = 0; j < mDotCount; j++) {
                mDots[i][j] = new Dot(i,j,radius,++dotValue);
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
        mDotDown = null;
        initDots();
        mInvalidatePath = false;
        mInvalidateProgress = false;
        if(mTouchedDots != null){
            mTouchedDots.clear();
        }
        cancelDotAnim();
        requestLayout();
        invalidate();
    }

    /**
     * 设置验证模式
     * @param verifyMode @VerifyMode描述，只接收CORRECT,WRONG，NORMAL三个值
     * */
    //todo 测试该方法，在正常情况下以及横竖屏切换情况下
    public void setVerifyMode(@VerifyMode int verifyMode) {
        mVerifyMode = verifyMode;
        invalidate();
    }

    public LockViewListener getLockViewListener() {
        return mLockViewListener;
    }

    //todo 设置监听器后的内存泄漏测试,去掉detach中的内容测试
    public void setLockViewListener(LockViewListener lockViewListener) {
        clearListener();
        this.mLockViewListener = lockViewListener;
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
    protected void onDraw(Canvas canvas) {
        //todo 这个部分无限制的循环绘制，能不能控制绘制次数
        //todo path有rewind能提高性能，drawCricle有没有类似提高性能的API
        for (int i = 0; i < mDotCount; i++) {
            for (int j = 0; j < mDotCount; j++) {
                float cx = getCx(j);
                float cy = getCy(i);
                Log.d(TAG,"i:"+i+" j"+j+" "+mDotDown[i][j]);
                drawCircle(canvas, cx, cy,mDots[i][j],mDotDown[i][j]);
            }
        }
        Log.d(TAG, "----------");
        if (mInvalidatePath) {
            float lastX = -1f;
            float lastY = -1f;
            Log.d(TAG, "" + mInvalidatePath + " " + mInvalidateProgress);
            if (mTouchedDots.size() == 0) {
                return;
            }

            mLinePaint.setColor(getCurrentColor(true));

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

    private void drawCircle(Canvas canvas, float cx, float cy,Dot dot,boolean dotDown) {
        if (dot != null) {
            mDotPaint.setColor(getCurrentColor(dotDown));
            canvas.drawCircle(cx,cy,dot.getRadius(),mDotPaint);
        }
    }

    /**
     * 获取当前画笔的颜色
     * @param dotDown 点是否被按下
     * @return 返回颜色值
     * */
    private int getCurrentColor(boolean dotDown){
        int colorRes = -1;
        if (!dotDown || mInvalidateProgress || mVerifyMode == NORMAL) {
            colorRes = mNormalColor;
        } else if (mVerifyMode == CORRECT) {
            colorRes = mCorrectColor;
        } else if (mVerifyMode == WRONG) {
            colorRes = mColorWrong;
        }
        return colorRes;
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
                if(!mTouchedDots.isEmpty()){
                    int count = mTouchedDots.size();
                    int[] result = new int[count];
                    for (int i = 0; i < count; i++) {
                        result[i] = mTouchedDots.get(i).getDotValue();
                    }
                    if (mLockViewListener != null) {
                        mLockViewListener.onComplete(result);
                    }
                }
                handleActionUp(event);
                return true;
            //描述一次事件流被中断
            //在手指触摸屏幕的过程中，旋转了屏幕
            //当手指触摸屏幕的过程中，息屏
            //当控件收到前驱事件（什么叫前驱事件？一个从DOWN一直到UP的所有事件组合称为完整的手势，中间的任
            // 意一次事件对于下一个事件而言就是它的前驱事件）之后，后面的事件如果被父控件拦截
            // ，那么当前控件就会收到一个CANCEL事件
            case MotionEvent.ACTION_CANCEL:
                Log.d(TAG, "onCancel");
                if (mLockViewListener != null) {
                    Log.d(TAG, "-------");
                    mLockViewListener.onCancel();
                }
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
            if (dot != null) {
                if (touchedDotSize == 1) {
                    mInvalidateProgress = true;
                    if (mLockViewListener != null) {
                        mLockViewListener.onStart(dot.getDotValue());
                    }
                } else if (touchedDotSize > 1) {
                    if (mLockViewListener != null) {
                        mLockViewListener.onProgress(dot.getDotValue());
                    }
                }
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
            mDotDown[dot.getRow()][dot.getColumn()] = true;
            startScaleAnimation(dot);
            if (mFeedbackEnabled) {
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            }
        }

        return dot;
    }

    private void handleActionDown(MotionEvent event) {
        resetTouchedDot();
        float xDown = event.getX();
        float yDown = event.getY();
        Dot dot = touchDotAndFeedback(xDown, yDown);
        mInvalidatePath = false;
        if (dot != null) {
            mInvalidateProgress = true;
            if (mLockViewListener != null && mTouchedDots.size() == 1) {
                mLockViewListener.onStart(dot.getDotValue());
            }
        }else{
            mInvalidateProgress = false;
        }
        xInProgress = xDown;
        yInProgress = yDown;
        invalidate();
    }

    private void resetTouchedDot() {
        if (mDotDown != null) {
            int row = mDotDown.length;
            int column = row;
            for (int r = 0; r < row; r++) {
                for (int c = 0; c < column; c++) {
                    mDotDown[r][c] = false;
                }
            }
        }

        if(mTouchedDots != null && !mTouchedDots.isEmpty()){
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

    //屏幕在旋转过程中方法回调顺序：onSaveInstanceState--->detach----->构造函数----->onRestoreInstanceState---->attach
    @Override
    protected Parcelable onSaveInstanceState() {
        Log.d(TAG, "onSave");
        Parcelable parcelable = super.onSaveInstanceState();
        return new SavedState(parcelable,mDotCount
                ,mTouchedDots,mInvalidatePath,mVerifyMode);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        //todo 测试旋转屏幕后，接口回调
        mDotCount = savedState.getDotCount();
        mTouchedDots = savedState.getTouchedDots();
        mInvalidatePath = savedState.isInvalidatePath();
        mVerifyMode = savedState.getVerifyMode();
    }

    private void configDotDown() {
        if (mTouchedDots != null && mTouchedDots.size() > 0 && mDotDown != null) {
            for (Dot dot : mTouchedDots) {
                mDotDown[dot.getRow()][dot.getColumn()] = true;
            }
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
        //todo 这个方法需要在那些地方回调
        clearListener();
        //todo 考察在这其中还需要释放那些资源
    }

    private void clearListener() {
        if (mLockViewListener != null) {
            mLockViewListener = null;
        }
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
    private static class Dot implements Parcelable {

        private int row;
        private int column;
        private float radius;
        private boolean isAnim;
        private int dotValue;

        Dot(int row, int column,float radius,int dotValue) {
            this.row = row;
            this.column = column;
            this.radius = radius;
            this.dotValue = dotValue;
        }

        private int getRow() {
            return row;
        }

        private int getColumn() {
            return column;
        }

        private float getRadius() {
            return radius;
        }

        private void setRadius(float radius) {
            this.radius = radius;
        }

        private boolean isAnim() {
            return isAnim;
        }

        private void setAnim(boolean anim) {
            isAnim = anim;
        }

        private int getDotValue() {
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

    /**
     * 在滑动过程中的回调监听器
     * */
    public interface LockViewListener{

        /**
         * 触碰到第一个点时被回调
         * @param dotValue 被触碰点的数值
         * */
        void onStart(int dotValue);

        /**
         * 滑动过程中触碰到点被回调
         * @param progressValue 被触碰点的数值
         * */
        void onProgress(int progressValue);

        /**
         * 滑动结束被回调
         * @param result 整个滑动过程产生的最终结果
         * */
        void onComplete(int[] result);

        /**
         * 滑动过程被终止时回掉的方法
         * */
        void onCancel();
    }

    private static class SavedState extends BaseSavedState{

        private int dotCount;
        private List<Dot> touchedDots = new ArrayList<>();
        private boolean invalidatePath;
        private int verifyMode;

        private SavedState(Parcelable source,int dotCount,List<Dot> touchedDots
                ,boolean invalidatePath,int verifyMode) {
            super(source);
            this.dotCount = dotCount;
            this.touchedDots.addAll(touchedDots);
            this.invalidatePath = invalidatePath;
            this.verifyMode = verifyMode;
        }

        private SavedState(Parcel source) {
            super(source);
            dotCount = source.readInt();
            source.readTypedList(touchedDots,Dot.CREATOR);
            this.invalidatePath = source.readByte() != 0;
            this.verifyMode = source.readInt();
        }

        private int getDotCount() {
            return dotCount;
        }

        private List<Dot> getTouchedDots() {
            return touchedDots;
        }

        private boolean isInvalidatePath() {
            return invalidatePath;
        }

        private int getVerifyMode() {
            return verifyMode;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(dotCount);
            out.writeTypedList(touchedDots);
            out.writeByte(this.invalidatePath?(byte)1:(byte)0);
            out.writeInt(verifyMode);
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
}
