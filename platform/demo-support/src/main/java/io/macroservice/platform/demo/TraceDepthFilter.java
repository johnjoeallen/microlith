package io.macroservice.platform.demo;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/** Continues the caller's depth for the duration of an incoming {@code /rpc} request. */
class TraceDepthFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/rpc/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        int depth = 0;
        String header = request.getHeader(TraceHeaderPropagator.DEPTH_HEADER);
        if (header != null) {
            try {
                depth = Math.max(0, Integer.parseInt(header.trim()));
            } catch (NumberFormatException ignored) {
                // an unusable depth just restarts the tree
            }
        }
        CallContext.depth(depth);
        try {
            chain.doFilter(request, response);
        } finally {
            CallContext.depth(0);
        }
    }
}
