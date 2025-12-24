package com.karin.idTech4Amm.SDK;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * idTech4A++ game boot intent
 */
public final class KGameIntent
{
    public String              game;
    public String              game_base;
    public String              dll;
    public String              mod;
    public String              command;
    public Map<String, String> parms;

    private String DLLPath(Context context)
    {
        return KUtils.GetGameLibDir(context) + "/lib" + dll + ".so";
    }

    public void AddParm(String name, Object value)
    {
        if(null == parms)
            parms = new LinkedHashMap<>();
        parms.put(name, Objects.toString(value));
    }

    private void Print()
    {
        List<String> list = new ArrayList<>();
        if(null != parms)
        {
            for(String key : parms.keySet())
            {
                String value = parms.get(key);
                if(null != value && !value.isEmpty()) // as prop if value is not empty -> +set key value
                    list.add(key + "=" + Objects.toString(parms.get(key), ""));
                else // else as command -> +key
                    list.add(key);
            }
        }
        Log.i("SDK", String.format("Intent: game=%s\nmod=%s\ndll=%s\ngame_base=%s\ncommand=%s\nparams:\n%s", game, mod, dll, game_base, command, String.join(" ", list)));
    }

    public void Build(Context context, Intent intent)
    {
        Print();

        String dllPath = DLLPath(context);
        KidTech4Command cmd = new KidTech4Command(KConstants.GAME_EXECUTABLE);
        // 1. add custom params first
        if(null != parms)
        {
            for(String key : parms.keySet())
            {
                String value = parms.get(key);
                if(null != value && !value.isEmpty()) // as prop if value is not empty -> +set key value
                    cmd.SetProp(key, value);
                else // else as command -> +key
                    cmd.SetParam(key, "");
            }
        }
        // 2. then add special params
        cmd.SetProp("fs_game", mod);
        if(null != game_base && !game_base.isEmpty())
            cmd.SetProp("fs_game_base", game_base);
        cmd.SetProp("harm_fs_gameLibPath", dllPath);
        // 2. finally add extras commands
        if(null == command)
            command = cmd.toString();
        else
            command = cmd + " " + command;

        // 3. print final intent
        Log.i("SDK", String.format("Start: game=%s\ndll=%s\ncommand=%s", game, dllPath, command));

        // 4. setup Android Intent
        intent.setComponent(new ComponentName(KConstants.IDTECH4AMM_PACKAGE_NAME, KConstants.IDTECH4AMM_ACTIVITY_NAME))
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                // 1. Setup game type
                .putExtra("game", game)
                // 2. Setup game command
                .putExtra("command", command)
        ;
    }
}
