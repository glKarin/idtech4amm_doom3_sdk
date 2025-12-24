package com.karin.doom3;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.karin.idTech4Amm.SDK.KConstants;
import com.karin.idTech4Amm.SDK.KGameIntent;
import com.karin.idTech4Amm.SDK.KUtils;

import java.util.Objects;
import java.util.Set;

/**
 * DOOM3 boot activity
 */
public class Main extends Activity
{
    /*
    Intent::extras
    game: game type
    command: command line arguments
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // setup
        SetupActivity();

        // create
        super.onCreate(savedInstanceState);

        // start
        StartGame();
    }

    private void StartGame()
    {
        String errorMsg = null;
        do
        {
            // check idTech4Amm is installed
            if(!KUtils.IsAppInstalled(this, KConstants.IDTECH4AMM_PACKAGE_NAME))
            {
                errorMsg = KConstants.IDTECH4AMM_PACKAGE_NAME + " is not installed";
                break;
            }

            // create launch intent
            KGameIntent intent = new KGameIntent();
            intent.game = KConstants.GAME_TYPE;
            intent.dll = KConstants.GAME_DLL_NAME;
            intent.mod = KConstants.GAME_MOD_NAME;

            // override default constants parameters from AndroidManifest.xml <meta-data>
            ActivityInfo info = null;
            try
            {
                info = getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
            }
            catch(PackageManager.NameNotFoundException e)
            {
                e.printStackTrace();
            }
            if(null != info)
            {
                Set<String> keys = info.metaData.keySet();
                for(String key : keys)
                {
                    Object value = info.metaData.get(key);
                    if(KConstants.META_GAME.equalsIgnoreCase(key))
                    {
                        intent.game = Objects.toString(value, KConstants.GAME_TYPE);
                    }
                    else if(KConstants.META_COMMAND.equalsIgnoreCase(key))
                    {
                        intent.command = Objects.toString(value, "");
                    }
                    else if(KConstants.META_MOD.equalsIgnoreCase(key))
                    {
                        intent.mod = Objects.toString(value, KConstants.GAME_MOD_NAME);
                    }
                    else if(KConstants.META_DLL.equalsIgnoreCase(key))
                    {
                        intent.dll = Objects.toString(value, KConstants.GAME_DLL_NAME);
                    }
                    else if(KConstants.META_GAME_BASE.equalsIgnoreCase(key))
                    {
                        intent.game_base = Objects.toString(value);
                    }
                    else
                    {
                        intent.AddParm(key, value);
                    }
                }
            }

            // start game
            try
            {
                Intent androidIntent = new Intent();
                intent.Build(this, androidIntent);
                startActivity(androidIntent);
            }
            catch(Exception e)
            {
                e.printStackTrace();
                errorMsg = e.getMessage();
            }
        } while(false);

        // print message if error
        if(null != errorMsg)
        {
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            Log.e(getClass().getName(), errorMsg);
        }

        // finish this activity
        finish();
    }

    private void SetupActivity()
    {
        // setup fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // make screen always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // setup fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // setup screen edges
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }

        // force landscape orientation
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) // 9
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }

        // hide navigation bar
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(KUtils.GetFullScreenFlags(true));
    }
}
