package uk.gov.hmcts.opal.common.operationid;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import uk.gov.hmcts.opal.common.logging.LogUtil;

@ControllerAdvice
@Slf4j(topic = "opal.OperationIdHeaderAdvice")
public class OperationIdHeaderAdvice implements ResponseBodyAdvice<Object> {

    private static final String OPERATION_ID_HEADER = "operation_id";

    @Override
    public boolean supports(@NonNull MethodParameter returnType,
        @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public @Nullable Object beforeBodyWrite(@Nullable Object body, @NonNull MethodParameter returnType,
        @NonNull MediaType selectedContentType, @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
        @NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response) {
        String operationId = LogUtil.getOrCreateOpalOperationId();

        log.debug(":OperationIdHeaderAdvice: {}={}", OPERATION_ID_HEADER, operationId);
        response.getHeaders().add(OPERATION_ID_HEADER, operationId);

        return body;
    }
}
