package com.destore.finance.client;

import com.destore.finance.dto.EnablingRequest;
import com.destore.finance.dto.EnablingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "finance-approval-automation", fallback = ExternalFinanceClientFallback.class)
public interface ExternalFinanceClient {

    @PostMapping("/api/finance-approval/approve")
    EnablingResponse approveRequest(@RequestBody EnablingRequest request);
}
