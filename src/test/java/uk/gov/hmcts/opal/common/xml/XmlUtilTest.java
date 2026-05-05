package uk.gov.hmcts.opal.common.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XmlUtilTest {

    @Test
    void marshalAndUnmarshalRoundTrip() throws Exception {
        TestEnvelope original = new TestEnvelope(1L, "NT");

        String xml = XmlUtil.marshalXmlString(original, TestEnvelope.class);
        TestEnvelope roundTrip = XmlUtil.unmarshalXmlString(xml, TestEnvelope.class);

        assertEquals(original, roundTrip);
    }

    @Test
    void validateXmlString_throwsForSchemaMismatch() {
        String schema = """
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
              <xs:element name="testEntity">
                <xs:complexType>
                  <xs:sequence>
                    <xs:element name="requiredField" type="xs:string"/>
                  </xs:sequence>
                </xs:complexType>
              </xs:element>
            </xs:schema>
            """;

        String xml = """
            <testEntity>
              <missingField>bad</missingField>
            </testEntity>
            """;

        assertThrows(RuntimeException.class, () -> XmlUtil.validateXmlString(schema, xml));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlRootElement(name = "testEntity")
    @XmlAccessorType(XmlAccessType.FIELD)
    static class TestEnvelope {

        @XmlElement(name = "testId")
        private Long testId;

        @XmlElement(name = "testType")
        private String testType;
    }
}
