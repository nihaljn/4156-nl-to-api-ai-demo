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

/** Rejects any request without a non-blank X-Client-Key header. */
@Component
public class ClientKeyFilter extends OncePerRequestFilter {

    /** The header that identifies the calling client. */
    public static final String HEADER = "X-Client-Key";

    private final ObjectMapper mapper;

    /**
     * Creates the filter.
     *
     * @param mapper writes the error body
     */
    public ClientKeyFilter(final ObjectMapper mapper) {
        super();
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            final FilterChain chain) throws ServletException, IOException {
        final String key = request.getHeader(HEADER);
        if (key == null || key.isBlank()) {
            response.setStatus(ErrorCode.UNAUTHENTICATED.httpStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), new ErrorBody(ErrorCode.UNAUTHENTICATED.name(),
                HEADER + " header is required", null));
        } else {
            chain.doFilter(request, response);
        }
    }
}
