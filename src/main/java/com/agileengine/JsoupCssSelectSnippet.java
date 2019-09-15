package com.agileengine;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


public class JsoupCssSelectSnippet {

    private static Logger LOGGER = LoggerFactory.getLogger(JsoupCssSelectSnippet.class);

    private static String CHARSET_NAME = "utf8";

    private Map<Element, Integer> elementIntegerMap = new HashMap<>();

    private static Optional<Elements> findElementsByQuery(File htmlFile, String cssQuery) {
        try {
            Document doc = Jsoup.parse(
                    htmlFile,
                    CHARSET_NAME,
                    htmlFile.getAbsolutePath());

            return Optional.of(doc.select(cssQuery));

        } catch (IOException e) {
            LOGGER.error("Error reading [{}] file", htmlFile.getAbsolutePath(), e);
            return Optional.empty();
        }
    }

    public void printCssPathForFoundelement(Attributes attributes, String resourcePath) {
        List<Element> elements = getElementsByAttributes(attributes, resourcePath);

        prioritizeElements(elements, attributes);

        LOGGER.info("Element path: {}", getElementPath());
    }

    private List<Element> getElementsByAttributes(Attributes attributes, String resourcePath) {
        List<Element> elements = new ArrayList<>();
        attributes.asList().stream()
                .map(a -> findElementsByQuery(new File(resourcePath), getCssQuery(a)))
                .forEach(e -> elements.addAll(e.get()));
        return elements;
    }

    private void prioritizeElements(List<Element> elements, Attributes attributes) {
        for (Attribute attribute : attributes) {
            for (Element element : elements) {
                if (!"".equals(element.attributes().get(attribute.getKey()))) {
                    elementIntegerMap.putIfAbsent(element, 0);
                    elementIntegerMap.put(element, elementIntegerMap.get(element) + 1);
                }
            }
        }
    }

    private Element getElementByPriority() {
        final Map<Element, Integer> sortedByCount = elementIntegerMap.entrySet()
                .stream()
                .sorted((Map.Entry.<Element, Integer>comparingByValue().reversed()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        int priorityIndex = sortedByCount.values().stream().findFirst().get();
        LOGGER.info("Priority index [{}]", priorityIndex);

        return sortedByCount.entrySet().stream().map(Map.Entry::getKey).findFirst().get();
    }

    private String getElementPath() {
        Element foundedElement = getElementByPriority();

        List<String> path = new ArrayList<>();
        path.add(foundedElement.tagName());
        Element parent = (Element) foundedElement.parentNode();
        while (parent.parentNode() != null) {
            path.add(parent.tagName());
            parent = (Element) parent.parentNode();
        }

        Collections.reverse(path);

        return String.join(" > ", path);
    }

    private String getCssQuery(Attribute atr) {
        return String.format("a[%s=%s]", atr.getKey(), atr.getValue());
    }


}