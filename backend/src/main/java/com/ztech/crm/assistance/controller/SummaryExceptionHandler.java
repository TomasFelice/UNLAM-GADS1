package com.ztech.crm.assistance.controller;

import com.ztech.crm.assistance.service.SummaryFailure;
import com.ztech.crm.shared.config.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@Order(-1)
@RestControllerAdvice(assignableTypes=CommercialSummaryController.class)
public class SummaryExceptionHandler {
    @ExceptionHandler(SummaryFailure.class)
    public ResponseEntity<ProblemDetail> failure(SummaryFailure ex,HttpServletRequest request) {
        var body=ProblemDetail.forStatusAndDetail(ex.getStatus(),ex.getMessage());
        body.setTitle(ex.getStatus().getReasonPhrase());body.setProperty("code",ex.getCode());
        body.setProperty("errors",List.of());body.setProperty("requestId",request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
        var response=ResponseEntity.status(ex.getStatus());
        if(ex.retryAfterSeconds()!=null) {
            body.setProperty("retryAfterSeconds",ex.retryAfterSeconds());response.header("Retry-After",ex.retryAfterSeconds().toString());
        }
        return response.body(body);
    }
}
