package com.jonbackhaus.metacrawler;

import com.nomagic.actions.ActionsCategory;
import com.nomagic.actions.NMAction;
import com.nomagic.magicdraw.uml.RepresentationTextCreator;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.magicdraw.core.Application;
import javax.jmi.reflect.RefObject;

import java.util.*;

public class MetacrawlerService {

    private static final int MAX_DEPTH = 5;

    public static void populatePropertyMenu(ActionsCategory parentCategory, Element element, int depth) {
        if (depth >= MAX_DEPTH)
            return;

        log("Populating menu for: " + RepresentationTextCreator.getRepresentedText(element) + " at depth " + depth);

        List<Property> properties = getMetachainProperties(element);
        log("Found " + properties.size() + " properties for " + element.getHumanType());

        properties.sort(Comparator.comparing(MetacrawlerService::getPropertyLabel));

        for (Property property : properties) {
            List<Element> targets = getTargetElements(element, property);
            if (targets.isEmpty())
                continue;

            String label = getPropertyLabel(property);
            log("Property " + label + " has " + targets.size() + " targets");

            ActionsCategory propertyCategory = new ActionsCategory(property.getID(), label);
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

    private static String getPropertyLabel(Property property) {
        String friendlyName = property.getName();
        if (friendlyName == null || friendlyName.isEmpty()) {
            friendlyName = property.getHumanName();
        }
        return String.format("%s (%s)", friendlyName, property.getName());
    }

    private static List<Property> getMetachainProperties(Element element) {
        List<Property> props = new ArrayList<>();
        if (element instanceof RefObject) {
            RefObject metaObject = ((RefObject) element).refMetaObject();
            log("MetaObject type: " + metaObject.getClass().getName());

            // In MD, the metaclass often implements the Kernel.Class interface
            if (metaObject instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class) {
                com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class metaclass = (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class) metaObject;

                // Get features (includes attributes/properties)
                // We should probably look at all features that are Properties
                Collection<?> features = metaclass.getFeature();
                for (Object feature : features) {
                    if (feature instanceof Property) {
                        props.add((Property) feature);
                    }
                }

                // Also check inherited features? metaclass.getFeature() might not be enough.
                // But for now let's see why it's empty.
            } else {
                log("MetaObject is NOT a Kernel.Class");
            }
        } else {
            log("Element is NOT a RefObject");
        }
        return props;
    }

    private static List<Element> getTargetElements(Element element, Property property) {
        List<Element> targets = new ArrayList<>();
        if (element instanceof RefObject) {
            try {
                // Try to get by property name
                Object value = ((RefObject) element).refGetValue(property.getName());
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
                log("Error getting values for property " + property.getName() + ": " + e.getMessage());
            }
        }
        return targets;
    }

    private static void log(String message) {
        Application.getInstance().getGUILog().log("[Metacrawler] " + message);
        System.out.println("[Metacrawler] " + message);
    }
}
