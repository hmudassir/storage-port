package dev.portfolio.storage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "uploads")
public class Upload {

	public enum Status {
		PENDING,
		READY,
		FAILED
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String filename;

	@Column(name = "byte_size", nullable = false)
	private long byteSize;

	@Column(name = "token_hash", nullable = false, length = 64)
	private String tokenHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status;

	@Column(name = "staging_key", nullable = false, length = 200)
	private String stagingKey;

	@Column(name = "private_key", nullable = false, length = 200)
	private String privateKey;

	protected Upload() {
	}

	public Upload(String filename, long byteSize, String tokenHash) {
		this.filename = filename;
		this.byteSize = byteSize;
		this.tokenHash = tokenHash;
		this.status = Status.PENDING;
		this.stagingKey = "pending";
		this.privateKey = "pending";
	}

	public void assignKeys() {
		this.stagingKey = "staging/" + id;
		this.privateKey = "private/" + id;
	}

	public Long getId() {
		return id;
	}

	public String getFilename() {
		return filename;
	}

	public long getByteSize() {
		return byteSize;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public Status getStatus() {
		return status;
	}

	public String getStagingKey() {
		return stagingKey;
	}

	public String getPrivateKey() {
		return privateKey;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
}
