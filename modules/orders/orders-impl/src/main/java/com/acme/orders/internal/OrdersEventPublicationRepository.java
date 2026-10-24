package com.acme.orders.internal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import io.macroservice.platform.support.ModuleComponent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.core.EventPublicationRepository;
import org.springframework.modulith.events.core.EventSerializer;
import org.springframework.modulith.events.core.PublicationTargetIdentifier;
import org.springframework.modulith.events.core.TargetEventPublication;

/**
 * Spring Modulith's event publication store, kept in the {@code event_publication} table of the orders schema.
 *
 * <p>Modulith ships a JDBC repository, but it is package-private, wants one primary {@code DataSource} and opens a connection at
 * start-up to detect the database. Here every module has its own pool that never connects at start-up and its own schema and
 * roles, so this is a small repository over the orders pool with the same table layout (Modulith schema v2) and the same
 * statements. It uses the pool that the orders transaction manager uses, so a publication is written in the same transaction as
 * the order change that caused it. Completion mode UPDATE only (completed rows are kept, not deleted or archived).
 */
@ModuleComponent(module = "orders")
@ConditionalOnProperty(name = "modules.orders.async", havingValue = "events")
public class OrdersEventPublicationRepository implements EventPublicationRepository {

    private final JdbcTemplate jdbc;
    private final EventSerializer serializer;

