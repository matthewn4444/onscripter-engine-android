/* -*- C++ -*-
 * 
 *  onscripter_main.cpp -- main function of ONScripter
 *
 *  Copyright (c) 2001-2016 Ogapee. All rights reserved.
 *
 *  ogapee@aqua.dti2.ne.jp
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

#include "ONScripter.h"
#include "version.h"

ONScripter* ons = NULL;

#if defined(IOS)
#import <Foundation/NSArray.h>
#import <UIKit/UIKit.h>
#import "DataCopier.h"
#import "DataDownloader.h"
#import "ScriptSelector.h"
#import "MoviePlayer.h"
#endif

#if defined(PSP)
#include <pspkernel.h>
#include <psputility.h>
#include <psppower.h>
#include <ctype.h>

PSP_HEAP_SIZE_KB(-1);

int psp_power_resume_number = 0;

int exit_callback(int arg1, int arg2, void *common)
{
    if (ons) {
        ons.endCommand();
        sceKernelExitGame();
    }
    return 0;
}

int power_callback(int unknown, int pwrflags, void *common)
{
    if (pwrflags & PSP_POWER_CB_RESUMING) psp_power_resume_number++;
    return 0;
}

int CallbackThread(SceSize args, void *argp)
{
    int cbid;
    cbid = sceKernelCreateCallback("Exit Callback", exit_callback, NULL);
    sceKernelRegisterExitCallback(cbid);
    cbid = sceKernelCreateCallback("Power Callback", power_callback, NULL);
    scePowerRegisterCallback(0, cbid);
    sceKernelSleepThreadCB();
    return 0;
}

int SetupCallbacks(void)
{
    int thid = sceKernelCreateThread("update_thread", CallbackThread, 0x11, 0xFA0, 0, 0);
    if (thid >= 0) sceKernelStartThread(thid, 0, 0);
    return thid;
}
#endif

void optionHelp()
{
    printf( "Usage: onscripter [option ...]\n" );
    printf( "      --cdaudio\t\tuse CD audio if available\n");
    printf( "      --cdnumber no\tchoose the CD-ROM drive number\n");
    printf( "  -f, --font file\tset a TTF font file\n");
    printf( "      --registry file\tset a registry file\n");
    printf( "      --dll file\tset a dll file\n");
    printf( "  -r, --root path\tset the root path to the archives\n");
    printf( "      --fullscreen\tstart in fullscreen mode\n");
    printf( "      --window\t\tstart in windowed mode\n");
    printf( "      --force-button-shortcut\tignore useescspc and getenter command\n");
    printf( "      --enable-wheeldown-advance\tadvance the text on mouse wheel down\n");
    printf( "      --disable-rescale\tdo not rescale the images in the archives\n");
    printf( "      --render-font-outline\trender the outline of a text instead of casting a shadow\n");
    printf( "      --edit\t\tenable online modification of the volume and variables when 'z' is pressed\n");
    printf( "      --key-exe file\tset a file (*.EXE) that includes a key table\n");
    printf( "  -h, --help\t\tshow this help and exit\n");
    printf( "  -v, --version\t\tshow the version information and exit\n");
    exit(0);
}

void optionVersion()
{
    printf("Written by Ogapee <ogapee@aqua.dti2.ne.jp>\n\n");
    printf("Copyright (c) 2001-2016 Ogapee.\n");
    printf("This is free software; see the source for copying conditions.\n");
    exit(0);
}

#ifdef ANDROID
extern "C"
{
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
    ONScripter::JNI_VM = vm;
    return JNI_VERSION_1_2;
};

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved)
{
    ONScripter::JNI_VM = vm;
};

JNIEXPORT jint JNICALL
JAVA_EXPORT_NAME(DemoRenderer_nativeGetWidth) ( JNIEnv* env, jobject thiz )
{
	return ons ? ons->getWidth() : 0;
}

JNIEXPORT jint JNICALL
JAVA_EXPORT_NAME(DemoRenderer_nativeGetHeight) ( JNIEnv* env, jobject thiz )
{
	return ons ? ons->getHeight() : 0;
}

JNIEXPORT void JNICALL
JAVA_EXPORT_NAME(ONScripterView_nativeSetSentenceFontScale) ( JNIEnv*  env, jobject thiz, double scale )
{
	if (ons && ONScripter::Sentence_font_scale != scale) {
		ONScripter::Sentence_font_scale = scale;
		ons->invalidateSentenceFontSize();
	}
}

JNIEXPORT void JNICALL JAVA_EXPORT_NAME(DemoGLSurfaceView_nativeSaveGameSettings) (JNIEnv *jniEnv, jobject thiz) {
    if (ons) {
        ons->saveGlovalData();
    }
}

JNIEXPORT void JNICALL JAVA_EXPORT_NAME(DemoGLSurfaceView_nativeInitJavaCallbacks) (JNIEnv * jniEnv, jobject thiz)
{
    ONScripter::setJavaEnv(jniEnv, thiz);
}

JNIEXPORT jint JNICALL JAVA_EXPORT_NAME(ONScripterView_nativeGetDialogFontSize) (JNIEnv * jniEnv, jobject thiz)
{
    return ons ? ons->getSentenceFontSize() : 0;
}

void playVideoAndroid(const char *filename, bool click_flag, bool loop_flag)
{
    JNIWrapper wrapper(ONScripter::JNI_VM);
    jchar *jc = new jchar[strlen(filename)];
    for (int i=0 ; i<strlen(filename) ; i++)
        jc[i] = filename[i];
    jcharArray jca = wrapper.env->NewCharArray(strlen(filename));
    wrapper.env->SetCharArrayRegion(jca, 0, strlen(filename), jc);
    wrapper.env->CallVoidMethod( ONScripter::JavaONScripter, ONScripter::JavaPlayVideo, jca, click_flag, loop_flag );
    delete[] jc;
}

int stat_ons(const char* path, struct stat *buf)
{
    if (!ONScripter::Use_java_io) {
        return stat(path, buf);
    }

    JNIWrapper wrapper(ONScripter::JNI_VM);
    JNIEnv * jniEnv = wrapper.env;

    jchar *jc = new jchar[strlen(path)];
    for (int i=0 ; i<strlen(path) ; i++)
        jc[i] = path[i];
    jcharArray jca = jniEnv->NewCharArray(strlen(path));
    jniEnv->SetCharArrayRegion(jca, 0, strlen(path), jc);
    jlong time = jniEnv->CallLongMethod( ONScripter::JavaONScripter, ONScripter::JavaGetStat, jca );
    jniEnv->DeleteLocalRef(jca);
    delete[] jc;

    if (time == -1) {
        return -1;
    }
    if (buf) {
        // Convert milliseconds to seconds
        buf->st_mtime = time / 1000;
    }
    return 0;
}

#undef fopen
FILE *fopen_ons(const char *path, const char *mode)
{
    if (!ONScripter::Use_java_io) {
        return fopen(path, mode);
    }

    int mode2 = 0;
    if (mode[0] == 'w') mode2 = 1;

    JNIWrapper wrapper(ONScripter::JNI_VM);
    JNIEnv * jniEnv = wrapper.env;

    jchar *jc = new jchar[strlen(path)];
    for (int i=0 ; i<strlen(path) ; i++)
        jc[i] = path[i];
    jcharArray jca = jniEnv->NewCharArray(strlen(path));
    jniEnv->SetCharArrayRegion(jca, 0, strlen(path), jc);
    int fd = jniEnv->CallIntMethod( ONScripter::JavaONScripter, ONScripter::JavaGetFD, jca, mode2 );
    jniEnv->DeleteLocalRef(jca);
    delete[] jc;

    return fdopen(fd, mode);
}

#undef mkdir
int mkdir_ons(const char* path, mode_t mode) {
    if (!ONScripter::Use_java_io) {
        return mkdir(path, mode);
    }

    JNIWrapper wrapper(ONScripter::JNI_VM);
    JNIEnv * jniEnv = wrapper.env;

    jchar *jc = new jchar[strlen(path)];
    for (int i=0 ; i<strlen(path) ; i++)
        jc[i] = path[i];
    jcharArray jca = jniEnv->NewCharArray(strlen(path));
    jniEnv->SetCharArrayRegion(jca, 0, strlen(path), jc);
    int ret = jniEnv->CallIntMethod( ONScripter::JavaONScripter, ONScripter::JavaMkdir, jca );
    jniEnv->DeleteLocalRef(jca);
    delete[] jc;
    return ret;
}
}
#endif

#if defined(IOS)
extern "C" void playVideoIOS(const char *filename, bool click_flag, bool loop_flag)
{
    NSString *str = [[NSString alloc] initWithUTF8String:filename];
    id obj = [MoviePlayer alloc];
    [[obj init] play:str click:click_flag loop:loop_flag];
    [obj release];
}
#endif

#if defined(QWS) || defined(ANDROID)
int SDL_main( int argc, char **argv )
#elif defined(PSP)
extern "C" int main( int argc, char **argv )
#else
int main( int argc, char **argv )
#endif
{
    if (!ons) {
        ons = new ONScripter();
    }

    printf("ONScripter version %s(%d.%02d)\n", ONS_VERSION, NSC_VERSION/100, NSC_VERSION%100 );

#if defined(PSP)
    ons->disableRescale();
    ons->enableButtonShortCut();
    SetupCallbacks();
#elif defined(WINCE)
    char currentDir[256];
    strcpy(currentDir, argv[0]);
    char* cptr = currentDir;
    int i, len = strlen(currentDir);
    for(i=len-1; i>0; i--){
        if(cptr[i] == '\\' || cptr[i] == '/')
            break;
    }
    cptr[i] = '\0';
    ons->setArchivePath(currentDir);
    ons->disableRescale();
    ons->enableButtonShortCut();
#elif defined(ANDROID) 
    ons->enableButtonShortCut();
#endif

#if defined(IOS)
#if defined(HAVE_CONTENTS)
    if ([[[DataCopier alloc] init] copy]) exit(-1);
#endif

    // scripts and archives are stored under /Library/Caches
    NSArray* cpaths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES);
    NSString* cpath = [[cpaths objectAtIndex:0] stringByAppendingPathComponent:@"ONS"];
    char filename[256];
    strcpy(filename, [cpath UTF8String]);
    ons->setArchivePath(filename);

    // output files are stored under /Documents
    NSArray* dpaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString* dpath = [[dpaths objectAtIndex:0] stringByAppendingPathComponent:@"ONS"];
    strcpy(filename, [dpath UTF8String]);
    ons->setSaveDir(filename);

#if defined(ZIP_URL)
    if ([[[DataDownloader alloc] init] download]) exit(-1);
#endif

#if defined(USE_SELECTOR)
    // scripts and archives are stored under /Library/Caches
    cpath = [[[ScriptSelector alloc] initWithStyle:UITableViewStylePlain] select];
    strcpy(filename, [cpath UTF8String]);
    ons->setArchivePath(filename);

    // output files are stored under /Documents
    dpath = [[dpaths objectAtIndex:0] stringByAppendingPathComponent:[cpath lastPathComponent]];
    NSFileManager *fm = [NSFileManager defaultManager];
    [fm createDirectoryAtPath:dpath withIntermediateDirectories: YES attributes: nil error:nil];
    strcpy(filename, [dpath UTF8String]);
    ons->setSaveDir(filename);
#endif

#if defined(RENDER_FONT_OUTLINE)
    ons->renderFontOutline();
#endif
#endif

    // ----------------------------------------
    // Parse options
    argv++;
    while( argc > 1 ){
        if ( argv[0][0] == '-' ){
            if ( !strcmp( argv[0]+1, "h" ) || !strcmp( argv[0]+1, "-help" ) ){
                optionHelp();
            }
            else if ( !strcmp( argv[0]+1, "v" ) || !strcmp( argv[0]+1, "-version" ) ){
                optionVersion();
            }
            else if ( !strcmp( argv[0]+1, "-cdaudio" ) ){
                ons->enableCDAudio();
            }
            else if ( !strcmp( argv[0]+1, "-cdnumber" ) ){
                argc--;
                argv++;
                ons->setCDNumber(atoi(argv[0]));
            }
            else if ( !strcmp( argv[0]+1, "f" ) || !strcmp( argv[0]+1, "-font" ) ){
                argc--;
                argv++;
                ons->setFontFile(argv[0]);
            }
            else if ( !strcmp( argv[0]+1, "-registry" ) ){
                argc--;
                argv++;
                ons->setRegistryFile(argv[0]);
            }
            else if ( !strcmp( argv[0]+1, "-dll" ) ){
                argc--;
                argv++;
                ons->setDLLFile(argv[0]);
            }
            else if ( !strcmp( argv[0]+1, "r" ) || !strcmp( argv[0]+1, "-root" ) ){
                argc--;
                argv++;
                ons->setArchivePath(argv[0]);
            }
            else if ( !strcmp( argv[0]+1, "-fullscreen" ) ){
                ons->setFullscreenMode();
            }
            else if ( !strcmp( argv[0]+1, "-window" ) ){
                ons->setWindowMode();
            }
            else if ( !strcmp( argv[0]+1, "-force-button-shortcut" ) ){
                ons->enableButtonShortCut();
            }
            else if ( !strcmp( argv[0]+1, "-enable-wheeldown-advance" ) ){
                ons->enableWheelDownAdvance();
            }
            else if ( !strcmp( argv[0]+1, "-disable-rescale" ) ){
                ons->disableRescale();
            }
            else if ( !strcmp( argv[0]+1, "-render-font-outline" ) ){
                ons->renderFontOutline();
            }
            else if ( !strcmp( argv[0]+1, "-edit" ) ){
                ons->enableEdit();
            }
            else if ( !strcmp( argv[0]+1, "-key-exe" ) ){
                argc--;
                argv++;
                ons->setKeyEXE(argv[0]);
            }
#if defined(ANDROID) 
            else if ( !strcmp( argv[0]+1, "-open-only" ) ){
                argc--;
                argv++;
                if (ons->openScript()) {
                    __android_log_print(ANDROID_LOG_INFO, "ONScripter", "Was not able to open script");
                }
                return 0;
            }
            else if ( !strcmp( argv[0]+1, "l" ) || !strcmp( argv[0]+1, "-language" ) ){
                argc--;
                argv++;
                ons->setMenuLanguage(argv[0]);
            }
            else if ( !strcmp( argv[0]+1, "-audio-hq" )){
                ons->enableHQAudio();
            }
            else if ( !strcmp( argv[0]+1, "-use-java-io" ) ) {
                ONScripter::Use_java_io = true;
            }
            else if ( !strcmp( argv[0]+1, "-screenshot-path" ) ){
                argc--;
                argv++;
                ons->setScreenshotFolder(argv[0]);
            }
#endif
            else{
                logw(stderr, " unknown option %s\n", argv[0] );
            }
        }
        else{
            optionHelp();
        }
        argc--;
        argv++;
    }
    
    // ----------------------------------------
    // Run ONScripter

#ifdef ANDROID
    try {
#endif
    if (ons->openScript()) {
        goto exit;
    }
    if (ons->init()) {
        goto exit;
    }
    ons->executeLabel();
#ifdef ANDROID
    } catch (ScriptException& e) {
        ons->sendException(e);
    }
#endif
exit:
    delete ons;
    ons = NULL;

#if defined(ANDROID)
    // Call finish
    JNIWrapper wrapper(ONScripter::JNI_VM);
    wrapper.env->CallVoidMethod(ONScripter::JavaONScripter, ONScripter::JavaOnFinish);
#endif
    return 0;
}
