package com.onscripter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.onscripter.exception.NativeONSException;

import java.io.File;
import java.lang.ref.WeakReference;


/**
 * This class is a wrapper to render ONScripter games inside a single view object
 * without any extra code. All you need to do is create the object by the
 * constructor and add it to your layout. Then you can set a ONScripterEventListener
 * if you want to. Finally it is your job to set the size of this view.
 *
 * You must also pass the following events from your activity for this ONScripterView
 * to act normally: <b>onPause, onResume, and onUserLeaveHint</b> and also on the
 * <i>onDestroy</i> event you should call <b>exitApp()</b>. Fail to do any of these
 * will cause the game to crash.
 * @author Matthew Ng
 *
 */
public class ONScripterView extends DemoGLSurfaceView {
    private static final String TAG = "ONScripterView";

    private static final int MSG_AUTO_MODE = 1;
    private static final int MSG_SKIP_MODE = 2;
    private static final int NUM_CONTROL_MODES = 2;

    public interface ONScripterEventListener {
        public void autoStateChanged(boolean selected);
        public void skipStateChanged(boolean selected);
        public void videoRequested(String filename, boolean clickToSkip, boolean shouldLoop);
        public void onNativeError(NativeONSException e, String line, String backtrace);
        public void onUserMessage(UserMessage messageId);
        public void onGameFinished();
    }

    private static class UpdateHandler extends Handler {
        private final WeakReference<ONScripterView> mThisView;
        UpdateHandler(ONScripterView activity) {
            mThisView = new WeakReference<ONScripterView>(activity);
        }
        @Override
        public void handleMessage(Message msg)
        {
            ONScripterView view = mThisView.get();
            if (view != null) {
                if (msg.what <= NUM_CONTROL_MODES) {
                    view.updateControls(msg.what, (Boolean)msg.obj);
                } else {
                    view.sendUserMessage(msg.what);
                }
            }
        }
    }

    /* Called from ONScripter.h */
    private static void receiveMessageFromNDK(int mode, boolean flag) {
        if (sHandler != null) {
            Message msg = new Message();
            msg.obj = flag;
            msg.what = mode;
            sHandler.sendMessage(msg);
        }
    }

    public enum UserMessage {
        CORRUPT_SAVE_FILE
    };

    private final AudioThread mAudioThread;
    private final String mCurrentDirectory;
    private final Handler mMainHandler;

    // Native methods
    private native void nativeSetSentenceFontScale(double scale);
    private native int nativeGetDialogFontSize();

    /**
     * Default constructor
     * @param context used for view constructor
     * @param gameDirectory is the location of the game
     */
    public ONScripterView(Context context, String gameDirectory) {
        this(context, gameDirectory, null);
    }

    /**
     * Constructor with font path
     * @param context used for view constructor
     * @param gameDirectory is the location of the game
     * @param fontPath is the location of the font
     */
    public ONScripterView(Context context, String gameDirectory, String fontPath) {
        this(context, gameDirectory, fontPath, true, false);
    }

    /**
     * Full constructor with the outline code
     * @param context used for view constructor
     * @param gameDirectory is the location of the game
     * @param fontPath is the location of the font
     * @param useHQAudio should use higher quality audio, default is true
     * @param shouldRenderOutline chooses whether to show outline on font
     */
    public ONScripterView(Context context, String gameDirectory, String fontPath,
                          boolean useHQAudio, boolean shouldRenderOutline) {
        super(context, gameDirectory, fontPath, useHQAudio, shouldRenderOutline);

        mCurrentDirectory = gameDirectory;
        mAudioThread = new AudioThread();
        mMainHandler = new Handler(Looper.getMainLooper());

        sHandler = new UpdateHandler(this);
        setFocusableInTouchMode(true);
        setFocusable(true);
        requestFocus();
    }

    /** Receive State Updates from Native Code */
    private static UpdateHandler sHandler;

