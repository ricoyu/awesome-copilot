package com.awesomecopilot.pattern.visitor.example;

/**
 * 违规停车的罚单
 * @author Rico Yu
 * @since Jul 5, 2016
 * @version 
 *
 */
public class FppTicket extends Ticket {
	private String block;
	private String building;
	private String street;

	@Override
	public void accept(Vistor v) {
        v.visit(this);
	}
}