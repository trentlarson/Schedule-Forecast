package com.icentris.jira.dao;

import java.util.Date;

public class TeamHours {

  public TeamHours() {}

  private Long id;
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  private Date created;
  public Date getCreated() { return created; }
  public void setCreated(Date created) { this.created = created; }

  private Date updated;
  public Date getUpdated() { return updated; }
  public void setUpdated(Date updated) { this.updated = updated; }

  private Long teamId;
  public Long getTeamId() { return teamId; }
  public void setTeamId(Long teamId) { this.teamId = teamId; }

  private String username;
  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  private Date startOfWeek;
  public Date getStartOfWeek() { return startOfWeek; }
  public void setStartOfWeek(Date startOfWeek) { this.startOfWeek = startOfWeek; }

  private Double hoursAvailable;
  public Double getHoursAvailable() { return hoursAvailable; }
  public void setHoursAvailable(Double hoursAvailable) { this.hoursAvailable = hoursAvailable; }

  public TeamHours(Long id_, Long teamId_, String username_, Date startOfWeek_,
                   Double hoursAvailable_) {
    this.id = id_;
    this.teamId = teamId_;
    this.username = username_;
    this.startOfWeek = startOfWeek_;
    this.hoursAvailable = hoursAvailable_;
  }

}
