package com.isaacsufyan.myimagecrop;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.media.FaceDetector;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

public class CropImage extends MonitoredActivity {

    final int IMAGE_MAX_SIZE = 1024;

    private static final String TAG = "TAG CropImage";
    public static final String IMAGE_PATH = "image-path";
    public static final String IMAGE_PATH_DEST = "image-path-destination";
    public static final String SCALE = "scale";
    public static final String ORIENTATION_IN_DEGREES = "orientation_in_degrees";
    public static final String ASPECT_X = "aspectX";
    public static final String ASPECT_Y = "aspectY";
    public static final String OUTPUT_X = "outputX";
    public static final String OUTPUT_Y = "outputY";
    public static final String SCALE_UP_IF_NEEDED = "scaleUpIfNeeded";
    public static final String CIRCLE_CROP = "circleCrop";
    public static final String RETURN_DATA = "return-data";
    public static final String RETURN_DATA_AS_BITMAP = "data";
    public static final String ACTION_INLINE_DATA = "inline-data";

    private final Bitmap.CompressFormat mOutputFormat = Bitmap.CompressFormat.JPEG;
    private boolean mCircleCrop = false;
    private final Handler mHandler = new Handler();

    private float mAspectX;
    private float mAspectY;
    private int mOutputX;
    private int mOutputY;
    private boolean mScale;
    private CropImageView mImageView;
    private ContentResolver mContentResolver;
    private Bitmap mBitmap;
    private Uri mImagePath;
    private Uri mImagePathDest;

    boolean mWaitingToPick;
    boolean mSaving;
    HighlightView mCrop;

    private boolean mScaleUp = true;
    int rotationInDegrees = 0;

    private final BitmapManager.ThreadSet mDecodingThreads =
            new BitmapManager.ThreadSet();

    private static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

    @Override
    public void onCreate(Bundle icicle) {

        super.onCreate(icicle);
        mContentResolver = getContentResolver();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.cropimage);

        mImageView = findViewById(R.id.image);

        showStorageToast(this);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {

            if (extras.getString(CIRCLE_CROP) != null) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
                    mImageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                }

