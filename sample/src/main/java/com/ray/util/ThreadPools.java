package com.ray.util;

import android.os.AsyncTask;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class ThreadPools {

    private static ExecutorService mIOPool,mGlobalCachePool,serialPool;
    static {
        mIOPool= Executors.newFixedThreadPool(2, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r,"IO-POOL");
            }
        });

        mGlobalCachePool= Executors.newCachedThreadPool();
        serialPool = Executors.newFixedThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r,"SERIAL-POOL");
            }
        });
    }

    private ThreadPools() {
        throw new UnsupportedOperationException();
    }

    /**
     * IO读写线程池,最多二个同时执行
     * @return
     */
    public static Executor ioPool(){
        return mIOPool;
    }

    /**
     * 繁重 任务线程池，适用于像ImageLoader转换图像这种时间不长但又很占CPU的任务
     * 排队执行的ThreadPool,核心线程为CORE_POOL_SIZE+1个
     * @return
     */
    public static Executor cpuPool(){
        return AsyncTask.THREAD_POOL_EXECUTOR;
    }



    /**
     * 全局cachePool,适用于AsyncHttpClient等不限制任务数的请求
     * @return
     */
    public static ExecutorService getDefault() {
        return mGlobalCachePool;
    }

    /**
     * 串行线程池
     * @return
     */
    public static ExecutorService serialPool() {
        return serialPool;
    }
}
