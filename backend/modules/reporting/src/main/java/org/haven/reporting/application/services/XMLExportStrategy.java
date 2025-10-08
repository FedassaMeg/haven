package org.haven.reporting.application.services;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * XML export strategy following HUD HMIS XML specifications.
 * Generates XML documents validated against HUD_HMIS.xsd schema.
 */
@Component
public class XMLExportStrategy implements HUDExportFormatter.FormatStrategy {

    private static final String NAMESPACE_URI = "https://www.hudhdx.info/Resources/Vendors/HMIS/HUD_HMIS.xsd";
    private static final String SCHEMA_VERSION = "2024";

    @Override
    public byte[] format(Map<String, List<Map<String, Object>>> sections) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            // Create root element with namespace
            Element root = doc.createElementNS(NAMESPACE_URI, "HMISExport");
            root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            root.setAttribute("xsi:schemaLocation", NAMESPACE_URI + " HUD_HMIS.xsd");
            root.setAttribute("version", SCHEMA_VERSION);
            doc.appendChild(root);

            // Add sections
            for (Map.Entry<String, List<Map<String, Object>>> section : sections.entrySet()) {
                String sectionName = section.getKey();
                List<Map<String, Object>> rows = section.getValue();

                Element sectionElement = doc.createElement(sectionName + "s");
                root.appendChild(sectionElement);

                for (Map<String, Object> row : rows) {
                    Element rowElement = doc.createElement(sectionName);

                    for (Map.Entry<String, Object> field : row.entrySet()) {
                        Element fieldElement = doc.createElement(field.getKey());
                        fieldElement.setTextContent(formatValue(field.getValue()));
                        rowElement.appendChild(fieldElement);
                    }

                    sectionElement.appendChild(rowElement);
                }
            }

            // Transform to byte array
            return transformToBytes(doc);

        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Failed to create XML document", e);
        }
    }

    private byte[] transformToBytes(Document doc) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            // Format output
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "no");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(baos);

            transformer.transform(source, result);

            return baos.toByteArray();

        } catch (TransformerException e) {
            throw new RuntimeException("Failed to transform XML document", e);
        }
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDate date) {
            return date.toString(); // ISO 8601 format
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.toString();
        }
        return value.toString();
    }
}
