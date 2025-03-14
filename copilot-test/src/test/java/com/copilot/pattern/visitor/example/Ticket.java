package com.copilot.pattern.visitor.example;

import java.util.Date;

public abstract class Ticket {
	protected Date issueDate;
	protected Double penalty;
	protected String ticketNo;

	public abstract void accept(Vistor vistor);
}