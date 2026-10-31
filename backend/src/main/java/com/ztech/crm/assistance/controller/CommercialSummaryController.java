package com.ztech.crm.assistance.controller;

import com.ztech.crm.assistance.dto.CommercialSummaryResponse;
import com.ztech.crm.assistance.service.CommercialSummaryService;
import com.ztech.crm.assistance.service.SummaryFailure;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/opportunities")
@Tag(name="Asistencia comercial")
public class CommercialSummaryController {
    private final CommercialSummaryService service;
    public CommercialSummaryController(CommercialSummaryService service){this.service=service;}
    @PostMapping("/{id}/commercial-summary")
    @Operation(summary="Generar resumen comercial efímero",description="Sin body ni parámetros. Contexto autorizado, hasta 50 actividades y 50 transiciones, cobertura y fuentes. No escribe negocio.")
    @ApiResponses({@ApiResponse(responseCode="200",description="Resumen con fuentes y cobertura"),
            @ApiResponse(responseCode="400",description="Entrada no admitida",content=@Content(schema=@Schema(implementation=org.springframework.http.ProblemDetail.class))),@ApiResponse(responseCode="404",description="Oportunidad no visible",content=@Content(schema=@Schema(implementation=org.springframework.http.ProblemDetail.class))),
            @ApiResponse(responseCode="409",description="AI_CONTEXT_CHANGED",content=@Content(schema=@Schema(implementation=org.springframework.http.ProblemDetail.class))),@ApiResponse(responseCode="422",description="AI_CONTENT_BLOCKED",content=@Content(schema=@Schema(implementation=org.springframework.http.ProblemDetail.class))),
            @ApiResponse(responseCode="429",description="AI_RATE_LIMITED o AI_QUOTA_EXCEEDED; retryAfterSeconds si se conoce",content=@Content(schema=@Schema(implementation=org.springframework.http.ProblemDetail.class))),
            @ApiResponse(responseCode="502",description="AI_INVALID_RESPONSE",content=@Content(schema=@Schema(implementation=org.springframework.http.ProblemDetail.class))),@ApiResponse(responseCode="503",description="AI_NOT_CONFIGURED o AI_UNAVAILABLE",content=@Content(schema=@Schema(implementation=org.springframework.http.ProblemDetail.class))),
            @ApiResponse(responseCode="504",description="AI_TIMEOUT",content=@Content(schema=@Schema(implementation=org.springframework.http.ProblemDetail.class)))})
    public CommercialSummaryResponse generate(@PathVariable Long id,HttpServletRequest request) throws java.io.IOException {
        if(id<=0||request.getInputStream().read()!=-1||request.getQueryString()!=null)
            throw new SummaryFailure(HttpStatus.BAD_REQUEST,"INVALID_REQUEST_BODY","El resumen sólo admite el ID de la oportunidad, sin cuerpo ni parámetros.");
        return service.generate(id);
    }
}
