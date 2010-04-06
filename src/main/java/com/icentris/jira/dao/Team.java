package com.icentris.jira.dao;

import java.util.Date;

public class Team {

  public Team() {}

  private Long id;
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  private Date created;
  public Date getCreated() { return created; }
  public void setCreated(Date created) { this.created = created; }

  private Date updated;
  public Date getUpdated() { return updated; }
  public void setUpdated(Date updated) { this.updated = updated; }

  private String name;
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  private Long projectId;
  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }

}
