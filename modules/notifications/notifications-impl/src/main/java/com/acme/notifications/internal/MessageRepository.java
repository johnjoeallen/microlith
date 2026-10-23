package com.acme.notifications.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface MessageRepository extends JpaRepository<MessageEntity, String> {

    Optional<MessageEntity> findByRequestId(String requestId);
}
