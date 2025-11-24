package com.destore.finance.client;

import com.destore.finance.dto.EnablingRequest;
import com.destore.finance.dto.EnablingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExternalFinanceClientFallback implements ExternalFinanceClient {

    @Override
    public EnablingResponse approveRequest(EnablingRequest request) {
        log.error("External finance service is unavailable. Returning fallback response for request: {}",
                request.getRequestId());

        return EnablingResponse.builder()
                .requestId(request.getRequestId())
                .approved(false)
                .reason("External finance service is currently unavailable. Please try again later.")
                .build();
    }
}
