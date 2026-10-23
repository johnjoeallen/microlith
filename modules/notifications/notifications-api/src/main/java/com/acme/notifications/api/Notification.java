package com.acme.notifications.api;

public record Notification(String notificationId, String customerId, String email, String template) {
}
