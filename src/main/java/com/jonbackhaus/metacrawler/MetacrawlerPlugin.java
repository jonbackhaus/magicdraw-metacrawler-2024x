package com.jonbackhaus.metacrawler;

import com.nomagic.magicdraw.plugins.Plugin;
import com.nomagic.magicdraw.actions.ActionsConfiguratorsManager;

public class MetacrawlerPlugin extends Plugin {

    @Override
    public void init() {
        ActionsConfiguratorsManager manager = ActionsConfiguratorsManager.getInstance();
        manager.addContainmentBrowserContextConfigurator(new MetacrawlerMenuConfigurator());
    }

    @Override
    public boolean close() {
        return true;
    }

    @Override
    public boolean isSupported() {
        return true;
    }
}
