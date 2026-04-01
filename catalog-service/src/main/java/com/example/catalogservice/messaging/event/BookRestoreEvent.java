package com.example.catalogservice.messaging.event;

import java.util.Map;

public record BookRestoreEvent(
        Long loanId,
        Map<Long, Integer> bookQuantities
) {}