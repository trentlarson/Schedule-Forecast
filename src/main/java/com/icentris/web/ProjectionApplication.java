package com.icentris.web;

import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;
import com.icentris.web.page.TaskPage;

public class ProjectionApplication extends WebApplication {

  public Class<? extends Page> getHomePage() {
    return TaskPage.class;
  }

}
