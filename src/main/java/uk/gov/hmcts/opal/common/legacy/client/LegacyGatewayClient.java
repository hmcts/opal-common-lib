package uk.gov.hmcts.opal.common.legacy.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

public interface LegacyGatewayClient {

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    ResponseEntity<String> post(@RequestParam("actionType") String actionType,
                                @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                @RequestBody Object request);
}
