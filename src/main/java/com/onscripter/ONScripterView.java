package com.onscripter;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.onscripter.exception.NativeONSException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * This class is a wrapper to render ONScripter games inside a single view object
 * without any extra code. All you need to do is create the object by the
 * constructor and add it to your layout. Then you can set a ONScripterEventListener
 * if you want to. Finally it is your job to set the size of this view.
 *
 * You must also pass the following events from your activity for this ONScripterView
 * to act normally: <b>onPause and onResume</b> and should not call exitApp() when <i>onDestroy</i>
 * occurs because it will freeze, please exit the app before onStop() or finish() when
 * onGameFinished() occurs.
 * @author Matthew Ng
 *
 */
public class ONScripterView extends DemoGLSurfaceView {
    private static final String TAG = "ONScripterView";

    private static final int MSG_AUTO_MODE = 1;
    private static final int MSG_SKIP_MODE = 2;
    private static final int MSG_SINGLE_PAGE_MODE = 3;
    private static final int MSG_ERROR_MESSAGE = 4;

    public interface ONScripterEventListener {
        void autoStateChanged(boolean selected);
        void skipStateChanged(boolean selected);
        void singlePageStateChanged(boolean selected);
        void videoRequested(@NonNull Uri videoUri, boolean clickToSkip, boolean shouldLoop);
        void onNativeError(NativeONSException e, String line, String backtrace);
        void onUserMessage(UserMessage messageId);
        void onGameFinished();
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
                if (msg.what < MSG_ERROR_MESSAGE) {
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

    // Native methods
    private native void nativeSetSentenceFontScale(double scale);
    private native int nativeGetDialogFontSize();

    /**
     * Constructor with parameters
     * @param builder used for view constructor
     */
    public ONScripterView(@NonNull Builder builder) {
        super(builder);

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

    /* Called from ONScripter.h */
    protected void playVideo(char[] filepath, boolean clickToSkip, boolean shouldLoop){
        if (!mHasExit && mListener != null) {
            Uri uri = getUri(filepath);
            if (uri == null) {
                return;
            }
            if (exists(uri)) {
                mIsVideoPlaying = true;
                mListener.videoRequested(uri, clickToSkip, shouldLoop);
                mIsVideoPlaying = false;
            } else {
                Log.e(TAG, "Cannot play video because it either does not exist. File: " + uri);
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
    protected int getFD(char[] filepath, int mode) {
        ParcelFileDescriptor pfd;
        try {
            final ContentResolver resolver = getContext().getContentResolver();
            Uri uri = getUri(filepath);
            if (uri == null) {
                return -1;
            }

            boolean exists = exists(uri);
            if (mode == 0 /* Read mode */) {
                if (!exists) {
                    // File does not exist
                    return -1;
                }
                pfd = resolver.openFileDescriptor(uri, "r");
            } else {    // Write Mode
                if (!exists) {
                    // File does not exist
                    if (!createFile(uri)) {
                        Log.e(TAG, "Unable to create file " + uri);
                        return -1;
                    }
                }
                pfd = resolver.openFileDescriptor(uri, "rw");
            }

            if (pfd == null) {
                Log.v(TAG, "Cannot find pfd");
                return -1;
            }
            return pfd.detachFd();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /* Called from ONScripter.h */
    protected long getStat(char[] filepath) {
        final ContentResolver resolver = getContext().getContentResolver();
        Uri uri = getUri(filepath);
        if (uri == null) {
            return -1;
        }

        // If file scheme, simple return, usually won't run here because can do stat in C
        boolean isFileScheme = ContentResolver.SCHEME_FILE.equals(uri.getScheme());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT || isFileScheme) {
            if (!isFileScheme) {
                throw new IllegalStateException("Cannot get file because uri is content "+ uri);
            }
            return new File(Objects.requireNonNull(uri.getPath())).lastModified();
        }

        // Use content resolver to get the date last modified and return it
        final String[] proj = new String[] { DocumentsContract.Document.COLUMN_LAST_MODIFIED };
        try (final Cursor c = resolver.query(uri, proj,null, null, null)) {
            if (c != null) {
                if (c.moveToNext()) {
                    return c.getLong(c.getColumnIndex(
                            DocumentsContract.Document.COLUMN_LAST_MODIFIED));
                }
            } else {
                Log.e(TAG, "Failed to resolve self, path: " + uri);
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    /* Called from ONScripter.h */
    protected int mkdir(char[] filepath) {
        Uri uri = getUri(filepath);
        if (uri == null) {
            return -1;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            File file = new File(DocumentsContract.getDocumentId(uri));
            Uri parentUri = DocumentsContract.buildDocumentUriUsingTree(uri, file.getParent());
            try {
                DocumentsContract.createDocument(getContext().getContentResolver(), parentUri,
                        DocumentsContract.Document.MIME_TYPE_DIR, file.getName());
            } catch (FileNotFoundException ignored) {
                return -1;
            }
            return 0;
        } else {
            return new File(Objects.requireNonNull(uri.getPath())).mkdir() ? 0 : -1;
        }
    }

    private boolean createFile(@NonNull Uri uri) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            File file = new File(DocumentsContract.getDocumentId(uri));
            Uri parentUri = DocumentsContract.buildDocumentUriUsingTree(uri, file.getParent());
            DocumentsContract.createDocument(getContext().getContentResolver(), parentUri,
                    "application/octet-stream", file.getName());
            return true;
        } else {
            return new File(Objects.requireNonNull(uri.getPath())).createNewFile();
        }
    }

    @Nullable
    private Uri getUri(@Nullable char[] filenameForC) {
        if (filenameForC == null) {
            return null;
        }
        String path = new String(filenameForC);
        if (path.startsWith(File.separator)) {
            // Absolute path
            return Uri.fromFile(new File(path));
        } else if (path.startsWith(ContentResolver.SCHEME_FILE)
                || path.startsWith(ContentResolver.SCHEME_CONTENT)) {
            // Content string
            return Uri.parse(path);
        }

        // Relative path from game path, build tree Uri above lollipop otherwise file uri
        path = getGamePath() + File.separator + path;
        return getTreeUri() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                ? DocumentsContract.buildDocumentUriUsingTree(getTreeUri(), path)
                : Uri.fromFile(new File(path));
    }

    private boolean exists(@NonNull Uri uri) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT
                || ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            return new File(Objects.requireNonNull(uri.getPath())).exists();
        } else if (getTreeUri() != null) {
            try (final Cursor c = getContext().getContentResolver().query(uri, null,
                    null, null, null)) {
                if (c != null) {
                    if (c.moveToNext()) {
                        return true;
                    }
                } else {
                    Log.e(TAG, "Failed to resolve self, path: " + uri);
                }
            } catch (Exception ignored) {
            }
        }
        return false;
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
                case MSG_SINGLE_PAGE_MODE:
                    mListener.singlePageStateChanged(flag);
                    break;
            }
        }
    }

    private void sendUserMessage(int messageIdFromNDK) {
        if (mListener != null) {
            switch(messageIdFromNDK) {
                case MSG_ERROR_MESSAGE:
                    mListener.onUserMessage(UserMessage.CORRUPT_SAVE_FILE);
                    break;
            }
        }
    }

    public static class Builder {
        @NonNull
        final Context context;
        @NonNull
        final Uri uri;
        @Nullable
        String fontPath;
        @Nullable
        String screenshotPath;
        boolean useHQAudio;
        boolean renderOutline;

        public Builder(@NonNull Context context, @NonNull Uri gameUri) {
            this.context = context;
            uri = gameUri;
        }

        public Builder setFontPath(@NonNull String fontPath) {
            this.fontPath = fontPath;
            return this;
        }

        public Builder setScreenshotPath(@NonNull String screenshotPath) {
            this.screenshotPath = screenshotPath;
            return this;
        }

        public Builder useHQAudio() {
            useHQAudio = true;
            return this;
        }

        public Builder useRenderOutline() {
            renderOutline = true;
            return this;
        }

        public ONScripterView create() {
            return new ONScripterView(this);
        }
    }

    // Load the libraries
    static {
        System.loadLibrary("sdl");
        System.loadLibrary("onscripter");
    }
}
