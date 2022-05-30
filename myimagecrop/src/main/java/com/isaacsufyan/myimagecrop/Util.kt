package com.isaacsufyan.myimagecrop;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Handler;
import android.view.Surface;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Util {

    private Util() {
    }

    public static Bitmap transform(Matrix scaler,
                                   Bitmap source,
                                   int targetWidth,
                                   int targetHeight,
                                   boolean scaleUp) {

        int deltaX = source.getWidth() - targetWidth;
        int deltaY = source.getHeight() - targetHeight;
        if (!scaleUp && (deltaX < 0 || deltaY < 0)) {

            Bitmap b2 = Bitmap.createBitmap(targetWidth, targetHeight,
                    Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b2);

            int deltaXHalf = Math.max(0, deltaX / 2);
            int deltaYHalf = Math.max(0, deltaY / 2);
            Rect src = new Rect(
                    deltaXHalf,
                    deltaYHalf,
                    deltaXHalf + Math.min(targetWidth, source.getWidth()),
                    deltaYHalf + Math.min(targetHeight, source.getHeight()));
            int dstX = (targetWidth - src.width()) / 2;
            int dstY = (targetHeight - src.height()) / 2;
            Rect dst = new Rect(
                    dstX,
                    dstY,
                    targetWidth - dstX,
                    targetHeight - dstY);
            c.drawBitmap(source, src, dst, null);
            return b2;
        }
        float bitmapWidthF = source.getWidth();
        float bitmapHeightF = source.getHeight();

        float bitmapAspect = bitmapWidthF / bitmapHeightF;
        float viewAspect = (float) targetWidth / targetHeight;

        float scale;
        if (bitmapAspect > viewAspect) {
            scale = targetHeight / bitmapHeightF;
        } else {
            scale = targetWidth / bitmapWidthF;
        }
        if (scale < .9F || scale > 1F) {
            scaler.setScale(scale, scale);
        } else {
            scaler = null;
        }

        Bitmap b1;
        if (scaler != null) {
            b1 = Bitmap.createBitmap(source, 0, 0,
                    source.getWidth(), source.getHeight(), scaler, true);
        } else {
            b1 = source;
        }

        int dx1 = Math.max(0, b1.getWidth() - targetWidth);
        int dy1 = Math.max(0, b1.getHeight() - targetHeight);

        Bitmap b2 = Bitmap.createBitmap(
                b1,
                dx1 / 2,
                dy1 / 2,
                targetWidth,
                targetHeight);

        if (b1 != source) {
            b1.recycle();
        }

        return b2;
    }

    public static void closeSilently(Closeable c) {

        if (c == null) return;
        try {
            c.close();
        } catch (Throwable t) {
            // do nothing
        }
    }

    private static class BackgroundJob
            extends MonitoredActivity.LifeCycleAdapter implements Runnable {

        private final MonitoredActivity mActivity;
        private final ProgressDialog mDialog;
        private final Runnable mJob;
        private final Handler mHandler;
        private final Runnable mCleanupRunner = new Runnable() {
            public void run() {

                mActivity.removeLifeCycleListener(BackgroundJob.this);
                if (mDialog.getWindow() != null) mDialog.dismiss();
            }
        };

        public BackgroundJob(MonitoredActivity activity, Runnable job,
                             ProgressDialog dialog, Handler handler) {

            mActivity = activity;
            mDialog = dialog;
            mJob = job;
            mActivity.addLifeCycleListener(this);
            mHandler = handler;
        }

        public void run() {

            try {
                mJob.run();
            } finally {
                mHandler.post(mCleanupRunner);
            }
        }


        @Override
        public void onActivityDestroyed(MonitoredActivity activity) {
            mCleanupRunner.run();
            mHandler.removeCallbacks(mCleanupRunner);
        }

        @Override
        public void onActivityStopped(MonitoredActivity activity) {

            mDialog.hide();
        }

        @Override
        public void onActivityStarted(MonitoredActivity activity) {

            mDialog.show();
        }
    }

    public static void startBackgroundJob(MonitoredActivity activity,
                                          String title, String message, Runnable job, Handler handler) {
        ProgressDialog dialog = ProgressDialog.show(
                activity, title, message, true, false);
        new Thread(new BackgroundJob(activity, job, dialog, handler)).start();
    }


    public static BitmapFactory.Options createNativeAllocOptions() {
        return new BitmapFactory.Options();
    }

    public static Bitmap rotateImage(Bitmap src, float degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }

    public static int getOrientationInDegree(Activity activity) {

        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        return degrees;
    }

    public static void copyStream(InputStream input, OutputStream output)
            throws IOException
    {

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1)
        {
            output.write(buffer, 0, bytesRead);
        }
    }

    public static void startCropImageFromGallery(Activity activity, File file, int requestCode) {
        Intent intent = new Intent(activity, CropImage.class);
        intent.putExtra(CropImage.IMAGE_PATH, file.getPath());
        intent.putExtra(CropImage.SCALE, true);
        intent.putExtra(CropImage.ASPECT_X, 10);
        intent.putExtra(CropImage.ASPECT_Y, 10);
        activity.startActivityForResult(intent, requestCode);
    }
}
