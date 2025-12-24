package com.karin.idTech4Amm.SDK;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.View;

public class KUtils
{
    public static boolean IsAppInstalled(Activity ctx, String nm)
    {
        try
        {
            ctx.getPackageManager().getPackageInfo(nm, PackageManager.GET_ACTIVITIES);
            return true;
        }
        catch(Exception e)
        {
            return false;
        }
    }

    public static String GetGameLibDir(Context context)
    {
        try
        {
            ApplicationInfo ainfo = context.getApplicationContext().getPackageManager().getApplicationInfo
                    (
                            context.getApplicationContext().getPackageName(),
                            PackageManager.GET_SHARED_LIBRARY_FILES
                    );
            return ainfo.nativeLibraryDir; //k for arm64-v8a apk install
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return context.getCacheDir().getAbsolutePath().replace("cache", "lib");        //k old, can work with armv5 and armv7-a
        }
    }

    /*
    @SuppressLint("InlinedApi")
    private final int m_uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN;
    @SuppressLint("InlinedApi")
    private final int m_uiOptions_def = View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
     */
    public static int GetFullScreenFlags(boolean hideNav)
    {
        int m_uiOptions = 0;

        if(hideNav)
        {
            m_uiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            {
                m_uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
            m_uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            m_uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            m_uiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        }
        return m_uiOptions;
    }
}
