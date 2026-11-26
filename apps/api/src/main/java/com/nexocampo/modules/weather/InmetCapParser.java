package com.nexocampo.modules.weather;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

final class InmetCapParser {

    private static final int MAX_ENTRIES = 80;
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private InmetCapParser() {}

    static List<IncomingAlert> parse(String xml, OffsetDateTime now) {
        if (xml == null || xml.isBlank()) {
            return List.of();
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            Element root = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                    .getDocumentElement();
            if ("alert".equals(localName(root))) {
                IncomingAlert alert = toAlert(root, now);
                return alert == null ? List.of() : List.of(alert);
            }
            List<Element> entries = new ArrayList<>();
            collectEntries(root, entries);
            List<IncomingAlert> alerts = new ArrayList<>();
            for (Element entry : entries) {
                if (alerts.size() >= MAX_ENTRIES) {
                    break;
                }
                try {
                    IncomingAlert alert = toAlert(entry, now);
                    if (alert != null) {
                        alerts.add(alert);
                    }
                } catch (RuntimeException ignored) {
                    // um item inválido não derruba o feed inteiro
                }
            }
            return List.copyOf(alerts);
        } catch (Exception ex) {
            return List.of();
        }
    }

    static List<String> itemLinks(String xml) {
        if (xml == null || xml.isBlank()) {
            return List.of();
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            Element root = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                    .getDocumentElement();
            List<Element> entries = new ArrayList<>();
            collectEntries(root, entries);
            List<String> preferred = new ArrayList<>();
            List<String> others = new ArrayList<>();
            for (Element entry : entries) {
                String link = text(entry, "link");
                if (link == null || !link.startsWith("https://")) {
                    continue;
                }
                CategoriaAlertaClimatico category = WeatherAlertRule.fromInmetText(text(entry, "title"));
                if (category == null) {
                    continue;
                }
                if (WeatherAlertRule.DEFAULT_CATEGORIES.contains(category)) {
                    preferred.add(link);
                } else {
                    others.add(link);
                }
            }
            preferred.addAll(others);
            return List.copyOf(preferred);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static void collectEntries(Element element, List<Element> entries) {
        String name = localName(element);
        if ("entry".equals(name) || "item".equals(name)) {
            entries.add(element);
            return;
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element childElement) {
                collectEntries(childElement, entries);
            }
        }
    }

    private static IncomingAlert toAlert(Element entry, OffsetDateTime now) {
        String text = join(
                text(entry, "title"),
                text(entry, "event"),
                text(entry, "summary"),
                text(entry, "description"),
                text(entry, "content"));
        CategoriaAlertaClimatico category = WeatherAlertRule.fromInmetText(text);
        if (category == null) {
            return null;
        }
        String externalId = first(text(entry, "identifier"), text(entry, "id"), text(entry, "guid"));
        if (externalId == null || externalId.isBlank()) {
            return null;
        }
        if (externalId.length() > 200) {
            externalId = externalId.substring(0, 200);
        }
        OffsetDateTime start = parseTime(first(text(entry, "onset"), text(entry, "effective"), text(entry, "updated")), now);
        OffsetDateTime end = parseTime(first(text(entry, "expires"), text(entry, "pubDate")), start.plusHours(24));
        if (!end.isAfter(now)) {
            return null;
        }
        List<List<double[]>> rings = polygons(entry);
        AlertGeometry geometry = !rings.isEmpty() ? AlertGeometry.polygons(rings) : circle(entry);
        if (geometry == null) {
            geometry = AlertGeometry.none();
        }
        if (geometry.rings().isEmpty() && geometry.circleLatitude() == null && geometry.latitude() == null) {
            return null;
        }
        return new IncomingAlert(
                FonteAlertaClimatico.INMET.name(),
                externalId,
                category,
                start,
                end,
                geometry,
                trim(text, 2000));
    }

    private static AlertGeometry circle(Element entry) {
        String raw = text(entry, "circle");
        if (raw == null) {
            return null;
        }
        String[] parts = raw.trim().split("\\s+");
        if (parts.length < 2) {
            return null;
        }
        String[] coords = parts[0].split(",");
        if (coords.length != 2) {
            return null;
        }
        try {
            double lat = Double.parseDouble(coords[0]);
            double lon = Double.parseDouble(coords[1]);
            double radius = Double.parseDouble(parts[1]);
            return AlertGeometry.circle(lat, lon, radius);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static List<List<double[]>> polygons(Element entry) {
        List<List<double[]>> rings = new ArrayList<>();
        for (String raw : texts(entry, "polygon")) {
            List<double[]> points = new ArrayList<>();
            for (String pair : raw.trim().split("\\s+")) {
                String[] coords = pair.split(",");
                if (coords.length != 2) {
                    continue;
                }
                try {
                    points.add(new double[] {Double.parseDouble(coords[0]), Double.parseDouble(coords[1])});
                } catch (NumberFormatException ignored) {
                    points.clear();
                    break;
                }
            }
            if (points.size() >= 3) {
                rings.add(List.copyOf(points));
            }
        }
        return rings;
    }

    private static List<String> texts(Element element, String name) {
        List<String> values = new ArrayList<>();
        collectText(element, name, values);
        return values;
    }

    private static void collectText(Element element, String name, List<String> values) {
        if (name.equals(localName(element))) {
            String value = element.getTextContent();
            if (value != null && !value.isBlank()) {
                values.add(value.trim());
            }
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element childElement) {
                collectText(childElement, name, values);
            }
        }
    }

    private static String text(Element element, String name) {
        List<String> values = texts(element, name);
        return values.isEmpty() ? null : values.get(0);
    }

    private static String localName(Element element) {
        String name = element.getLocalName();
        if (name != null) {
            return name;
        }
        String nodeName = element.getNodeName();
        int separator = nodeName.indexOf(':');
        return separator >= 0 ? nodeName.substring(separator + 1) : nodeName;
    }

    private static OffsetDateTime parseTime(String raw, OffsetDateTime fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String value = raw.trim();
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            // tenta o próximo formato
        }
        try {
            return Instant.parse(value).atOffset(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            // tenta data local
        }
        try {
            return LocalDateTime.parse(value).atZone(ZONE).toOffsetDateTime();
        } catch (DateTimeException ignored) {
            return fallback;
        }
    }

    private static String first(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String join(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(value.trim());
        }
        return builder.toString();
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
