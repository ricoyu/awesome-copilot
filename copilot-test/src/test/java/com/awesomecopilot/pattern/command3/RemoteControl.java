package com.awesomecopilot.pattern.command3;

//Invoker
public class RemoteControl {
	
	private Command slot;

	public void setCommand(Command command) {
		this.slot = command;
	}

	public void pressButton() {
		slot.execute();
	}
}