package gm.engine.xml.model;

import gm.engine.exception.InvalidEventDataException;

public enum CommissionType {
    ON_PURCHASE,
    ON_CLOSE;

    public static CommissionType fromXmlValue(String xmlValue) {

        if (xmlValue == null) {
            throw new InvalidEventDataException("Error: commission type is missing.");
        }

        String normalized = xmlValue.trim();

        if (normalized.equalsIgnoreCase("on-purchase")) {
            return ON_PURCHASE;
        }
        if (normalized.equalsIgnoreCase("on-close")) {
            return ON_CLOSE;
        }

        throw new InvalidEventDataException(String.format(
                "Error: unknown commission type \"%s\". Expected \"on-purchase\" or \"on-close\".", xmlValue));
    }
}