package com.onscripter;

import android.content.Context;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.support.annotation.RequiresApi;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.onscripter.exception.NativeONSException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
    private static final boolean SDK_L_UP = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

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
            mThisView = new WeakReference<>(activity);
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
    private final Handler mMainHandler;
    private final DocumentFile mExtGameRootFolder;

    // Native methods
    private native void nativeSetSentenceFontScale(double scale);
    private native int nativeGetDialogFontSize();

    /**
     * Default constructor
     * @param context used for view constructor
     * @param gameDirectory is the location of the game
     * @throws FileNotFoundException gameDirectory does not exist
     * @throws IOException kitkat+, cannot canonicalize game paths
     */
    public ONScripterView(Context context, String gameDirectory) throws IOException {
        this(context, gameDirectory, null);
    }

    /**
     * Constructor with font path
     * @param context used for view constructor
     * @param gameDirectory is the location of the game
     * @param fontPath is the location of the font
     * @throws FileNotFoundException gameDirectory does not exist
     * @throws IOException kitkat+, cannot canonicalize game paths
     */
    public ONScripterView(Context context, String gameDirectory, String fontPath)
            throws IOException {
        this(context, gameDirectory, fontPath, true, false);
    }

    /**
     * Full constructor with the outline code
     * @param context used for view constructor
     * @param gameDirectory is the location of the game
     * @param fontPath is the location of the font
     * @param useHQAudio should use higher quality audio, default is true
     * @param shouldRenderOutline chooses whether to show outline on font
     * @throws FileNotFoundException gameDirectory does not exist
     * @throws IOException kitkat+, cannot canonicalize game paths
     */
    public ONScripterView(Context context, String gameDirectory, String fontPath,
                          boolean useHQAudio, boolean shouldRenderOutline)
            throws IOException {
        super(context, gameDirectory, fontPath, useHQAudio, shouldRenderOutline);

        // Check the input paths exists and get the document file for current folder above lollipop
        final File gameFolder = new File(gameDirectory);
        if (!gameFolder.exists()) {
            throw new FileNotFoundException("Cannot find path to start game " + gameDirectory);
        }
        mExtGameRootFolder = SDK_L_UP ? getExternalDocFileFromPath(gameFolder) : null;

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
        triggerKeyEvent(keyCode, 1);
        triggerKeyEvent(keyCode, 0);
    }

    /**
     * Get the font size of the text currently showing
     * @return font size (pixels)
     */
    public int getGameFontSize() {
        return !mHasExit ? nativeGetDialogFontSize() : 0;
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
        if (!mHasExit) {
            nativeSetSentenceFontScale(scaleFactor);
        }
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
            File video = new File(getCurrentDirectory() + "/" + new String(filename)
                    .replace("\\", "/"));
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

    /* Called from ONScripter.h */
    protected int getFD(char[] filename, int mode) {
        final String path = new String(filename);
        ParcelFileDescriptor pfd;
        try {
            if (mode == 0) {
                String p = getCurrentDirectory() + "/" + path;
                p = p.replace('\\', '/');
                pfd = ParcelFileDescriptor.open(new File(p), ParcelFileDescriptor.MODE_READ_ONLY);
            } else if (SDK_L_UP && mExtGameRootFolder != null) {
                final String[] parts = path.split(File.separator);
                DocumentFile file = mExtGameRootFolder;

                // Build relative path for its folder path
                for (int i = 0; i < parts.length - 1; i++) {
                    String part = parts[i];
                    if (!part.isEmpty()) {
                        DocumentFile next = file.findFile(part);
                        file = next != null ? next : file.createDirectory(part);
                    }
                }

                // Handle last file to delete if exists and allowed to be deleted
                final String name = parts[parts.length - 1];
                DocumentFile df2 = file.findFile(name);
                if (df2 != null && df2.exists() && !df2.delete()) {
                    Log.e(TAG, "Cannot delete file to create new file");
                    return -1;
                }
                file = file.createFile("application/octet-stream", name);
                pfd = getContext().getContentResolver().openFileDescriptor(file.getUri(), "w");
            } else {
                // Game running from internal memory or below lollipop
                // Note that typically this will not run, internal memory or lollipop would receive
                // the file descriptor in native code.
                String filename2 = getCurrentDirectory() + "/" + path;
                filename2 = filename2.replace('\\', '/');
                File file = new File(filename2);
                if (!file.exists()) {
                    File parent = file.getParentFile();
                    if (!parent.exists()) {
                        if (!parent.mkdirs()) {
                            Log.w(TAG, "Unable to create folder path");
                            return -1;
                        }
                    }
                    if (file.createNewFile()) {
                        Log.w(TAG, "Unable to create file");
                        return -1;
                    }
                }
                pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_WRITE_ONLY);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        if (pfd == null) {
            Log.v(TAG, "Cannot find pfd");
            return -1;
        }
        return pfd.detachFd();
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

    private String contentUriToPath(Uri uri) {
        String[] parts = uri.getLastPathSegment().split(":");
        return "/storage/" + parts[0] + "/" + (parts.length > 1 ? parts[1] : "");
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private DocumentFile getExternalDocFileFromPath(File file) throws IOException {
        final String canonicalPath = file.getCanonicalPath();
        final String internRoot = Environment.getExternalStorageDirectory().getCanonicalPath();
        if (canonicalPath.startsWith(internRoot)) {
            // Using internal filesystem
            return null;
        }
        final Context c = getContext();
        for (UriPermission p : c.getContentResolver().getPersistedUriPermissions()) {
            final String permPath = contentUriToPath(p.getUri());
            if (canonicalPath.startsWith(permPath)) {
                final String relativePath = canonicalPath.substring(permPath.length());

                // Build the DocumentFile path from relative position
                DocumentFile df = DocumentFile.fromTreeUri(c, p.getUri());
                String[] parts = relativePath.split(File.separator);
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        DocumentFile next = df.findFile(part);
                        if (next == null) {
                            throw new FileNotFoundException(
                                    "Cannot find file from relative path " + relativePath);
                        }
                        df = next;
                    }
                }
                if (df.canRead() && df.canWrite()) {
                    return df;
                }
                break;
            }
        }
        Log.e(TAG, "No permissions to read or write file path");
        throw new IOException("No permissions to read or write file path");
    }

    // Load the libraries
    static {
        System.loadLibrary("sdl");
        System.loadLibrary("application");
        System.loadLibrary("sdl_main");
    }
}
