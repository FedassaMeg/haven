package org.haven.reporting.application.services;

import org.haven.reporting.domain.ExportFormat;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Formats HUD export data into CSV, XML, or Excel formats following HUD specifications.
 * Applies format-specific quirks like column ordering, padding, and encoding requirements.
 */
@Service
public class HUDExportFormatter {

    private final CSVExportStrategy csvStrategy;
    private final XMLExportStrategy xmlStrategy;
    private final ExcelExportStrategy excelStrategy;

    public HUDExportFormatter(
            CSVExportStrategy csvStrategy,
            XMLExportStrategy xmlStrategy,
            ExcelExportStrategy excelStrategy) {
        this.csvStrategy = csvStrategy;
        this.xmlStrategy = xmlStrategy;
        this.excelStrategy = excelStrategy;
    }

    /**
     * Format export sections into the specified format.
     *
     * @param sections Map of section name to list of row data
     * @param format Target export format
     * @return Formatted byte array
     */
    public byte[] format(Map<String, List<Map<String, Object>>> sections, ExportFormat format) {
        return switch (format) {
            case CSV -> csvStrategy.format(sections);
            case XML -> xmlStrategy.format(sections);
            case EXCEL -> excelStrategy.format(sections);
        };
    }

    /**
     * Format a single section into the specified format.
     */
    public byte[] formatSection(String sectionName, List<Map<String, Object>> rows, ExportFormat format) {
        return format(Map.of(sectionName, rows), format);
    }

    /**
     * Strategy interface for format-specific implementations.
     */
    public interface FormatStrategy {
        byte[] format(Map<String, List<Map<String, Object>>> sections);
    }
}
