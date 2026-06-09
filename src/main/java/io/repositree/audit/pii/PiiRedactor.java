package io.repositree.audit.pii;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PiiRedactor {

    // RFC-5322 simplified
    private static final Pattern EMAIL = Pattern.compile(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");

    // E.164 / common formats: +91-9876543210, +1-555-867-5309, 07700900077
    private static final Pattern PHONE = Pattern.compile(
            "(?:\\+\\d{1,3}[\\s\\-]?)?(?:\\(?\\d{1,4}\\)?[\\s\\-]?)?\\d{3,4}[\\s\\-]?\\d{4}");

    // Luhn-passing 13–19 digit card numbers (spaces/dashes tolerated)
    private static final Pattern CARD = Pattern.compile(
            "\\b(?:\\d[ \\-]?){13,19}\\b");

    // Indian Aadhaar: 4 4 4 digit groups
    private static final Pattern AADHAAR = Pattern.compile(
            "\\b\\d{4}[\\s]\\d{4}[\\s]\\d{4}\\b");

    public String redact(String text) {
        if (text == null) return null;
        String result = AADHAAR.matcher(text).replaceAll("[AADHAAR]");
        result = EMAIL.matcher(result).replaceAll("[EMAIL]");
        result = PHONE.matcher(result).replaceAll("[PHONE]");
        result = CARD.matcher(result).replaceAll("[CARD]");
        return result;
    }

    public boolean containsPii(String text) {
        if (text == null) return false;
        return EMAIL.matcher(text).find()
                || PHONE.matcher(text).find()
                || CARD.matcher(text).find()
                || AADHAAR.matcher(text).find();
    }
}
