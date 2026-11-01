package dev.portfolio.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class StoragePortTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectStorage storage;

	@Test
	void completeReadsStagingOnceAndASecondCompleteDoesNot() throws Exception {
		int readsBefore = storage.reads();
		Created created = create(5);
		mvc.perform(put("/api/uploads/" + created.id() + "/content")
				.header("X-Upload-Token", created.token())
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.content("hello".getBytes()))
				.andExpect(status().isNoContent());

		mvc.perform(post("/api/uploads/" + created.id() + "/complete"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("READY"))
				.andExpect(jsonPath("$.privateKey").value("private/" + created.id()));

		int afterFirst = storage.reads();
		assertThat(afterFirst - readsBefore).isEqualTo(1);

		mvc.perform(post("/api/uploads/" + created.id() + "/complete"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("READY"));
		assertThat(storage.reads()).isEqualTo(afterFirst);
		assertThat(storage.get("private/" + created.id())).isEqualTo("hello".getBytes());
	}

	@Test
	void aSizeMismatchFailsAndALaterCompleteDoesNotReadAgain() throws Exception {
		int readsBefore = storage.reads();
		Created created = create(5);
		mvc.perform(put("/api/uploads/" + created.id() + "/content")
				.header("X-Upload-Token", created.token())
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.content("hi".getBytes()))
				.andExpect(status().isNoContent());

		mvc.perform(post("/api/uploads/" + created.id() + "/complete"))
				.andExpect(status().isBadRequest());
		int afterFailure = storage.reads();
		assertThat(afterFailure - readsBefore).isEqualTo(1);

		mvc.perform(post("/api/uploads/" + created.id() + "/complete"))
				.andExpect(status().isBadRequest());
		assertThat(storage.reads()).isEqualTo(afterFailure);
	}

	private Created create(int byteSize) throws Exception {
		MvcResult created = mvc.perform(post("/api/uploads")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"filename\":\"note.txt\",\"byteSize\":" + byteSize + "}"))
				.andExpect(status().isCreated())
				.andReturn();
		String body = created.getResponse().getContentAsString();
		Number id = JsonPath.read(body, "$.id");
		String token = JsonPath.read(body, "$.token");
		return new Created(id.longValue(), token);
	}

	private record Created(long id, String token) {
	}
}
