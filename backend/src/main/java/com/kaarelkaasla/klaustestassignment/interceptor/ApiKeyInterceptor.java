package com.kaarelkaasla.klaustestassignment.interceptor;

import io.grpc.*;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Interceptor to check for a valid API key in gRPC calls. Ensures that only requests with the correct API key are
 * processed.
 */
@Component
@GrpcGlobalServerInterceptor
public class ApiKeyInterceptor implements ServerInterceptor {

    /**
     * The header key to retrieve the API key from.
     */
    @Value("${api.key-header}")
    private String apiKeyHeader;

    /**
     * The valid API key retrieved from the application properties.
     */
    @Value("${api.key}")
    private String validApiKey;

    /**
     * Intercepts incoming gRPC calls to check for the presence and validity of an API key.
     *
     * @param call
     *            the server call
     * @param headers
     *            the call headers
     * @param next
     *            the next server call handler
     *
     * @return a listener for server call events
     */
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        String apiKey = headers.get(Metadata.Key.of(apiKeyHeader, Metadata.ASCII_STRING_MARSHALLER));

        if (validApiKey.equals(apiKey)) {
            return next.startCall(call, headers);
        } else {
            call.close(Status.PERMISSION_DENIED.withDescription("Invalid API key"), new Metadata());
            return new ServerCall.Listener<>() {
            };
        }
    }
}
