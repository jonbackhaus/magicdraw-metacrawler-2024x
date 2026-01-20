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

    // Balancing depth with performance. 3-4 levels is usually enough for a
    // hover-menu.
    private static final int MAX_DEPTH = 3;

    // Global cache for metamodel properties (once discovered, they don't change).
    private static final Map<String, List<ModelElement>> metamodelCache = new ConcurrentHashMap<>();

    /**
     * Populates the Metacrawler menu recursively.
     * Structure: Element -> Properties -> Targets -> Properties -> Targets...
     */
    public static void populatePropertyMenu(ActionsCategory parentCategory, Element element, int depth,
            Set<String> visitedIds) {
        if (depth >= MAX_DEPTH || element == null)
            return;

        // Prevent infinite loops in cycles, but allow traversing the same element at
        // different levels if needed.
        // For a context menu, we usually want to avoid re-including an element that's
        // already in the *ancestry* of this menu branch.
        String elementId = element.getID();
        if (visitedIds.contains(elementId))
            return;

        visitedIds.add(elementId);

        List<ModelElement> properties = getCachedMetamodelProperties(element);

        // Sort properties by name
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

            // Property Submenu
            ActionsCategory propertyCategory = new ActionsCategory(propDef.refMofId(), propName);
            propertyCategory.setNested(true);

            for (Element target : targets) {
                String targetLabel = RepresentationTextCreator.getRepresentedText(target);

                // Target Submenu (to allow further recursion on hover)
                ActionsCategory targetCategory = new ActionsCategory(target.getID(), targetLabel);
                targetCategory.setNested(true);

                // First action: Navigate to this element
                targetCategory.addAction(new MetacrawlerAction(target, "➡️ Select: " + targetLabel));

                // Recurse: Show properties of this target
                populatePropertyMenu(targetCategory, target, depth + 1, new HashSet<>(visitedIds));

                propertyCategory.addAction(targetCategory);
            }

            parentCategory.addAction(propertyCategory);
        }
    }

    private static List<ModelElement> getCachedMetamodelProperties(Element element) {
        if (!(element instanceof RefObject))
            return Collections.emptyList();

        RefObject metaObject = ((RefObject) element).refMetaObject();
        String mofId = metaObject.refMofId();

        return metamodelCache.computeIfAbsent(mofId, id -> {
            List<ModelElement> props = new ArrayList<>();
            if (metaObject instanceof org.omg.mof.model.Class) {
                collectProperties((org.omg.mof.model.Class) metaObject, props, new HashSet<>());
            }
            return props;
        });
    }

    private static void collectProperties(org.omg.mof.model.Class mofClass, List<ModelElement> props,
            Set<org.omg.mof.model.Class> visited) {
        if (mofClass == null || !visited.add(mofClass))
            return;

        try {
            for (Object content : mofClass.getContents()) {
                if (content instanceof MofAttribute || content instanceof Reference) {
                    props.add((ModelElement) content);
                }
            }
            for (Object supertype : mofClass.getSupertypes()) {
                if (supertype instanceof org.omg.mof.model.Class) {
                    collectProperties((org.omg.mof.model.Class) supertype, props, visited);
                }
            }
        } catch (Exception e) {
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
            }
        }
        return targets;
    }

    private static void log(String message) {
        // System.out.println("[Metacrawler] " + message);
    }
}
