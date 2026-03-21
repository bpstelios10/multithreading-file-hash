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
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class RequestsCounterFilter extends OncePerRequestFilter {

    @Getter
    private final AtomicLong totalRequestsReceived = new AtomicLong();
    // this getter might give back stale value. if we want 100% correct results we need either add a lock inside
    // the getter, or use volatile ( the lock flashes to main memory when it finishes. volatile says
    // always read from main memory. so no chance for inconsistency or race conditions! )
    @Getter
    private long publicRequests = 0;
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                 @NonNull FilterChain filterChain) throws ServletException, IOException {
        long currentRequestIndex = totalRequestsReceived.incrementAndGet();
        log.debug("Current request number: [{}]", currentRequestIndex);

        String path = request.getRequestURI();

        lock.lock();
        try {
            if (!path.startsWith("/file-hashes/private/")) {
                publicRequests++;
            }
        } finally {
            lock.unlock();
        }

        filterChain.doFilter(request, response);
    }
}
