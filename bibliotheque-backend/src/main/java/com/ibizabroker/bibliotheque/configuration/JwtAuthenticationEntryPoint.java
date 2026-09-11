package com.ibizabroker.bibliotheque.configuration;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        boolean expired = Boolean.TRUE.equals(request.getAttribute(JwtRequestFilter.EXPIRED_TOKEN_ATTRIBUTE));
        String message = expired
                ? "Votre session a expire. Veuillez vous reconnecter."
                : "Vous devez etre connecte(e) pour effectuer cette action.";

        log.warn("Acces refuse (401) sur {} {} : {}", request.getMethod(), request.getRequestURI(),
                expired ? "token expire" : "token absent ou invalide");

        // Ecrit directement le corps de la reponse plutot que response.sendError(),
        // pour garantir que le vrai message atteigne le frontend (BasicErrorController
        // masque le message des exceptions par defaut).
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.getWriter().write(message);
    }

}
