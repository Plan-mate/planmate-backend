package com.planmate.planmate_backend.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(JwtEntryPoint.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private record ErrorResponse(String error, String message) {}

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Throwable cause = authException.getCause();
        ErrorResponse errorResponse = cause instanceof ExpiredJwtException
                ? new ErrorResponse("TOKEN_EXPIRED", "엑세스 토큰이 만료되었습니다.")
                : new ErrorResponse("UNAUTHORIZED", "인증에 실패했습니다.");

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
