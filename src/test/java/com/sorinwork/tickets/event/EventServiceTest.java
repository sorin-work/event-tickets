package com.sorinwork.tickets.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.common.NotFoundException;
import com.sorinwork.tickets.support.IntegrationTest;
import com.sorinwork.tickets.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EventServiceTest extends IntegrationTest {

  @Autowired EventService service;
  @Autowired EventRepository events;

  @Test
  void createsDraftEventWithSlug() {
    Event event = service.create(TestData.eventForm("Summer Jazz Night"));

    assertThat(event.getId()).isNotNull();
    assertThat(event.getSlug()).isEqualTo("summer-jazz-night");
    assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
  }

  @Test
  void addsSuffixWhenSlugIsTaken() {
    service.create(TestData.eventForm("Board Games"));
    service.create(TestData.eventForm("Board Games"));
    Event third = service.create(TestData.eventForm("Board games!"));

    assertThat(third.getSlug()).isEqualTo("board-games-3");
  }

  @Test
  void nonLatinTitlesStillGetWorkingDistinctUrls() {
    Event first = service.create(TestData.eventForm("Концерт"));
    Event second = service.create(TestData.eventForm("Вечер джаза"));

    assertThat(first.getSlug()).isEqualTo("event");
    assertThat(second.getSlug()).isEqualTo("event-2");
  }

  @Test
  void anEventCalledPastDoesNotHideThePastEventsPage() {
    Event event = service.create(TestData.eventForm("Past"));

    assertThat(event.getSlug()).isEqualTo("past-2");
    assertThatThrownBy(() -> service.changeSlug(event.getId(), "past"))
        .hasMessageContaining("used by the site");
  }

  @Test
  void updateKeepsTheSlug() {
    Event event = service.create(TestData.eventForm("Board Games"));
    EventForm changed = TestData.eventForm("Board Games Marathon");
    changed.setDescription("   ");

    Event updated = service.update(event.getId(), changed);

    assertThat(updated.getTitle()).isEqualTo("Board Games Marathon");
    assertThat(updated.getSlug()).isEqualTo("board-games");
    assertThat(updated.getDescription()).isNull();
  }

  @Test
  void getUnknownEventThrows() {
    assertThatThrownBy(() -> service.get(999)).isInstanceOf(NotFoundException.class);
  }

  @Test
  void publishesAndUnpublishes() {
    Event event = service.create(TestData.eventForm("Open Mic"));
    data.tier(event, "Entry", "5", 50);

    assertThat(service.publish(event.getId()).getStatus()).isEqualTo(EventStatus.PUBLISHED);
    assertThat(service.unpublish(event.getId()).getStatus()).isEqualTo(EventStatus.DRAFT);
  }

  @Test
  void cannotPublishCancelledEvent() {
    Event event = service.create(TestData.eventForm("Open Mic"));
    service.cancel(event.getId());

    assertThatThrownBy(() -> service.publish(event.getId())).isInstanceOf(BusinessException.class);
  }

  @Test
  void cannotUnpublishDraft() {
    Event event = service.create(TestData.eventForm("Open Mic"));

    assertThatThrownBy(() -> service.unpublish(event.getId()))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void changesTheSlug() {
    Event event = service.create(TestData.eventForm("Summer Jazz Night"));

    assertThat(service.changeSlug(event.getId(), " Jazz-2030 ").getSlug()).isEqualTo("jazz-2030");
    assertThat(events.findBySlug("jazz-2030")).isPresent();
  }

  @Test
  void rejectsBadOrTakenSlugs() {
    Event event = service.create(TestData.eventForm("Summer Jazz Night"));
    service.create(TestData.eventForm("Taken"));

    assertThatThrownBy(() -> service.changeSlug(event.getId(), "no spaces"))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> service.changeSlug(event.getId(), "--"))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> service.changeSlug(event.getId(), "taken"))
        .hasMessageContaining("already uses /events/taken");
  }

  @Test
  void cannotPublishAnEventThatAlreadyStarted() {
    Event event = service.create(TestData.eventForm("Yesterday"));
    data.tier(event, "Entry", "5", 50);
    clock.set(java.time.Instant.parse("2030-03-15T10:00:00Z"));

    assertThatThrownBy(() -> service.publish(event.getId()))
        .hasMessageContaining("already started");
  }

  @Test
  void cannotPublishWithoutTiers() {
    Event event = service.create(TestData.eventForm("Open Mic"));

    assertThatThrownBy(() -> service.publish(event.getId()))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("ticket tier");
  }
}
