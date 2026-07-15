package uk.gov.hmcts.opal.common.operationid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;

@RestController
public class OperationIdResponseFilterController {

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/no-content")
    public ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/service-error")
    public void serviceUnavailable() {
        throw new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable");
    }
}
