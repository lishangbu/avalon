package io.github.lishangbu.avalon.app;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.StringReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class SseTestEventTest {

	@ParameterizedTest
	@ValueSource(strings = { "event:ready\ndata:ready\n\n", "event: ready\ndata: ready\n\n",
			"data: ready\nevent: ready\n\n", ": heartbeat\n\nevent:ready\n: comment\ndata:ready\n\n",
			"event: ignored\n\nevent: ready\nid: 1\nretry: 1000\nunknown: value\ndata: ready\n\n",
			"event: ready\r\ndata: ready\r\n\r\n" })
	void readsEquivalentEventRepresentations(String stream) throws Exception {
		assertEquals(new SseTestEvent("ready", "ready"), SseTestEvent.read(reader(stream)));
	}

	@Test
	void preservesPayloadWhitespaceAndJoinsDataLines() throws Exception {
		assertEquals(new SseTestEvent("message", " ready \n\tchanged\n"),
				SseTestEvent.read(reader("event:\ndata:  ready \ndata:\tchanged\ndata\n\n")));
	}

	@Test
	void consumesOnlyOneEventIncludingItsDelimiter() throws Exception {
		var reader = reader("event:ready\ndata:ready\n\nevent:changed\ndata:changed\n\n");
		assertEquals(new SseTestEvent("ready", "ready"), SseTestEvent.read(reader));
		assertEquals(new SseTestEvent("changed", "changed"), SseTestEvent.read(reader));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", ": heartbeat\n\n", "event: ready\ndata: ready\n" })
	void rejectsEndOfStreamWithoutACompleteEvent(String stream) {
		assertThrows(EOFException.class, () -> SseTestEvent.read(reader(stream)));
	}

	private BufferedReader reader(String stream) {
		return new BufferedReader(new StringReader(stream));
	}

}
