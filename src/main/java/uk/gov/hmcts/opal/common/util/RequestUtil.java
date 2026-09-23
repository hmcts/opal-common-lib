package uk.gov.hmcts.opal.common.util;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;

public class RequestUtil {

    public static boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/");
    }
}
