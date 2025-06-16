package com.awesomecopilot.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class Product {
	@JsonProperty("product_id")
	private String productId;
	private String name;
	private String description;
	private double price;
	private int stock;
	private List<String> categories;
	private List<String> tags;
	@JsonProperty("created_at")
	private LocalDateTime createdAt;
	@JsonProperty("updated_at")
	private LocalDateTime updatedAt;
	private double rating;
	private List<Feature> features;
	private String location;
	@JsonProperty("is_active")
	private boolean isActive;
	private Dimensions dimensions;
	private List<Variant> variants;

	@Data
	public static class Feature {
		private String name;
		private String value;
	}

	@Data
	public static class Dimensions {
		private double length;
		private double width;
		private double height;
		private double weight;
	}

	@Data
	public static class Variant {
		private String color;
		private String size;
		private double price;
	}
}
