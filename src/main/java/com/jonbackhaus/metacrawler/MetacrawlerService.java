package com.jonbackhaus.metacrawler;

import com.nomagic.actions.ActionsCategory;
import com.nomagic.magicdraw.uml.RepresentationTextCreator;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.magicdraw.core.Application;
import javax.jmi.reflect.RefObject;
import org.omg.mof.model.MofAttribute;
import org.omg.mof.model.Reference;
import org.omg.mof.model.ModelElement;
import org.omg.mof.model.Namespace;

import java.util.*;

public class MetacrawlerService {

    private static final int MAX_DEPTH = 5;

    public static void populatePropertyMenu(ActionsCategory parentCategory, Element element, int depth) {
        if (depth >= MAX_DEPTH)
            return;

        log("Populating menu for: " + RepresentationTextCreator.getRepresentedText(element) + " at depth " + depth);

        Map<String, ModelElement> propertyMap = getMetachainProperties(element);
        log("Found " + propertyMap.size() + " properties for " + element.getHumanType());

        List<String> sortedNames = new ArrayList<>(propertyMap.keySet());
        Collections.sort(sortedNames);

        for (String propName : sortedNames) {
            ModelElement propDef = propertyMap.get(propName);
            List<Element> targets = getTargetElements(element, propName);
            if (targets.isEmpty())
                continue;

            String label = getPropertyLabel(propDef);
            log("Property " + label + " has " + targets.size() + " targets");

            ActionsCategory propertyCategory = new ActionsCategory(propDef.refMofId(), label);
            propertyCategory.setNested(true);

            for (Element target : targets) {
                String targetLabel = RepresentationTextCreator.getRepresentedText(target);
                ActionsCategory targetCategory = new ActionsCategory(target.getID(), targetLabel);
                targetCategory.setNested(true);

                targetCategory.addAction(new MetacrawlerAction(target, targetLabel));

                populatePropertyMenu(targetCategory, target, depth + 1);

                propertyCategory.addAction(targetCategory);
            }

            parentCategory.addAction(propertyCategory);
        }
    }

    private static String getPropertyLabel(ModelElement propDef) {
        String name = "";
        try {
            name = propDef.getName();
        } catch (Exception e) {
        }

        // In MOF, we don't have a direct "humanName" but we can use the technical name
        // MagicDraw often adds decorations but technical name is reliable for Metachain
        return name;
    }

    private static Map<String, ModelElement> getMetachainProperties(Element element) {
        Map<String, ModelElement> propertyMap = new HashMap<>();
        if (element instanceof RefObject) {
            RefObject metaObject = ((RefObject) element).refMetaObject();
            if (metaObject instanceof org.omg.mof.model.Class) {
                collectProperties((org.omg.mof.model.Class) metaObject, propertyMap, new HashSet<>());
            }
        }
        return propertyMap;
    }

    private static void collectProperties(org.omg.mof.model.Class mofClass, Map<String, ModelElement> propertyMap,
            Set<org.omg.mof.model.Class> visited) {
        if (mofClass == null || !visited.add(mofClass))
            return;

        try {
            for (Object content : mofClass.getContents()) {
                if (content instanceof MofAttribute || content instanceof Reference) {
                    ModelElement me = (ModelElement) content;
                    if (!propertyMap.containsKey(me.getName())) {
                        propertyMap.put(me.getName(), me);
                    }
                }
            }

            // Collect from supertypes
            for (Object supertype : mofClass.getSupertypes()) {
                if (supertype instanceof org.omg.mof.model.Class) {
                    collectProperties((org.omg.mof.model.Class) supertype, propertyMap, visited);
                }
            }
        } catch (Exception e) {
            log("Error collecting properties from " + mofClass + ": " + e.getMessage());
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
                // Ignore
            }
        }
        return targets;
    }

    private static void log(String message) {
        Application.getInstance().getGUILog().log("[Metacrawler] " + message);
        System.out.println("[Metacrawler] " + message);
    }
}
