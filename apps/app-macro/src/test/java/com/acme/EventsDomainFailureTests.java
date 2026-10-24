package com.acme;

/** The failure tests with follow-ups delivered as Spring Modulith events ({@code modules.orders.async=events}). */
class EventsDomainFailureTests extends DomainFailureTests {

    @Override
    String asyncMode() {
        return "events";
    }

    @Override
    String notifyStatusSql() {
        return "select status from event_publication where serialized_event like ?";
    }

    @Override
    String deliveredStatus() {
        return "COMPLETED";
    }

    @Override
    String undeliveredNotifySql() {
        return "select count(*) from event_publication where completion_date is null and serialized_event like ?";
    }

    @Override
    String notifyArgument(String requestId) {
        return "%\"requestId\":\"" + requestId + ":notify\"%";
    }
}
