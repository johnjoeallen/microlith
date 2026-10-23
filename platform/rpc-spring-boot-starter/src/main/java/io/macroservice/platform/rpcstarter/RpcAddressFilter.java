package io.macroservice.platform.rpcstarter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import org.springframework.web.filter.OncePerRequestFilter;

/** Restricts {@code /rpc/**} to requests that arrive on one local address (see {@code rpc.server.address}). */
class RpcAddressFilter extends OncePerRequestFilter {

    private final String allowedLocalAddress;

    RpcAddressFilter(String address) throws IOException {
        this.allowedLocalAddress = InetAddress.getByName(address).getHostAddress();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.equals("/rpc") || path.startsWith("/rpc/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (allowedLocalAddress.equals(request.getLocalAddr())) {
            chain.doFilter(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
