package uk.gov.hmcts.opal.common.operationid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/illegal-argument-error")
    public void illegalArgument() {
        throw new IllegalArgumentException("BOOM");
    }

    @GetMapping("/secure")
    public ResponseEntity<Void> secure() {
        return ResponseEntity.ok().build();
    }
}
