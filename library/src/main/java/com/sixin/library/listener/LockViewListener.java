package com.sixin.library.listener;
/**
 * 在滑动过程中的回调监听器
 * */
public interface LockViewListener {

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
