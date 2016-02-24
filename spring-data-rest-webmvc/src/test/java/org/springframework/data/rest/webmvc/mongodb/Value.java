package org.springframework.data.rest.webmvc.mongodb;

public class Value {
	private String text;

	public Value() {
	}

	public Value(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}