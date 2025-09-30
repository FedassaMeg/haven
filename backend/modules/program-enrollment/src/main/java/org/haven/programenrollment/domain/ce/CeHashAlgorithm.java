package org.haven.programenrollment.domain.ce;

/**
 * Cryptographic hash algorithms supported for CE packet identity construction.
 */
public enum CeHashAlgorithm {
    SHA256_SALT,
    BCRYPT;

    public boolean isAdaptive() {
        return this == BCRYPT;
    }
}
