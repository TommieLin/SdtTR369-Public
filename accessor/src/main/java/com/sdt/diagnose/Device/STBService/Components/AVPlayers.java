package com.sdt.diagnose.Device.STBService.Components;

import com.sdt.annotations.Tr369Get;

public class AVPlayers {

    @Tr369Get("Device.Services.STBService.1.AVPlayers.AVPlayer.1.Name")
    public String SK_TR369_GetAVPlayerName1() {
        return AVPlayersManager.getInstance().getAVPlayerName1();
    }

    @Tr369Get("Device.Services.STBService.1.AVPlayers.AVPlayer.2.Name")
    public String SK_TR369_GetAVPlayerName2() {
        return AVPlayersManager.getInstance().getAVPlayerName2();
    }

    @Tr369Get("Device.Services.STBService.1.AVPlayers.AVPlayer.3.Name")
    public String SK_TR369_GetAVPlayerName3() {
        return AVPlayersManager.getInstance().getAVPlayerName3();
    }

}
