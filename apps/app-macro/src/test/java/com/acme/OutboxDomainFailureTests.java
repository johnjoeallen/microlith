package com.acme;

/** The failure tests with follow-ups delivered by the outbox relay ({@code modules.orders.async=outbox}). */
class OutboxDomainFailureTests extends DomainFailureTests {

    @Override
    String asyncMode() {
        return "outbox";
    }

    @Override
    String notifyStatusSql() {
        return "select status from outbox where request_id = ?";
    }

    @Override
    String deliveredStatus() {
        return "DONE";
    }

    @Override
    String undeliveredNotifySql() {
        return "select count(*) from outbox where status = 'PENDING' and request_id = ?";
    }

    @Override
    String notifyArgument(String requestId) {
        return requestId + ":notify";
    }
}
