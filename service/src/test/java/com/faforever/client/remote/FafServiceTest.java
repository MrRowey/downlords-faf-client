package com.faforever.client.remote;

import com.faforever.client.api.ApiDtoMapper;
import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.avatar.Avatar;
import com.faforever.client.avatar.event.AvatarChangedEvent;
import com.faforever.client.news.NewsItem;
import com.faforever.client.news.NewsTag;
import com.faforever.client.review.Review;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.supcomhub.api.dto.Account;
import org.supcomhub.api.dto.GameReview;
import org.supcomhub.api.dto.MapVersionReview;
import org.supcomhub.api.dto.ModVersionReview;
import org.supcomhub.api.dto.NewsPost;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FafServiceTest {

  private FafService instance;
  @Mock
  private FafServerAccessor fafServerAccessor;
  @Mock
  private EventBus eventBus;
  @Mock
  private FafApiAccessor fafApiAccessor;
  @Mock
  private ApiDtoMapper apiDtoMapper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    instance = new FafService(fafServerAccessor, fafApiAccessor, eventBus, apiDtoMapper);
  }

  @Test
  public void selectAvatar() throws Exception {
    URL url = new URL("http://example.com");
    instance.selectAvatar(new Avatar(1, url, "Description"));

    ArgumentCaptor<AvatarChangedEvent> eventCaptor = ArgumentCaptor.forClass(AvatarChangedEvent.class);
    verify(eventBus).post(eventCaptor.capture());

    Avatar avatar = eventCaptor.getValue().getAvatar();
    assertThat(avatar, not(nullValue()));
    assertThat(avatar.getUrl(), is(url));
    assertThat(avatar.getDescription(), is("Description"));

    verify(fafServerAccessor).selectAvatar(1);
  }

  @Test
  public void createGameReview() throws Exception {
    Review review = createReview(null, "something", 3, 42);

    when(fafApiAccessor.createGameReview(any()))
      .thenReturn((GameReview) new GameReview().setReviewer(account()).setScore((short) 1).setId("1"));

    instance.saveGameReview(review, 5);
    verify(fafApiAccessor).createGameReview(any());
  }

  @Test
  public void createMapVersionReview() throws Exception {
    Review review = createReview(null, "something", 3, 42);

    when(fafApiAccessor.createMapVersionReview(any()))
      .thenReturn((MapVersionReview) new MapVersionReview().setReviewer(account()).setScore((short) 1).setId("1"));

    instance.saveMapVersionReview(review, "5");
    verify(fafApiAccessor).createMapVersionReview(any());
  }

  @Test
  public void createModVersion() throws Exception {
    Review review = createReview(null, "something", 3, 42);

    when(fafApiAccessor.createModVersionReview(any()))
      .thenReturn((ModVersionReview) new ModVersionReview().setReviewer(account()).setScore((short) 1).setId("1"));

    instance.saveModVersionReview(review, "5");
    verify(fafApiAccessor).createModVersionReview(any());
  }

  @Test
  public void getNews() throws ExecutionException, InterruptedException {
    NewsPost newsPost = new NewsPost();
    when(fafApiAccessor.getNews()).thenReturn(Collections.singletonList(newsPost));

    NewsItem newsItem = new NewsItem("", "", "", "", OffsetDateTime.now(), Collections.singletonList(NewsTag.UNCATEGORIZED));
    when(apiDtoMapper.map(newsPost)).thenReturn(newsItem);

    CompletableFuture<List<NewsItem>> news = instance.getNews();

    verify(fafApiAccessor).getNews();
    verify(apiDtoMapper).map(newsPost);

    assertThat(news.get().get(0), is(newsItem));
  }

  private Review createReview(String id, String text, int rating, Integer playerId) {
    Review review = new Review();
    review.setId(id);
    review.setText(text);
    review.setScore(rating);
    Optional.ofNullable(playerId)
      .map(String::valueOf)
      .ifPresent(s -> {
        com.faforever.client.player.Player player = new com.faforever.client.player.Player(s);
        player.setId(playerId);
        review.setReviewer(player);
      });

    return review;
  }

  private Account account() {
    return (Account) new Account().setId("1");
  }
}
