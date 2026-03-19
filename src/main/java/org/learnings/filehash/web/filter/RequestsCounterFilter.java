package org.learnings.filehash.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class RequestsCounterFilter extends OncePerRequestFilter {

    @Getter
    private final AtomicLong totalRequestsReceived = new AtomicLong();

    @Override
    public void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                 FilterChain filterChain) throws ServletException, IOException {
        long currentRequestIndex = totalRequestsReceived.incrementAndGet();
        log.debug("Current request number: [{}]", currentRequestIndex);

        filterChain.doFilter(request, response);
    }
}
