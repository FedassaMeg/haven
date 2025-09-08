package org.haven.clientprofile.infrastructure.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.haven.clientprofile.domain.Client;
import org.postgresql.util.PGobject;

@Converter(autoApply = false)
public class AdministrativeGenderConverter implements AttributeConverter<Client.AdministrativeGender, Object> {

    private String toDatabaseValue(Client.AdministrativeGender attribute) {
        if (attribute == null) return null;
        return switch (attribute) {
            case MALE -> "MALE";
            case FEMALE -> "FEMALE";
            case OTHER -> "OTHER";
            case UNKNOWN -> "PREFER_NOT_TO_SAY"; // Map UNKNOWN to existing DB enum
        };
    }

    @Override
    public Object convertToDatabaseColumn(Client.AdministrativeGender attribute) {
        String value = toDatabaseValue(attribute);
        if (value == null) return null;
        try {
            PGobject pg = new PGobject();
            pg.setType("gender");
            pg.setValue(value);
            return pg;
        } catch (Exception e) {
            // Fallback to plain string if PGobject cannot be instantiated
            return value;
        }
    }

    @Override
    public Client.AdministrativeGender convertToEntityAttribute(Object dbData) {
        if (dbData == null) return Client.AdministrativeGender.UNKNOWN;
        String value;
        if (dbData instanceof PGobject pg) {
            value = pg.getValue();
        } else {
            value = dbData.toString();
        }

        return switch (value) {
            case "MALE" -> Client.AdministrativeGender.MALE;
            case "FEMALE" -> Client.AdministrativeGender.FEMALE;
            case "OTHER" -> Client.AdministrativeGender.OTHER;
            case "NON_BINARY" -> Client.AdministrativeGender.OTHER;
            case "TRANSGENDER" -> Client.AdministrativeGender.OTHER;
            case "PREFER_NOT_TO_SAY" -> Client.AdministrativeGender.UNKNOWN;
            default -> Client.AdministrativeGender.OTHER;
        };
    }
}
