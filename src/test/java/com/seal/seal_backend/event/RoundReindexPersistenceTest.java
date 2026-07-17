package com.seal.seal_backend.event;

import com.seal.seal_backend.domain.entity.Discipline;
import com.seal.seal_backend.domain.entity.Event;
import com.seal.seal_backend.domain.entity.Round;
import com.seal.seal_backend.domain.entity.TermPlan;
import com.seal.seal_backend.domain.entity.User;
import com.seal.seal_backend.domain.enums.EventType;
import com.seal.seal_backend.domain.enums.TermType;
import com.seal.seal_backend.domain.repository.RoundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Persistence-layer test for the round-ordering reindex against a REAL H2 schema that carries the
 * uq_rounds_event_order unique constraint (event_id, order_number). Unlike the mocked service test,
 * this actually hits the DB constraint, so the "insert before shift" flush-order bug is caught here.
 *
 * replace = NONE keeps the configured H2 datasource (MODE=MySQL) instead of a plain embedded one.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RoundReindexPersistenceTest {

    @Autowired RoundRepository roundRepository;
    @Autowired TestEntityManager em;

    private Long eventId;

    @BeforeEach
    void seed() {
        User owner = new User();
        owner.setEmail("owner+" + System.nanoTime() + "@seal.local");
        owner.setPasswordHash("x");
        owner.setFullName("Owner");
        em.persist(owner);

        Discipline discipline = new Discipline();
        discipline.setCode("SE" + System.nanoTime() % 100000);
        discipline.setName("Software Engineering");
        em.persist(discipline);

        TermPlan termPlan = new TermPlan();
        termPlan.setTerm(TermType.FALL);
        termPlan.setYear(2024);
        termPlan.setDiscipline(discipline);
        em.persist(termPlan);

        Event event = new Event();
        event.setName("SEAL Fall 2024");
        event.setSlug("seal-fall-2024-" + System.nanoTime());
        event.setEventType(EventType.FALL);
        event.setDiscipline(discipline);
        event.setTermPlan(termPlan);
        event.setOwnerCoordinator(owner);
        event.setCreatedBy(owner);
        em.persist(event);

        // Existing rounds: [1 Prelim (non-final), 2 Final] — the exact failing setup.
        persistRound(event, "Prelim", 1, false);
        persistRound(event, "Final", 2, true);
        em.flush();
        em.clear();

        eventId = event.getId();
    }

    private Round persistRound(Event event, String name, int order, boolean isFinal) {
        Round r = new Round();
        r.setEvent(event);
        r.setName(name);
        r.setOrderNumber(order);
        r.setIsFinalRound(isFinal);
        em.persist(r);
        return r;
    }

    /**
     * Reproduces the original bug directly at the DB layer: inserting a new round at the occupied
     * order 2 WITHOUT first making room collides on uq_rounds_event_order. This is why the fix must
     * shift before inserting; it also proves the H2 test schema really enforces the constraint.
     */
    @Test
    void naiveInsertAtOccupiedOrder_violatesUniqueConstraint() {
        Event event = em.getEntityManager().getReference(Event.class, eventId);
        Round clash = new Round();
        clash.setEvent(event);
        clash.setName("Semifinal");
        clash.setOrderNumber(2); // already taken by the Final round
        clash.setIsFinalRound(false);

        assertThatThrownBy(() -> {
            em.persist(clash);
            em.flush();
        }).isInstanceOf(Exception.class);
    }

    /**
     * The fix path: shift the block up FIRST (flushed bulk DML), THEN insert at the freed slot.
     * Result: [1 Prelim, 2 Semifinal, 3 Final] — the final ends up last (highest order), no collision.
     */
    @Test
    void shiftUpThenInsert_succeeds_finalEndsUpLast() {
        roundRepository.shiftOrdersUp(eventId, 2);

        Event event = em.getEntityManager().getReference(Event.class, eventId);
        Round inserted = new Round();
        inserted.setEvent(event);
        inserted.setName("Semifinal");
        inserted.setOrderNumber(2);
        inserted.setIsFinalRound(false);
        roundRepository.saveAndFlush(inserted);

        List<Round> rounds = roundRepository.findByEventIdOrderByOrderNumber(eventId);
        assertThat(rounds).extracting(Round::getName)
                .containsExactly("Prelim", "Semifinal", "Final");
        assertThat(rounds).extracting(Round::getOrderNumber)
                .containsExactly(1, 2, 3); // contiguous
        Round last = rounds.get(rounds.size() - 1);
        assertThat(last.getIsFinalRound()).isTrue(); // the final is last (highest order)
    }

    /**
     * Deleting a middle round then shifting down keeps the order numbers contiguous with no collision.
     */
    @Test
    void deleteMiddleRound_thenShiftDown_keepsContiguous() {
        // Start from [1 Prelim, 2 Final] → insert Semifinal at 2 → [1 Prelim, 2 Semifinal, 3 Final].
        roundRepository.shiftOrdersUp(eventId, 2);
        Event event = em.getEntityManager().getReference(Event.class, eventId);
        Round mid = new Round();
        mid.setEvent(event);
        mid.setName("Semifinal");
        mid.setOrderNumber(2);
        mid.setIsFinalRound(false);
        roundRepository.saveAndFlush(mid);

        Long midId = mid.getId();
        roundRepository.deleteById(midId);
        roundRepository.shiftOrdersDown(eventId, 2); // close the gap left at order 2

        List<Round> rounds = roundRepository.findByEventIdOrderByOrderNumber(eventId);
        assertThat(rounds).extracting(Round::getName)
                .containsExactly("Prelim", "Final");
        assertThat(rounds).extracting(Round::getOrderNumber)
                .containsExactly(1, 2); // contiguous, no collision
    }
}
