package com.isaacsufyan.myimagecrop;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.FileDescriptor;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public class BitmapManager {

    private static final String TAG = "BitmapManager";

    private static enum State {CANCEL, ALLOW}

    private static class ThreadStatus {

        public State mState = State.ALLOW;
        public BitmapFactory.Options mOptions;

        @NonNull
        @Override
        public String toString() {

            String s;
            if (mState == State.CANCEL) {
                s = "Cancel";
            } else if (mState == State.ALLOW) {
                s = "Allow";
            } else {
                s = "?";
            }
            s = "thread state = " + s + ", options = " + mOptions;
            return s;
        }
    }

    public static class ThreadSet implements Iterable<Thread> {

        private final WeakHashMap<Thread, Object> mWeakCollection =
                new WeakHashMap<>();

        public void add(Thread t) {

            mWeakCollection.put(t, null);
        }

        public void remove(Thread t) {

            mWeakCollection.remove(t);
        }

        @NonNull
        public Iterator<Thread> iterator() {

            return mWeakCollection.keySet().iterator();
        }
    }

    private final WeakHashMap<Thread, ThreadStatus> mThreadStatus =
            new WeakHashMap<>();

    private static BitmapManager sManager = null;

    private BitmapManager() {

    }

    private synchronized ThreadStatus getOrCreateThreadStatus(Thread t) {

        ThreadStatus status = mThreadStatus.get(t);
        if (status == null) {
            status = new ThreadStatus();
            mThreadStatus.put(t, status);
        }
        return status;
    }

    private synchronized void setDecodingOptions(Thread t,
                                                 BitmapFactory.Options options) {

        getOrCreateThreadStatus(t).mOptions = options;
    }

    synchronized BitmapFactory.Options getDecodingOptions(Thread t) {

        ThreadStatus status = mThreadStatus.get(t);
        return status != null ? status.mOptions : null;
    }

    synchronized void removeDecodingOptions(Thread t) {
        ThreadStatus status = mThreadStatus.get(t);
        if (status != null) {
            status.mOptions = null;
        }
    }

    public synchronized void allowThreadDecoding(ThreadSet threads) {

        for (Thread t : threads) {
            allowThreadDecoding(t);
        }
    }

    public synchronized void cancelThreadDecoding(ThreadSet threads) {

        for (Thread t : threads) {
            cancelThreadDecoding(t);
        }
    }

    public synchronized boolean canThreadDecoding(Thread t) {

        ThreadStatus status = mThreadStatus.get(t);
        if (status == null) {
            return true;
        }

        return (status.mState != State.CANCEL);
    }

    public synchronized void allowThreadDecoding(Thread t) {
        getOrCreateThreadStatus(t).mState = State.ALLOW;
    }

    public synchronized void cancelThreadDecoding(Thread t) {

        ThreadStatus status = getOrCreateThreadStatus(t);
        status.mState = State.CANCEL;
        if (status.mOptions != null) {
            status.mOptions.requestCancelDecode();
        }
        notifyAll();
    }

    public synchronized void dump() {
        for (Map.Entry<Thread, ThreadStatus> entry : mThreadStatus.entrySet()) {
            Log.v(TAG, "[Dump] Thread " + entry.getKey() + " ("
                    + entry.getKey().getId()
                    + ")'s status is " + entry.getValue());
        }
    }

    public static synchronized BitmapManager instance() {

        if (sManager == null) {
            sManager = new BitmapManager();
        }
        return sManager;
    }

    /**
     * The real place to delegate bitmap decoding to BitmapFactory.
     */
    public Bitmap decodeFileDescriptor(FileDescriptor fd,
                                       BitmapFactory.Options options) {

        if (options.mCancel) {
            return null;
        }

        Thread thread = Thread.currentThread();
        if (!canThreadDecoding(thread)) {
            // History.d(TAG, "Thread " + thread + " is not allowed to decode.");
            return null;
        }

        setDecodingOptions(thread, options);
        Bitmap b = BitmapFactory.decodeFileDescriptor(fd, null, options);

        removeDecodingOptions(thread);
        return b;
    }
}
