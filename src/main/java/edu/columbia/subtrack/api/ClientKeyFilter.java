package edu.columbia.subtrack.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Rejects requests without a non-blank X-Client-Key (SPEC section 2) before anything else runs. */
@Component
public class ClientKeyFilter extends OncePerRequestFilter {

    public static final String CLIENT_KEY = "clientKey";

    private final ObjectMapper json;

    public ClientKeyFilter(ObjectMapper json) {
        this.json = json;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String key = request.getHeader("X-Client-Key");
        if (key == null || key.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            json.writeValue(response.getOutputStream(),
                    new ErrorResponse("UNAUTHENTICATED", "X-Client-Key header is missing or blank", null));
            return;
        }
        request.setAttribute(CLIENT_KEY, key);
        chain.doFilter(request, response);
    }
}
