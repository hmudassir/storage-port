package dev.portfolio.storage;

public interface ObjectStorage {

	void put(String key, byte[] body);

	byte[] get(String key);

	int reads();
}
