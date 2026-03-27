package com.pruefstein.todo.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Todo
/* extends PanacheEntity */ {

	private String task;
	private Date completed;

	// Mocking of existing data, this would normally be in your DB and go via
	// Hibernate/Panache
	private static final List<Todo> all = new ArrayList<>();

	public static List<Todo> listAll()
	{
		return all;
	}

	public void persist()
	{
		all.add(this);
	}

	public String getTask()
	{
		return task;
	}

	public void setTask(String task)
	{
		this.task = task;
	}

	public Date getCompleted()
	{
		return completed;
	}

	public void setCompleted(Date completed)
	{
		this.completed = completed;
	}
}
