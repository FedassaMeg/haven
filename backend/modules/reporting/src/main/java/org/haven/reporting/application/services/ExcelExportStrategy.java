package org.haven.reporting.application.services;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Excel export strategy using Apache POI.
 * Creates separate sheets per CSV section with formatted headers.
 */
@Component
public class ExcelExportStrategy implements HUDExportFormatter.FormatStrategy {

    @Override
    public byte[] format(Map<String, List<Map<String, Object>>> sections) {
        try (Workbook workbook = new XSSFWorkbook()) {

            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            for (Map.Entry<String, List<Map<String, Object>>> section : sections.entrySet()) {
                String sectionName = sanitizeSheetName(section.getKey());
                List<Map<String, Object>> rows = section.getValue();

                if (rows.isEmpty()) {
                    continue;
                }

                Sheet sheet = workbook.createSheet(sectionName);

                // Get columns (maintain consistent order)
                List<String> columns = new ArrayList<>(rows.get(0).keySet());
                Collections.sort(columns);

                // Create header row
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < columns.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns.get(i));
                    cell.setCellStyle(headerStyle);
                }

                // Create data rows
                for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
                    Map<String, Object> rowData = rows.get(rowIdx);
                    Row dataRow = sheet.createRow(rowIdx + 1);

                    for (int colIdx = 0; colIdx < columns.size(); colIdx++) {
                        String columnName = columns.get(colIdx);
                        Object value = rowData.get(columnName);

                        Cell cell = dataRow.createCell(colIdx);
                        setCellValue(cell, value, dateStyle);
                    }
                }

                // Auto-size columns
                for (int i = 0; i < columns.size(); i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            // Write to byte array
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                workbook.write(baos);
                return baos.toByteArray();
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel export", e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd"));
        return style;
    }

    private void setCellValue(Cell cell, Object value, CellStyle dateStyle) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number num) {
            cell.setCellValue(num.doubleValue());
        } else if (value instanceof Boolean bool) {
            cell.setCellValue(bool);
        } else if (value instanceof LocalDate date) {
            cell.setCellValue(date.toString());
            cell.setCellStyle(dateStyle);
        } else if (value instanceof LocalDateTime dateTime) {
            cell.setCellValue(dateTime.toString());
            cell.setCellStyle(dateStyle);
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private String sanitizeSheetName(String name) {
        // Excel sheet names must be <= 31 chars and cannot contain: \ / ? * [ ]
        String sanitized = name.replaceAll("[\\\\/?\\*\\[\\]]", "_");
        if (sanitized.length() > 31) {
            sanitized = sanitized.substring(0, 31);
        }
        return sanitized;
    }
}