    private ONScripterEventListener mListener;
    boolean mIsVideoPlaying = false;
    boolean mHasUserLeaveHint = false;
    boolean mHasExit = false;

    @Override
    public void exitApp() {
        mHasExit = true;
        super.exitApp();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!mHasExit) {
            mAudioThread.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mAudioThread.onResume();
        mHasUserLeaveHint = false;
    }

    public void onStop() {
        if (mHasUserLeaveHint) {
            super.onUserLeaveHint();
        }
    }

    /**
     * Set the listener for game events
     * @param listener listener object
     */
    public void setONScripterEventListener(ONScripterEventListener listener) {
        mListener = listener;
    }

    /**
     * Send native key press to the app
     * @param keyCode the key to simulate into the game
     */
    public void sendNativeKeyPress(int keyCode) {
        nativeKey(keyCode, 1);
        nativeKey(keyCode, 0);
    }

    /**
     * Get the font size of the text currently showing
     * @return font size (pixels)
     */
    public int getGameFontSize() {
        return nativeGetDialogFontSize();
    }

    /**
     * Get the render width of the game. This value is not the size of this view and is set in the
     * script
     * @return width of the game
     */
    public int getGameWidth() {
        return mRenderer.nativeGetWidth();
    }

    /**
     * Get the render height of the game. This value is not the size of this view and is set in the
     * script
     * @return height of the game
     */
    public int getGameHeight() {
        return mRenderer.nativeGetHeight();
    }

    /**
     * Set the font scaling where 1.0 is default 100% size
     * @param scaleFactor scale factor
     */
    public void setFontScaling(double scaleFactor) {
        nativeSetSentenceFontScale(scaleFactor);
    }

    @Override
    public void onUserLeaveHint() {
        mHasUserLeaveHint = true;
        if (!mIsVideoPlaying) {
            super.onUserLeaveHint();
        }
    }

    /* Called from ONScripter.h */
    protected void playVideo(char[] filename, boolean clickToSkip, boolean shouldLoop){
        if (mListener != null) {
            File video = new File(mCurrentDirectory + "/" + new String(filename).replace("\\", "/"));
            if (video.exists() && video.canRead()) {
                mIsVideoPlaying = true;
                mListener.videoRequested(video.getAbsolutePath(), clickToSkip, shouldLoop);
                mIsVideoPlaying = false;
            } else {
                Log.e(TAG, "Cannot play video because it either does not exist or cannot be read." +
                        " File: " + video.getPath());
            }
        }
    }

    /* Called from ONScripter.h */
    protected void receiveException(final String message, final String currentLineBuffer,
                                    final String backtrace) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (currentLineBuffer != null) {
                    Log.e(TAG, message + "\nCurrent line: " + currentLineBuffer + "\n" + backtrace);
                } else {
                    Log.e(TAG, message + "\n" + backtrace);
                }
                if (mListener != null) {
                    NativeONSException exception = new NativeONSException(message);
                    mListener.onNativeError(exception, currentLineBuffer, backtrace);
                }

            }
        });
    }

    /* Called from ONScripter.h */
    protected void onLoadFile(String filename, String savePath) {
    }

    /* Called from ONScripter.h */
    @Override
    protected void onFinish() {
        super.onFinish();
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onGameFinished();
                }
            }
        });
    }

    private void updateControls(int mode, boolean flag) {
        if (mListener != null) {
            switch(mode) {
                case MSG_AUTO_MODE:
                    mListener.autoStateChanged(flag);
                    break;
                case MSG_SKIP_MODE:
                    mListener.skipStateChanged(flag);
                    break;
            }
        }
    }

    private void sendUserMessage(int messageIdFromNDK) {
        if (mListener != null) {
            switch(messageIdFromNDK) {
                case 3:
                    mListener.onUserMessage(UserMessage.CORRUPT_SAVE_FILE);
                    break;
            }
        }
    }

    // Load the libraries
    static {
        System.loadLibrary("sdl");
        System.loadLibrary("application");
        System.loadLibrary("sdl_main");
    }
}
