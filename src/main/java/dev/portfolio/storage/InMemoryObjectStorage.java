package dev.portfolio.storage;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

@Component
public class InMemoryObjectStorage implements ObjectStorage {

	private final ConcurrentHashMap<String, byte[]> objects = new ConcurrentHashMap<>();

	private final AtomicInteger reads = new AtomicInteger();

	@Override
	public void put(String key, byte[] body) {
		objects.put(key, Arrays.copyOf(body, body.length));
	}

	@Override
	public byte[] get(String key) {
		reads.incrementAndGet();
		byte[] body = objects.get(key);
		if (body == null) {
			throw new MissingObjectException(key);
		}
		return Arrays.copyOf(body, body.length);
	}

	@Override
	public int reads() {
		return reads.get();
	}

	public static class MissingObjectException extends RuntimeException {

		public MissingObjectException(String key) {
			super(key);
		}
	}
}
