package com.jonbackhaus.metacrawler;

import com.nomagic.actions.ActionsCategory;
import com.nomagic.magicdraw.uml.RepresentationTextCreator;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import javax.jmi.reflect.RefObject;

import java.util.*;

public class MetacrawlerService {

    private static final int MAX_DEPTH = 5;

    public static void populatePropertyMenu(ActionsCategory parentCategory, Element element, int depth) {
        if (depth >= MAX_DEPTH)
            return;

        List<Property> properties = getMetachainProperties(element);
        properties.sort(Comparator.comparing(MetacrawlerService::getPropertyLabel));

        for (Property property : properties) {
            List<Element> targets = getTargetElements(element, property);
            if (targets.isEmpty())
                continue;

            String label = getPropertyLabel(property);
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
            if (metaObject instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class) {
                Collection<?> allFeatures = ((com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class) metaObject)
                        .getFeature();
                for (Object feature : allFeatures) {
                    if (feature instanceof Property) {
                        Property p = (Property) feature;
                        if (p.getType() != null) {
                            props.add(p);
                        }
                    }
                }
            }
        }
        return props;
    }

    private static List<Element> getTargetElements(Element element, Property property) {
        List<Element> targets = new ArrayList<>();
        if (element instanceof RefObject) {
            try {
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
                // Ignore
            }
        }
        return targets;
    }
}
