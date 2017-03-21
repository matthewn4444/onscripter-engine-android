package com.onscripter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.media.AudioManager;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.Locale;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class DemoRenderer extends GLSurfaceView_SDL.Renderer {
    public DemoRenderer(String currentDirectory, String fontPath, boolean useHQAudio,
                        boolean renderOutline) {
        mCurrentDirectory = currentDirectory;
        mFontPath = fontPath;
        mShouldRenderOutline = renderOutline;
        mUseHQAudio = useHQAudio;
        doNativeInit(true);
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

    private void doNativeInit(boolean openOnly) {
        int n = 2;
        if (openOnly) {
            n++;
        }
        if (mShouldRenderOutline) {
            n++;
        }
        if (mUseHQAudio) {
            n++;
        }
        if (mFontPath != null) {
            n += 2;
        }
        String[] arg = new String[n];
        n = 0;
        arg[n++] = "--language";
        arg[n++] = Locale.getDefault().getLanguage();
        if (openOnly) {
            arg[n++] = "--open-only";
        }
        if (mShouldRenderOutline) {
            arg[n++] = "--render-font-outline";
        }
        if (mUseHQAudio) {
            arg[n++] = "--audio-hq";
        }
        if (mFontPath != null) {
            arg[n++] = "-f";
            arg[n++] = mFontPath;
        }
        nativeInit(mCurrentDirectory, arg);

    }

    // Called from native code, returns 1 on success, 0 when GL context lost
    // (user put app to background)
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
    private native void nativeInit(String currentDirectoryPath, String[] arg);
    private native void nativeResize(int w, int h);
    private native void nativeDone();
    native int nativeGetWidth();
    native int nativeGetHeight();

    private final String mCurrentDirectory;
    private final String mFontPath;
    private final boolean mUseHQAudio;
    private final boolean mShouldRenderOutline;
}

class DemoGLSurfaceView extends GLSurfaceView_SDL {
    public DemoGLSurfaceView(Context context, String currentDirectory, String fontPath,
                             boolean useHQAudio, boolean shouldRenderOutline) {
        super(context);
        nativeInitJavaCallbacks();
        mRenderer = new DemoRenderer(currentDirectory, fontPath, useHQAudio,
                shouldRenderOutline);
        setRenderer(mRenderer);
        mExitted = false;
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
        if ( action >= 0 ) {
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
        nativeKey(keyCode, 1);
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, final KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_MENU){
            super.onKeyUp(keyCode, event);
            return false;
        }
        nativeKey(keyCode, 0);
        return true;
    }

    @Override
    public void onPause() {
        nativeKey(0, 3); // send SDL_ACTIVEEVENT
        super.onPause();
        surfaceDestroyed(this.getHolder());
    }

    @Override
    public void onResume() {
        super.onResume();
        nativeKey(0, 3); // send SDL_ACTIVEEVENT
    }

    protected void onFinish() {
        mExitted = true;
    }

    private Point mLastGameSize;
    private boolean mExitted;

    DemoRenderer mRenderer;

    private native int nativeInitJavaCallbacks();
    protected native void nativeMouse( int x, int y, int action );
    protected native void nativeKey( int keyCode, int down );
}
