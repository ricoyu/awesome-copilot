package com.awesomecopilot.pattern.builder3.builders;

import com.awesomecopilot.pattern.builder3.cars.CarType;
import com.awesomecopilot.pattern.builder3.components.Engine;
import com.awesomecopilot.pattern.builder3.components.GPSNavigator;
import com.awesomecopilot.pattern.builder3.components.Transmission;
import com.awesomecopilot.pattern.builder3.components.TripComputer;

/**
 * Builder interface defines all possible ways to configure a product.
 */
public interface Builder {
    
    void setCarType(CarType type);
    
    void setSeats(int seats);
    
    void setEngine(Engine engine);
    
    void setTransmission(Transmission transmission);
    
    void setTripComputer(TripComputer tripComputer);
    
    void setGPSNavigator(GPSNavigator gpsNavigator);
}
