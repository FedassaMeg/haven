package org.haven.reporting.domain;

/**
 * Supported export formats for HMIS data
 */
public enum ExportFormat {
    CSV("CSV", "text/csv", ".csv"),
    XML("XML", "application/xml", ".xml"),
    EXCEL("Excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx");

    private final String displayName;
    private final String mimeType;
    private final String fileExtension;

    ExportFormat(String displayName, String mimeType, String fileExtension) {
        this.displayName = displayName;
        this.mimeType = mimeType;
        this.fileExtension = fileExtension;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public boolean isCsv() {
        return this == CSV;
    }

    public boolean isXml() {
        return this == XML;
    }

    public boolean isExcel() {
        return this == EXCEL;
    }
}
