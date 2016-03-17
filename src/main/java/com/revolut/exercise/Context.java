package com.revolut.exercise;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.revolut.exercise.core.User;

public enum Context {

	INSTANCE;
	
	private static final Logger LOGGER = Logger.getLogger(Context.class);
	
	private final ExecutorService executor;
	private final Map<Integer, User> users;
	private int userId;
	
	private Context() {
		executor = Executors.newSingleThreadExecutor();
		users = new HashMap<Integer, User>();
		userId = 0;
	}
	
	public Collection<User> getUsers() {
		Collection<User> result = Collections.emptyList();
		try {
			result = executor.submit(new Callable<Collection<User>>() {

				@Override
				public Collection<User> call() throws Exception {
					return new ArrayList<User>(users.values());
				}
			}).get();
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.error(e.getMessage(), e);
		}
		return result;
	}
	
	public User createUser(String name, int balance) {
		User result = null;
		try {
			result = executor.submit(new Callable<User>() {

				@Override
				public User call() throws Exception {
					User user = new User(++userId, name, balance);
					users.put(user.getId(), user);
					return user;
				}
			}).get();
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.error(e.getMessage(), e);
		}
		return result;
	}
}
