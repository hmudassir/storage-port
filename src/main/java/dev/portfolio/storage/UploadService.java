package dev.portfolio.storage;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;

import dev.portfolio.storage.InMemoryObjectStorage.MissingObjectException;

@Service
public class UploadService {

	private final UploadRepository uploads;

	private final ObjectStorage storage;

	private final SecureRandom random = new SecureRandom();

	public UploadService(UploadRepository uploads, ObjectStorage storage) {
		this.uploads = uploads;
		this.storage = storage;
	}

	@Transactional
	public CreatedUpload create(String filename, long byteSize) {
		byte[] raw = new byte[32];
		random.nextBytes(raw);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
		Upload upload = uploads.save(new Upload(filename.trim(), byteSize, sha256(token)));
		upload.assignKeys();
		return new CreatedUpload(upload.getId(), token);
	}

	@Transactional
	public void store(Long id, String token, byte[] body) {
		Upload upload = uploads.findById(id).orElseThrow(UploadService::missing);
		if (upload.getStatus() != Upload.Status.PENDING) {
			throw rejected("Upload is no longer pending");
		}
		if (!sha256(token).equals(upload.getTokenHash())) {
			throw rejected("Upload token is wrong");
		}
		storage.put(upload.getStagingKey(), body);
	}

	@Transactional(noRollbackFor = UploadRejectedException.class)
	public UploadView complete(Long id) {
		Upload upload = uploads.findById(id).orElseThrow(UploadService::missing);
		if (upload.getStatus() == Upload.Status.READY) {
			return UploadView.from(upload);
		}
		if (upload.getStatus() == Upload.Status.FAILED) {
			throw rejected("Upload already failed");
		}
		byte[] body;
		try {
			body = storage.get(upload.getStagingKey());
		}
		catch (MissingObjectException exception) {
			upload.setStatus(Upload.Status.FAILED);
			throw rejected("Upload bytes are missing");
		}
		if (body.length != upload.getByteSize()) {
			upload.setStatus(Upload.Status.FAILED);
			throw rejected("Upload size does not match");
		}
		storage.put(upload.getPrivateKey(), body);
		upload.setStatus(Upload.Status.READY);
		return UploadView.from(upload);
	}

	private static String sha256(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (java.security.NoSuchAlgorithmException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private static ErrorResponseException missing() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Upload was not found");
		problem.setTitle("Not Found");
		return new ErrorResponseException(HttpStatus.NOT_FOUND, problem, null);
	}

	private static UploadRejectedException rejected(String message) {
		return new UploadRejectedException(message);
	}

	public record CreatedUpload(Long id, String token) {
	}

	public record UploadView(Long id, String status, String privateKey) {

		static UploadView from(Upload upload) {
			return new UploadView(upload.getId(), upload.getStatus().name(), upload.getPrivateKey());
		}
	}
}
