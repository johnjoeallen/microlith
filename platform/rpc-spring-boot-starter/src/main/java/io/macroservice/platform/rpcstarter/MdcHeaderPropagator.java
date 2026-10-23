package io.macroservice.platform.rpcstarter;

import java.util.Map;
import org.slf4j.MDC;

/** Propagates the correlation id and W3C traceparent that the server side placed in the logging MDC. */
public class MdcHeaderPropagator implements RpcHeaderPropagator {

    public static final String CORRELATION_KEY = "correlationId";
    public static final String TRACEPARENT_KEY = "traceparent";

    @Override
    public void contribute(Map<String, String> headers) {
        String correlation = MDC.get(CORRELATION_KEY);
        if (correlation != null) {
            headers.put(RpcController.CORRELATION_HEADER, correlation);
        }
        String traceparent = MDC.get(TRACEPARENT_KEY);
        if (traceparent != null) {
            headers.put(RpcController.TRACEPARENT_HEADER, traceparent);
        }
    }
}
