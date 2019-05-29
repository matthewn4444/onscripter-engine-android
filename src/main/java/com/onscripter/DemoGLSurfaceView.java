package com.onscripter;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Point;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class DemoRenderer extends GLSurfaceView_SDL.Renderer {
    public DemoRenderer(@NonNull ONScripterView.Builder builder) {
        mBuilder = builder;

        // Get the tree uri if supported
        if (ContentResolver.SCHEME_CONTENT.equals(builder.uri.getScheme())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mTreeUri = DocumentsContract.buildDocumentUriUsingTree(builder.uri,
                        DocumentsContract.getTreeDocumentId(builder.uri));
                mGameDirectory = DocumentsContract.getDocumentId(builder.uri);
            } else {
                throw new IllegalStateException("Uri game path is incorrect, content scheme " +
                        "cannot be used for lower than lollipop");
            }
        } else {
            if (builder.uri.getPath() == null) {
                throw new NullPointerException("Cannot have null path for game uri "
                        + builder.uri.toString());
            }
            mTreeUri = null;
            mGameDirectory = builder.uri.getPath();
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Set background to black when nothing is drawn
        gl.glEnable(GL10.GL_DEPTH_TEST);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        final Point size = getScaledDimensions(w, h);
        nativeResize(size.x, size.y);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        nativeInitJavaCallbacks();

        // Calls main() and never returns, hehe - we'll call eglSwapBuffers() from native code
        doNativeInit(false);
    }

    void doNativeInit(boolean openOnly) {
        final List<String> flags = new ArrayList<>();
        flags.add("--language");
        flags.add(Locale.getDefault().getLanguage());
        if (mTreeUri != null) {
            flags.add("--use-java-io");
        }
        if (openOnly) {
            flags.add("--open-only");
        }
        if (mBuilder.renderOutline) {
            flags.add("--render-font-outline");
        }
        if (mBuilder.useHQAudio) {
            flags.add("--audio-hq");
        }
        if (mBuilder.fontPath != null) {
            flags.add("-f");
            flags.add(mBuilder.fontPath);
        }
        if (mBuilder.screenshotPath != null) {
            flags.add("--screenshot-path");
            flags.add(mBuilder.screenshotPath);
        }

        // If uses file scheme send the directory
        nativeInit(mTreeUri != null ? null : mGameDirectory, flags.toArray(new String[0]));
    }

    // Called from native code, returns 1 on success, 0 when GL context lost
    // (user put app to background)
    @Keep
    public int swapBuffers() {
        return super.SwapBuffers() ? 1 : 0;
    }

    public void exitApp() {
        nativeDone();
    };

    Point getScaledDimensions(int containerWidth, int containerHeight) {
        final Point size = new Point(containerWidth, containerHeight);
        int gameWidth = nativeGetWidth();
        int gameHeight = nativeGetHeight();
        if (gameWidth > 0 && gameHeight > 0) {
            float containerRatio = containerWidth * 1f / containerHeight;
            float gameRatio = gameWidth * 1f / gameHeight;

            if (gameRatio > containerRatio) {
                // Use container's width
                size.y = (int) (containerWidth / gameRatio);
            } else {
                // Use container's height
                size.x = (int) (containerHeight * gameRatio);
            }
        }
        return size;
    }

    private native void nativeInitJavaCallbacks();
    private native void nativeInit(String path, String[] arg);
    private native void nativeResize(int w, int h);
    private native void nativeDone();
    native int nativeGetWidth();
    native int nativeGetHeight();

    private final static String TAG = "DemoRenderer";

    @NonNull
    final String mGameDirectory;
    @Nullable
    final Uri mTreeUri;
    @NonNull
    final ONScripterView.Builder mBuilder;
}

class DemoGLSurfaceView extends GLSurfaceView_SDL {
    public DemoGLSurfaceView(@NonNull ONScripterView.Builder builder) {
        super(builder.context);
        nativeInitJavaCallbacks();
        mRenderer = new DemoRenderer(builder);
        setRenderer(mRenderer);
        mExitted = false;
        mRenderer.doNativeInit(true);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        // TODO: add multitouch support (added in Android 2.0 SDK)
        int action = -1;
        if( event.getAction() == MotionEvent.ACTION_DOWN ) {
            action = 0;
        }
        if( event.getAction() == MotionEvent.ACTION_UP ) {
            action = 1;
        }
        if( event.getAction() == MotionEvent.ACTION_MOVE ) {
            action = 2;
        }
        if ( action >= 0 && !mExitted ) {
            nativeMouse( (int)event.getX(), (int)event.getY(), action );
        }

        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final Point size = mExitted ? mLastGameSize : mRenderer.getScaledDimensions(
                MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
        if (size.x > 0 && size.y > 0) {
            mLastGameSize = size;
        }
        setMeasuredDimension(size.x, size.y);
    }

    public void exitApp() {
        mExitted = true;
        mRenderer.exitApp();
    };

    @Override
    public boolean onKeyDown(int keyCode, final KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            Activity activity = (Activity) this.getContext();
            AudioManager audio = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
            int volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                    + (keyCode == KeyEvent.KEYCODE_VOLUME_UP ? 1 : (-1));
            if (volume >= 0 && volume <= audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) {
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);
            }
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            super.onKeyDown(keyCode, event);
            return false;
        }
        triggerKeyEvent(keyCode, 1);
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, final KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_MENU){
            super.onKeyUp(keyCode, event);
            return false;
        }
        triggerKeyEvent(keyCode, 0);
        return true;
    }

    @Override
    public void onPause() {
        triggerKeyEvent(0, 3); // send SDL_ACTIVEEVENT

        // Some games require saving the gloval again when going to overview to save last file
        queueEvent(mSaveGameSettingsRunnable);
        super.onPause();
        surfaceDestroyed(this.getHolder());
    }

    @Override
    public void onResume() {
        super.onResume();
        triggerKeyEvent(0, 3); // send SDL_ACTIVEEVENT
    }

    protected void triggerKeyEvent(int keyCode, int down) {
        if (!mExitted) {
            nativeKey(keyCode, down);
        }
    }

    protected void onFinish() {
        mExitted = true;
    }

    @NonNull
    protected String getGamePath() {
        return mRenderer.mGameDirectory;
    }

    @Nullable
    protected Uri getTreeUri() {
        return mRenderer.mTreeUri;
    }

    private final Runnable mSaveGameSettingsRunnable = new Runnable() {
        @Override
        public void run() {
            nativeSaveGameSettings();
        }
    };

    private Point mLastGameSize;
    private boolean mExitted;

    DemoRenderer mRenderer;

    private native void nativeSaveGameSettings();
    private native int nativeInitJavaCallbacks();
    private native void nativeMouse( int x, int y, int action );
    private native void nativeKey( int keyCode, int down );
}
