package com.clutch.mastery.task;

import com.clutch.mastery.manager.MasteryManager;

public class AutoSaveTask implements Runnable {
    private final MasteryManager masteryManager;

    public AutoSaveTask(MasteryManager masteryManager) {
        this.masteryManager = masteryManager;
    }

    @Override
    public void run() {
        masteryManager.saveAllOnlineAsync();
    }
}
