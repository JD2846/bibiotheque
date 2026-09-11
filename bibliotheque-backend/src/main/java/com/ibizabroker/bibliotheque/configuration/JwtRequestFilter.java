package com.ibizabroker.bibliotheque.configuration;

import com.ibizabroker.bibliotheque.service.JwtService;
import com.ibizabroker.bibliotheque.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtRequestFilter.class);

    /**
     * Nom de l'attribut de requete pose quand le token JWT est expire, afin que
     * JwtAuthenticationEntryPoint puisse distinguer ce cas d'un token simplement
     * absent/invalide et renvoyer un message different.
     */
    public static final String EXPIRED_TOKEN_ATTRIBUTE = "com.ibizabroker.bibliotheque.EXPIRED_JWT";

    /**
     * Exempter les paths Swagger / OpenAPI du filtre JWT
     * pour éviter le 401 sur les endpoints publics de documentation.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/api/swagger-ui")
                || path.startsWith("/api/v3/api-docs");
    }

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtService jwtService;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");

        String username = null;
        String jwtToken = null;

        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                username = jwtUtil.getUsernameFromToken(jwtToken);
            } catch (IllegalArgumentException e) {
                log.warn("Tentative d'acces refusee sur {} : token JWT illisible", request.getRequestURI());
            } catch (ExpiredJwtException e) {
                log.warn("Tentative d'acces refusee sur {} : token JWT expire pour l'utilisateur '{}'",
                        request.getRequestURI(), e.getClaims().getSubject());
                request.setAttribute(EXPIRED_TOKEN_ATTRIBUTE, Boolean.TRUE);
            }
        } else {
            log.debug("Requete sans token Bearer sur {}", request.getRequestURI());
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = jwtService.loadUserByUsername(username);

            if (jwtUtil.validateToken(jwtToken, userDetails)) {

                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            }
        }
        filterChain.doFilter(request, response);

    }

}
