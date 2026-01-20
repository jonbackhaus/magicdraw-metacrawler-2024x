package com.jonbackhaus.metacrawler;

import com.nomagic.actions.ActionsCategory;
import com.nomagic.actions.ActionsManager;
import com.nomagic.magicdraw.actions.BrowserContextAMConfigurator;
import com.nomagic.magicdraw.ui.browser.Tree;
import com.nomagic.magicdraw.ui.browser.Node;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.magicdraw.core.Application;

public class MetacrawlerMenuConfigurator implements BrowserContextAMConfigurator {

    @Override
    public void configure(ActionsManager manager, Tree tree) {
        Node selectedNode = tree.getSelectedNode();
        if (selectedNode != null && selectedNode.getUserObject() instanceof Element) {
            Element element = (Element) selectedNode.getUserObject();

            Application.getInstance().getGUILog().log("[Metacrawler] Configuring menu for " + element.getHumanName());

            ActionsCategory metacrawlerCategory = new ActionsCategory("METACRAWLER", "Metacrawler");
            metacrawlerCategory.setNested(true);

            // Build the non-recursive menu for maximum performance
            MetacrawlerService.populatePropertyMenu(metacrawlerCategory, element);

            manager.addCategory(metacrawlerCategory);
        }
    }

    @Override
    public int getPriority() {
        return 0;
    }
}
