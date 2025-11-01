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
}
