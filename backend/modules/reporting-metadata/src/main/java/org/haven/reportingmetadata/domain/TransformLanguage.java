package org.haven.reportingmetadata.domain;

/**
 * Transformation expression language types
 */
public enum TransformLanguage {
    /**
     * No transformation - direct field mapping
     */
    NONE,

    /**
     * SQL fragment (compatible with PostgreSQL)
     * Used in SELECT clauses for query-based exports
     */
    SQL,

    /**
     * Java Expression Language (Spring EL)
     * For in-memory transformations
     */
    JAVA_EL
}
