package io.repositree.audit.pii;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PiiRedactorTest {

    private final PiiRedactor redactor = new PiiRedactor();

    @Test
    void redactsEmailAddress() {
        String input = "User nikhil@repositree.io signed in from 192.168.1.1";
        String result = redactor.redact(input);
        assertThat(result).doesNotContain("nikhil@repositree.io");
        assertThat(result).contains("[EMAIL]");
    }

    @Test
    void redactsPhoneNumber() {
        String input = "Contact: +91-9876543210 for support";
        String result = redactor.redact(input);
        assertThat(result).doesNotContain("9876543210");
        assertThat(result).contains("[PHONE]");
    }

    @Test
    void redactsCreditCard() {
        String input = "Payment with card 4111111111111111 approved";
        String result = redactor.redact(input);
        assertThat(result).doesNotContain("4111111111111111");
        assertThat(result).contains("[CARD]");
    }

    @Test
    void redactsIndianAadhaar() {
        String input = "Aadhaar 1234 5678 9012 verified";
        String result = redactor.redact(input);
        assertThat(result).doesNotContain("1234 5678 9012");
        assertThat(result).contains("[AADHAAR]");
    }

    @Test
    void preservesNonPiiText() {
        String input = "Agent completed task: summarize-lease-doc in 1234ms";
        String result = redactor.redact(input);
        assertThat(result).isEqualTo(input);
    }

    @Test
    void redactsMultiplePiiInSameString() {
        String input = "Owner john.doe@example.com called at +1-555-867-5309";
        String result = redactor.redact(input);
        assertThat(result).doesNotContain("john.doe@example.com");
        assertThat(result).doesNotContain("555-867-5309");
        assertThat(result).contains("[EMAIL]");
        assertThat(result).contains("[PHONE]");
    }

    @ParameterizedTest
    @CsvSource({
        "user@example.com,true",
        "not-an-email,false",
        "+1-800-555-0100,true",
        "hello world,false"
    })
    void containsPii_detectsCorrectly(String input, boolean expectsPii) {
        assertThat(redactor.containsPii(input)).isEqualTo(expectsPii);
    }
}