    OrdersEventPublicationRepository(@Qualifier("ordersDataSource") DataSource dataSource, EventSerializer serializer) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.serializer = serializer;
    }

    @Override
    public TargetEventPublication create(TargetEventPublication publication) {
        jdbc.update("insert into event_publication (id, event_type, listener_id, publication_date, serialized_event, status, "
                        + "completion_attempts, last_resubmission_date) values (?, ?, ?, ?, ?, ?, ?, ?)",
                publication.getIdentifier(), publication.getEvent().getClass().getName(), publication.getTargetIdentifier().getValue(),
                Timestamp.from(publication.getPublicationDate()), serialize(publication.getEvent()), EventPublication.Status.PUBLISHED.name(),
                0, null);
        return publication;
    }

    @Override
    public void markProcessing(UUID identifier) {
        jdbc.update("update event_publication set status = 'PROCESSING', completion_attempts = coalesce(completion_attempts, 0) + 1 "
                + "where id = ? and completion_date is null", identifier);
    }

    @Override
    public void markCompleted(UUID identifier, Instant completionDate) {
        jdbc.update("update event_publication set status = 'COMPLETED', completion_date = ? where id = ?", Timestamp.from(completionDate), identifier);
    }

    @Override
    public void markCompleted(Object event, PublicationTargetIdentifier identifier, Instant completionDate) {
        jdbc.update("update event_publication set status = 'COMPLETED', completion_date = ? "
                + "where listener_id = ? and completion_date is null and serialized_event = ?",
                Timestamp.from(completionDate), identifier.getValue(), serialize(event));
    }

    @Override
    public void markFailed(UUID identifier) {
        jdbc.update("update event_publication set status = 'FAILED' where id = ? and completion_date is null", identifier);
    }

    /**
     * Claims an incomplete publication for resubmission; false if another instance claimed it first. A publication claimed
     * (RESUBMITTED) more than two minutes ago is claimable again: the instance that claimed it may have died.
     */
    @Override
    public boolean markResubmitted(UUID identifier, Instant resubmissionDate) {
        return jdbc.update("update event_publication set status = 'RESUBMITTED', last_resubmission_date = ? where id = ? and completion_date is null "
                        + "and (status is null or status <> 'RESUBMITTED' or last_resubmission_date < ?)",
                Timestamp.from(resubmissionDate), identifier, Timestamp.from(resubmissionDate.minusSeconds(120))) == 1;
    }

    @Override
    public List<TargetEventPublication> findIncompletePublications() {
        return query("select * from event_publication where completion_date is null order by publication_date");
    }

    @Override
    public List<TargetEventPublication> findIncompletePublicationsPublishedBefore(Instant instant) {
        return query("select * from event_publication where completion_date is null and publication_date < ? order by publication_date",
                Timestamp.from(instant));
    }

    @Override
    public Optional<TargetEventPublication> findIncompletePublicationsByEventAndTargetIdentifier(Object event, PublicationTargetIdentifier identifier) {
        return query("select * from event_publication where serialized_event = ? and listener_id = ? "
                + "and (completion_date is null or status = 'FAILED') order by publication_date", serialize(event), identifier.getValue())
                .stream().findFirst();
    }

    @Override
    public List<TargetEventPublication> findCompletedPublications() {
        return query("select * from event_publication where completion_date is not null order by publication_date");
    }

    @Override
    public List<TargetEventPublication> findFailedPublications(FailedCriteria criteria) {
        return query("select * from event_publication where status = 'FAILED' and publication_date < ? order by publication_date limit ?",
                Timestamp.from(criteria.getPublicationDateReference()), criteria.getMaxItemsToRead());
    }

    @Override
    public List<TargetEventPublication> findByStatus(EventPublication.Status status) {
        return query("select * from event_publication where status = ? order by publication_date", status.name());
    }

    @Override
    public int countByStatus(EventPublication.Status status) {
        Integer count = jdbc.queryForObject("select count(*) from event_publication where status = ?", Integer.class, status.name());
        return count == null ? 0 : count;
    }

    @Override
    public void deletePublications(List<UUID> identifiers) {
        identifiers.forEach(id -> jdbc.update("delete from event_publication where id = ?", id));
    }

    @Override
    public void deleteCompletedPublications() {
        jdbc.update("delete from event_publication where completion_date is not null");
    }

    @Override
    public void deleteCompletedPublicationsBefore(Instant instant) {
        jdbc.update("delete from event_publication where completion_date < ?", Timestamp.from(instant));
    }

    private String serialize(Object event) {
        return serializer.serialize(event).toString();
    }

    private List<TargetEventPublication> query(String sql, Object... arguments) {
        return jdbc.query(sql, (rs, row) -> publication(rs), arguments);
    }

    private TargetEventPublication publication(ResultSet rs) throws SQLException {
        try {
            Object event = serializer.deserialize(rs.getString("serialized_event"), Class.forName(rs.getString("event_type")));
            Timestamp completed = rs.getTimestamp("completion_date");
            Timestamp resubmitted = rs.getTimestamp("last_resubmission_date");
            String status = rs.getString("status");
            return new Stored(rs.getObject("id", UUID.class), event, PublicationTargetIdentifier.of(rs.getString("listener_id")),
                    rs.getTimestamp("publication_date").toInstant(), completed == null ? null : completed.toInstant(),
                    status == null ? (completed == null ? EventPublication.Status.PUBLISHED : EventPublication.Status.COMPLETED)
                            : EventPublication.Status.valueOf(status),
                    resubmitted == null ? null : resubmitted.toInstant(), rs.getInt("completion_attempts"));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unknown event type " + e.getMessage(), e);
        }
    }

    /** A publication read back from the table. */
    private static final class Stored implements TargetEventPublication {

        private final UUID identifier;
        private final Object event;
        private final PublicationTargetIdentifier target;
        private final Instant publicationDate;
        private Instant completionDate;
        private EventPublication.Status status;
        private final Instant lastResubmissionDate;
        private final int completionAttempts;

        Stored(UUID identifier, Object event, PublicationTargetIdentifier target, Instant publicationDate, Instant completionDate,
                EventPublication.Status status, Instant lastResubmissionDate, int completionAttempts) {
            this.identifier = identifier;
            this.event = event;
            this.target = target;
            this.publicationDate = publicationDate;
            this.completionDate = completionDate;
            this.status = status;
            this.lastResubmissionDate = lastResubmissionDate;
            this.completionAttempts = completionAttempts;
        }

        @Override
        public UUID getIdentifier() {
            return identifier;
        }

        @Override
        public Object getEvent() {
            return event;
        }

        @Override
        public PublicationTargetIdentifier getTargetIdentifier() {
            return target;
        }

        @Override
        public Instant getPublicationDate() {
            return publicationDate;
        }

        @Override
        public Optional<Instant> getCompletionDate() {
            return Optional.ofNullable(completionDate);
        }

        @Override
        public void markCompleted(Instant instant) {
            this.completionDate = instant;
            this.status = EventPublication.Status.COMPLETED;
        }

        @Override
        public EventPublication.Status getStatus() {
            return status;
        }

        @Override
        public Instant getLastResubmissionDate() {
            return lastResubmissionDate;
        }

        @Override
        public int getCompletionAttempts() {
            return completionAttempts;
        }
    }
}
