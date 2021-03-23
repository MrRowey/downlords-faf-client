package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Type("league")
@AllArgsConstructor
@NoArgsConstructor
public class League {
  @Id
  private String id;
  private OffsetDateTime createTime;
  private OffsetDateTime updateTime;
  private String descriptionKey;
  private String nameKey;
  private String technicalName;
  private String currentSeasonId;
}
