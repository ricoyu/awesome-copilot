package com.awesomecopilot.jackson.inherit;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class Vehicle {
    
	private String make;
	private String model;
	
}
