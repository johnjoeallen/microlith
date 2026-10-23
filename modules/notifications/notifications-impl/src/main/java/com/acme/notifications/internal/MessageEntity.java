package com.acme.notifications.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "message")
class MessageEntity {

    @Id
    @Column(name = "notification_id")
    private String notificationId;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "email")
    private String email;

    @Column(name = "template")
    private String template;

    @Column(name = "params")
    private String params;

    protected MessageEntity() {
    }

    MessageEntity(String notificationId, String requestId, String customerId, String email, String template, String params) {
        this.notificationId = notificationId;
        this.requestId = requestId;
        this.customerId = customerId;
        this.email = email;
        this.template = template;
        this.params = params;
    }

    String getNotificationId() {
        return notificationId;
    }

    void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    String getRequestId() {
        return requestId;
    }

    void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    String getCustomerId() {
        return customerId;
    }

    void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    String getEmail() {
        return email;
    }

    void setEmail(String email) {
        this.email = email;
    }

    String getTemplate() {
        return template;
    }

    void setTemplate(String template) {
        this.template = template;
    }

    String getParams() {
        return params;
    }

    void setParams(String params) {
        this.params = params;
    }
}