                mCircleCrop = true;
                mAspectX = 1;
                mAspectY = 1;
            }

            mImagePath = extras.getParcelable(IMAGE_PATH);
            mImagePathDest = extras.getParcelable(IMAGE_PATH_DEST);
            mBitmap = getBitmap(mImagePath);

            try {
                ExifInterface exif = new ExifInterface(mImagePath.getPath());
                int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                rotationInDegrees = exifToDegrees(rotation);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (extras.containsKey(ASPECT_X) && extras.get(ASPECT_X) instanceof Float) {
                mAspectX = extras.getFloat(ASPECT_X);
            } else {
                throw new IllegalArgumentException("aspect_x must be integer");
            }
            if (extras.containsKey(ASPECT_Y) && extras.get(ASPECT_Y) instanceof Float) {
                mAspectY = extras.getFloat(ASPECT_Y);
            } else {

                throw new IllegalArgumentException("aspect_y must be integer");
            }
            mOutputX = extras.getInt(OUTPUT_X);
            mOutputY = extras.getInt(OUTPUT_Y);
            mScale = extras.getBoolean(SCALE, true);
            mScaleUp = extras.getBoolean(SCALE_UP_IF_NEEDED, true);
        }


        if (mBitmap == null) {

            Log.d(TAG, "finish!!!");
            finish();
            return;
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        findViewById(R.id.btn_cancel).setOnClickListener(
                v -> {
                    setResult(RESULT_CANCELED);
                    finish();
                });

        findViewById(R.id.btn_done).setOnClickListener(
                v -> {
                    try {
                        onSaveClicked();
                    } catch (Exception e) {
                        finish();
                    }
                });
        findViewById(R.id.rotateLeft).setOnClickListener(
                v -> {
                    mBitmap = Util.rotateImage(mBitmap, -90);
                    RotateBitmap rotateBitmap = new RotateBitmap(mBitmap);
                    mImageView.setImageRotateBitmapResetBase(rotateBitmap, true);
                    mRunFaceDetection.run();
                });

        findViewById(R.id.rotateRight).setOnClickListener(
                v -> {
                    mBitmap = Util.rotateImage(mBitmap, 90);
                    RotateBitmap rotateBitmap = new RotateBitmap(mBitmap);
                    mImageView.setImageRotateBitmapResetBase(rotateBitmap, true);
                    mRunFaceDetection.run();
                });

        if (rotationInDegrees == 90) {
            mBitmap = Util.rotateImage(mBitmap, 90);
            RotateBitmap rotateBitmap = new RotateBitmap(mBitmap);
            mImageView.setImageRotateBitmapResetBase(rotateBitmap, true);
            mRunFaceDetection.run();
//            90+90;
        } else if (rotationInDegrees == 180) {
            mBitmap = Util.rotateImage(mBitmap, 180);
            RotateBitmap rotateBitmap = new RotateBitmap(mBitmap);
            mImageView.setImageRotateBitmapResetBase(rotateBitmap, true);
            mRunFaceDetection.run();
//            180+180;

        } else if (rotationInDegrees == 270) {
            mBitmap = Util.rotateImage(mBitmap, 270);
            RotateBitmap rotateBitmap = new RotateBitmap(mBitmap);
            mImageView.setImageRotateBitmapResetBase(rotateBitmap, true);
            mRunFaceDetection.run();
//            270-90;
        } else {
            startFaceDetection();
        }
    }

    private Uri getImageUri(String path) {
        return Uri.fromFile(new File(path));
    }

    private Bitmap getBitmap(Uri uri) {

        InputStream in;
        try {
            in = mContentResolver.openInputStream(uri);

            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;

            BitmapFactory.decodeStream(in, null, o);
            in.close();

            int scale = 1;
            if (o.outHeight > IMAGE_MAX_SIZE || o.outWidth > IMAGE_MAX_SIZE) {
                scale = (int) Math.pow(2, (int) Math.round(Math.log(IMAGE_MAX_SIZE / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
            }
            System.gc();
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            in = mContentResolver.openInputStream(uri);
            Bitmap b = BitmapFactory.decodeStream(in, null, o2);
            in.close();

            return b;
        } catch (IOException e) {
            Log.e(TAG, "file " + uri.getPath() + " not found");
        }
        return null;
    }

    private void startFaceDetection() {

        if (isFinishing()) {
            return;
        }

        mImageView.setImageBitmapResetBase(mBitmap, true);

        Util.startBackgroundJob(this, null,
                "Please wait\u2026",
                () -> {
                    final CountDownLatch latch = new CountDownLatch(1);
                    final Bitmap b = mBitmap;
                    mHandler.post(() -> {

                        if (b != mBitmap && b != null) {
                            mImageView.setImageBitmapResetBase(b, true);
                            mBitmap.recycle();
                            mBitmap = b;
                        }
                        if (mImageView.getScale() == 1F) {
                            mImageView.center(true, true);
                        }
                        latch.countDown();
                    });
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    mRunFaceDetection.run();
                }, mHandler);
    }

    private void onSaveClicked() {
        if (mSaving) return;

        if (mCrop == null) {
            return;
        }

        mSaving = true;

        Rect r = mCrop.getCropRect();

        int width = r.width();
        int height = r.height();

        Bitmap croppedImage;

        croppedImage = Bitmap.createBitmap(width, height, mCircleCrop ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        if (croppedImage == null) {
            return;
        }

        {
            Canvas canvas = new Canvas(croppedImage);
            Rect dstRect = new Rect(0, 0, width, height);
            canvas.drawBitmap(mBitmap, r, dstRect, null);
        }

        if (mCircleCrop) {
            Canvas c = new Canvas(croppedImage);
            Path p = new Path();
            p.addCircle(width / 2F, height / 2F, width / 2F,
                    Path.Direction.CW);
            c.clipPath(p, Region.Op.DIFFERENCE);
            c.drawColor(0x00000000, PorterDuff.Mode.CLEAR);
        }

        if (mOutputX != 0 && mOutputY != 0) {

            if (mScale) {

                Bitmap old = croppedImage;
                croppedImage = Util.transform(new Matrix(),
                        croppedImage, mOutputX, mOutputY, mScaleUp);
                if (old != croppedImage) {

                    old.recycle();
                }
            } else {

                Bitmap b = Bitmap.createBitmap(mOutputX, mOutputY,
                        Bitmap.Config.RGB_565);
                Canvas canvas = new Canvas(b);

                Rect srcRect = mCrop.getCropRect();
                Rect dstRect = new Rect(0, 0, mOutputX, mOutputY);

                int dx = (srcRect.width() - dstRect.width()) / 2;
                int dy = (srcRect.height() - dstRect.height()) / 2;

                /* If the srcRect is too big, use the center part of it. */
                srcRect.inset(Math.max(0, dx), Math.max(0, dy));

                /* If the dstRect is too big, use the center part of it. */
                dstRect.inset(Math.max(0, -dx), Math.max(0, -dy));

                /* Draw the cropped bitmap in the center */
                canvas.drawBitmap(mBitmap, srcRect, dstRect, null);

                /* Set the cropped bitmap as the new bitmap */
                croppedImage.recycle();
                croppedImage = b;
            }
        }

        // Return the cropped image directly or save it to the specified URI.
        Bundle myExtras = getIntent().getExtras();
        if (myExtras != null && (myExtras.getParcelable("data") != null
                || myExtras.getBoolean(RETURN_DATA))) {

            Bundle extras = new Bundle();
            extras.putParcelable(RETURN_DATA_AS_BITMAP, croppedImage);
            setResult(RESULT_OK,
                    (new Intent()).setAction(ACTION_INLINE_DATA).putExtras(extras));
            finish();
        } else {
            final Bitmap b = croppedImage;
            Util.startBackgroundJob(this, null, getString(R.string.saving_image),
                    () -> saveOutput(b), mHandler);
        }
    }

    private void saveOutput(Bitmap croppedImage) {

        if (mImagePathDest != null) {
            OutputStream outputStream = null;

            try {
                outputStream = mContentResolver.openOutputStream(mImagePathDest);
                if (outputStream != null) {
                    croppedImage.compress(mOutputFormat, 90, outputStream);
                }
            } catch (IOException ex) {
                Log.e(TAG, "Cannot open file: " + mImagePathDest, ex);
                setResult(RESULT_CANCELED);
                finish();
                return;
            } finally {

                Util.closeSilently(outputStream);
            }

            Bundle extras = new Bundle();
            Intent intent = new Intent(mImagePathDest.toString());
            intent.putExtras(extras);
            intent.putExtra(IMAGE_PATH, mImagePath);
            intent.putExtra(IMAGE_PATH_DEST, mImagePathDest);
            intent.putExtra(ORIENTATION_IN_DEGREES, Util.getOrientationInDegree(this));
            setResult(RESULT_OK, intent);
        } else {
            Log.e(TAG, "not defined image url");
        }
        croppedImage.recycle();
        finish();
    }

    @Override
    protected void onPause() {

        super.onPause();
        BitmapManager.instance().cancelThreadDecoding(mDecodingThreads);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBitmap != null) {
            mBitmap.recycle();
        }
    }


    Runnable mRunFaceDetection = new Runnable() {
        float mScale = 1F;
        Matrix mImageMatrix;
        final FaceDetector.Face[] mFaces = new FaceDetector.Face[3];
        int mNumFaces;

        private void handleFace(FaceDetector.Face f) {

            PointF midPoint = new PointF();

            int r = ((int) (f.eyesDistance() * mScale)) * 2;
            f.getMidPoint(midPoint);
            midPoint.x *= mScale;
            midPoint.y *= mScale;

            int midX = (int) midPoint.x;
            int midY = (int) midPoint.y;

            HighlightView hv = new HighlightView(mImageView);

            int width = mBitmap.getWidth();
            int height = mBitmap.getHeight();

            Rect imageRect = new Rect(0, 0, width, height);

            RectF faceRect = new RectF(midX, midY, midX, midY);
            faceRect.inset(-r, -r);
            if (faceRect.left < 0) {
                faceRect.inset(-faceRect.left, -faceRect.left);
            }

            if (faceRect.top < 0) {
                faceRect.inset(-faceRect.top, -faceRect.top);
            }

            if (faceRect.right > imageRect.right) {
                faceRect.inset(faceRect.right - imageRect.right,
                        faceRect.right - imageRect.right);
            }

            if (faceRect.bottom > imageRect.bottom) {
                faceRect.inset(faceRect.bottom - imageRect.bottom,
                        faceRect.bottom - imageRect.bottom);
            }

            hv.setup(mImageMatrix, imageRect, faceRect, mCircleCrop,
                    mAspectX != 0 && mAspectY != 0);

            mImageView.add(hv);
        }

        private void makeDefault() {
            HighlightView hv = new HighlightView(mImageView);

            int width = mBitmap.getWidth();
            int height = mBitmap.getHeight();

            Rect imageRect = new Rect(0, 0, width, height);

            // make the default size about 4/5 of the width or height
            float cropWidth = Math.min(width, height) * 4f / 5f;
            float cropHeight = cropWidth;

            if (mAspectX != 0 && mAspectY != 0) {
                if (mAspectX > mAspectY) {
                    cropHeight = cropWidth * mAspectY / mAspectX;
                } else {
                    cropWidth = cropHeight * mAspectX / mAspectY;
                }
            }

            float x = (width - cropWidth) / 2;
            float y = (height - cropHeight) / 2;

            RectF cropRect = new RectF(x, y, x + cropWidth, y + cropHeight);
            hv.setup(mImageMatrix, imageRect, cropRect, mCircleCrop,
                    mAspectX != 0 && mAspectY != 0);

            mImageView.mHighlightViews.clear();

            mImageView.add(hv);
        }

        private Bitmap prepareBitmap() {

            if (mBitmap == null) {
                return null;
            }

            if (mBitmap.getWidth() > 256) {
                mScale = 256.0F / mBitmap.getWidth();
            }
            Matrix matrix = new Matrix();
            matrix.setScale(mScale, mScale);
            return Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
        }

        public void run() {
            mImageMatrix = mImageView.getImageMatrix();
            Bitmap faceBitmap = prepareBitmap();
            mScale = 1.0F / mScale;
            if (faceBitmap != null) {
                FaceDetector detector = new FaceDetector(faceBitmap.getWidth(),
                        faceBitmap.getHeight(), mFaces.length);
                mNumFaces = detector.findFaces(faceBitmap, mFaces);
            }

            if (faceBitmap != null && faceBitmap != mBitmap) {
                faceBitmap.recycle();
            }

            mHandler.post(() -> {
                mWaitingToPick = mNumFaces > 1;
                if (mNumFaces > 0) {
                    for (int i = 0; i < mNumFaces; i++) {
                        handleFace(mFaces[i]);
                    }
                } else {
                    makeDefault();
                }
                mImageView.invalidate();
                if (mImageView.mHighlightViews.size() == 1) {
                    mCrop = mImageView.mHighlightViews.get(0);
                    mCrop.setFocus(true);
                }

                if (mNumFaces > 1) {
                    Toast.makeText(CropImage.this,
                            "Multi face crop help",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    public static final int NO_STORAGE_ERROR = -1;
    public static final int CANNOT_STAT_ERROR = -2;

    public static void showStorageToast(Activity activity) {
        showStorageToast(activity, calculatePicturesRemaining(activity));
    }

    public static void showStorageToast(Activity activity, int remaining) {
        String noStorageText = null;
        if (remaining == NO_STORAGE_ERROR) {
            String state = Environment.getExternalStorageState();
            if (state.equals(Environment.MEDIA_CHECKING)) {
                noStorageText = activity.getString(R.string.preparing_card);
            } else {
                noStorageText = activity.getString(R.string.no_storage_card);
            }
        } else if (remaining < 1) {
            noStorageText = activity.getString(R.string.not_enough_space);
        }
        if (noStorageText != null) {
            Toast.makeText(activity, noStorageText, Toast.LENGTH_LONG).show();
        }
    }

    public static int calculatePicturesRemaining(Activity activity) {
        try {
            String storageDirectory;
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                storageDirectory = Environment.getExternalStorageDirectory().toString();
            } else {
                storageDirectory = activity.getFilesDir().toString();
            }
            StatFs stat = new StatFs(storageDirectory);
            float remaining = ((float) stat.getAvailableBlocks()
                    * (float) stat.getBlockSize()) / 400000F;
            return (int) remaining;
        } catch (Exception ex) {
            return CANNOT_STAT_ERROR;
        }
    }
}

