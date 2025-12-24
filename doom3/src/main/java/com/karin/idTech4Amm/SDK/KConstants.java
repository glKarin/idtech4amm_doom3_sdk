package com.karin.idTech4Amm.SDK;

/**
 * Constants
 */
public final class KConstants
{
    public static final String GAME_TYPE                = "doom3"; // game type: doom3
    public static final String GAME_DLL_NAME            = "game"; // game library name: libXXX.so
    public static final String GAME_MOD_NAME            = "bloodmod"; // game mod name(fs_game)
    public static final String GAME_EXECUTABLE          = "game.arm"; // initial game executable
    public static final String IDTECH4AMM_PACKAGE_NAME  = "com.karin.idTech4Amm"; // idTech4A++ application package nameexecutable
    public static final String IDTECH4AMM_ACTIVITY_NAME = "com.n0n3m4.q3e.Q3EMain"; // idTech4A++ game activity name

    // meta-data name in AndroidManifest.xml
    public static final String META_COMMAND   = "COMMAND"; // extras commands string
    public static final String META_GAME      = "GAME"; // game type
    public static final String META_MOD       = "MOD"; // mod name(fs_game)
    public static final String META_DLL       = "DLL"; // library name
    public static final String META_GAME_BASE = "GAME_BASE"; // fs_game_base

    private KConstants() {}
}
