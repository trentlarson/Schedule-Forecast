package com.trentlarson.forecast.core.web.page;

import java.sql.*;

import com.trentlarson.forecast.core.helper.ForecastUtil;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;

import org.apache.wicket.model.CompoundPropertyModel;

public class TaskPage extends WebPage {
  
  public TaskPage() {
    Form<TaskBean> newForm = new Form<TaskBean>("editForm", new CompoundPropertyModel(new TaskBean())) {
      @Override
      public void onSubmit() {
        try {
	  Connection conn = ForecastUtil.getConnection();
	  PreparedStatement stat = conn.prepareStatement("update issue set summary = ? where id = ?");
	  stat.executeUpdate();
	} catch (SQLException e) {
	   throw new IllegalStateException();
	}
      }
    };
    add(newForm);
  }
  
    public class TaskBean {
	private long id;
	private String summary;

	public long getId() {
	    return id;
	}
	public void setId(long id_) {
	    this.id = id_;
	}

	public String getSummary() {
	    return summary;
	}
	public void setSummary(String summary_) {
	    this.summary = summary_;
	}
    }

}
