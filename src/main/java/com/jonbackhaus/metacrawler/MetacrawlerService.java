package com.jonbackhaus.metacrawler;

import com.nomagic.actions.ActionsCategory;
import com.nomagic.magicdraw.uml.RepresentationTextCreator;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.magicdraw.core.Application;
import javax.jmi.reflect.RefObject;
import org.omg.mof.model.MofAttribute;
import org.omg.mof.model.Reference;
import org.omg.mof.model.ModelElement;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MetacrawlerService {

    // LIMIT DEPTH to 1 to prevent UI locking and enable truly "interactive"
    // step-by-step crawling.
    // Deep recursion in context menus is non-performant.
    private static final int MAX_DEPTH = 1;

    // Cache for properties discovered per MetaClass MOF ID to avoid repeated
    // reflexive lookups.
    private static final Map<String, List<ModelElement>> metaclassPropertyCache = new ConcurrentHashMap<>();

    public static void populatePropertyMenu(ActionsCategory parentCategory, Element element, int depth) {
        if (depth >= MAX_DEPTH)
            return;

        log("Populating menu for: " + RepresentationTextCreator.getRepresentedText(element) + " at depth " + depth);

        List<ModelElement> properties = getCachedProperties(element);
        log("Discovered " + properties.size() + " metachain properties for " + element.getHumanType());

        // We sort them for readability
        properties.sort(Comparator.comparing(p -> {
            try {
                return p.getName();
            } catch (Exception e) {
                return "";
            }
        }));

        for (ModelElement propDef : properties) {
            String propName = "";
            try {
                propName = propDef.getName();
            } catch (Exception e) {
                continue;
            }

            List<Element> targets = getTargetElements(element, propName);
            if (targets.isEmpty())
                continue;

            ActionsCategory propertyCategory = new ActionsCategory(propDef.refMofId(), propName);
            propertyCategory.setNested(true);

            for (Element target : targets) {
                String targetLabel = RepresentationTextCreator.getRepresentedText(target);

                // Add an action to select this target
                MetacrawlerAction navigationAction = new MetacrawlerAction(target, targetLabel);
                propertyCategory.addAction(navigationAction);

                // We DON'T recurse deeply here anymore to prevent UI hangs.
                // The user "crawls" by selecting the target and then right-clicking it.
            }

            parentCategory.addAction(propertyCategory);
        }
    }

    private static List<ModelElement> getCachedProperties(Element element) {
        if (!(element instanceof RefObject))
            return Collections.emptyList();

        RefObject metaObject = ((RefObject) element).refMetaObject();
        String mofId = metaObject.refMofId();

        return metaclassPropertyCache.computeIfAbsent(mofId, id -> {
            List<ModelElement> props = new ArrayList<>();
            if (metaObject instanceof org.omg.mof.model.Class) {
                org.omg.mof.model.Class mofClass = (org.omg.mof.model.Class) metaObject;
                Set<org.omg.mof.model.Class> visited = new HashSet<>();
                collectProperties(mofClass, props, visited);
            }
            return props;
        });
    }

    private static void collectProperties(org.omg.mof.model.Class mofClass, List<ModelElement> props,
            Set<org.omg.mof.model.Class> visited) {
        if (mofClass == null || !visited.add(mofClass))
            return;

        try {
            // Get immediate contents
            for (Object content : mofClass.getContents()) {
                if (content instanceof MofAttribute || content instanceof Reference) {
                    props.add((ModelElement) content);
                }
            }

            // Collect from supertypes
            for (Object supertype : mofClass.getSupertypes()) {
                if (supertype instanceof org.omg.mof.model.Class) {
                    collectProperties((org.omg.mof.model.Class) supertype, props, visited);
                }
            }
        } catch (Exception e) {
            log("Error collecting properties: " + e.getMessage());
        }
    }

    private static List<Element> getTargetElements(Element element, String propertyName) {
        List<Element> targets = new ArrayList<>();
        if (element instanceof RefObject) {
            try {
                Object value = ((RefObject) element).refGetValue(propertyName);
                if (value instanceof Element) {
                    targets.add((Element) value);
                } else if (value instanceof Collection) {
                    for (Object item : (Collection<?>) value) {
                        if (item instanceof Element) {
                            targets.add((Element) item);
                        }
                    }
                }
            } catch (Exception e) {
                // Property might not exist on this specific instance or other JMI error
            }
        }
        return targets;
    }

    private static void log(String message) {
        Application.getInstance().getGUILog().log("[Metacrawler] " + message);
        System.out.println("[Metacrawler] " + message);
    }
}
