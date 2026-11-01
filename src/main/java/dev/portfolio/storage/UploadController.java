package dev.portfolio.storage;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
public class UploadController {

	private final UploadService uploads;

	public UploadController(UploadService uploads) {
		this.uploads = uploads;
	}

	@PostMapping("/api/uploads")
	public ResponseEntity<UploadService.CreatedUpload> create(@Valid @RequestBody CreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(uploads.create(request.filename(), request.byteSize()));
	}

	@PutMapping(value = "/api/uploads/{id}/content", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity<Void> content(
			@PathVariable Long id,
			@RequestHeader("X-Upload-Token") String token,
			@RequestBody byte[] body) {
		uploads.store(id, token, body);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/api/uploads/{id}/complete")
	public UploadService.UploadView complete(@PathVariable Long id) {
		return uploads.complete(id);
	}

	public record CreateRequest(
			@NotBlank @Size(max = 200) String filename,
			@Min(1) @Max(1_000_000) long byteSize) {
	}
}
