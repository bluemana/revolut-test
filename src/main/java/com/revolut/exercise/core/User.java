package com.revolut.exercise.core;

public class User {

	private final int id;
	private final String name;
	private final int balance;
	
	public User(int id, String name, int balance) {
		this.id = id;
		this.name = name;
		this.balance = balance;
	}
	
	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public int getBalance() {
		return balance;
	}
}
