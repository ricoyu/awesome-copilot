package com.awesomecopilot.pattern.command2;

/**
 * There would be a different type of jobs that can be executed. The following are
 * the different concrete classes whose instances will be executed by the different
 * command objects.
 * 
 * @author Rico Yu
 * @since Aug 24, 2016
 * @version
 *
 */
public class Email {

	public void sendEmail() {
		System.out.println("Sending email.......");
	}
}