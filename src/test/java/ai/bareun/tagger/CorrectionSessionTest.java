package ai.bareun.tagger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.google.protobuf.MessageLite;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import ai.bareun.client.BareunClient;
import ai.bareun.client.BareunException;
import ai.bareun.client.ConnectCode;
import ai.bareun.protos.CloseSessionResponse;
import ai.bareun.protos.CorrectErrorRequest;
import ai.bareun.protos.CorrectErrorResponse;
import ai.bareun.protos.GetSessionEventsRequest;
import ai.bareun.protos.GetSessionEventsResponse;
import ai.bareun.protos.OpenSessionRequest;
import ai.bareun.protos.OpenSessionResponse;
import ai.bareun.protos.SessionEvent;
import ai.bareun.protos.SessionEventKind;

/**
 * {@link CorrectionSession}·{@link Corrector#openSession()} 단위 테스트.
 * 경로별로 응답을 정하는 가짜 서버를 쓴다.
 */
class CorrectionSessionTest {

    private HttpServer server;
    /** 서버가 받은 요청 본문. 경로별로 쌓는다. */
    private final Map<String, List<byte[]>> received = new ConcurrentHashMap<>();

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private static String path(String method) {
        return "/bareun.RevisionService/" + method;
    }

    /**
     * 경로마다 요청 본문으로 응답을 만드는 함수를 달아 가짜 서버를 띄운다.
     * 함수가 없는 경로나 함수가 null 을 돌려주면 NOT_FOUND 오류 JSON 을 보낸다.
     */
    private BareunClient start(Map<String, Function<byte[], MessageLite>> routes) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            byte[] in = ex.getRequestBody().readAllBytes();
            String p = ex.getRequestURI().getPath();
            received.computeIfAbsent(p, k -> new ArrayList<>()).add(in);
            var fn = routes.get(p);
            MessageLite out = fn == null ? null : fn.apply(in);
            byte[] body;
            int status;
            if (out == null) {
                status = 404;
                body = "{\"code\":\"not_found\",\"message\":\"SESSION_NOT_FOUND: 모르는 세션\"}"
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                ex.getResponseHeaders().add("Content-Type", "application/json");
            } else {
                status = 200;
                body = out.toByteArray();
                ex.getResponseHeaders().add("Content-Type", "application/proto");
            }
            ex.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        return BareunClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("koba-TEST").build();
    }

    private static MessageLite opened() {
        return OpenSessionResponse.newBuilder().setSessionId("sess-1").setSessionRevision(0)
                .setIdleTimeoutSeconds(600).setMaxLifetimeSeconds(3600).build();
    }

    @Test
    void 세션을_열고_나눠_교정하고_닫는다() throws Exception {
        var client = start(Map.of(
                path("OpenSession"), in -> opened(),
                path("CorrectError"), in -> {
                    try {
                        var req = CorrectErrorRequest.parseFrom(in);
                        return CorrectErrorResponse.newBuilder()
                                .setRevised(req.getDocument().getContent() + "!")
                                .setSessionRevision(req.getDocument().getContent().length()).build();
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                },
                path("CloseSession"), in -> CloseSessionResponse.newBuilder().setSessionRevision(9).build()));

        try (CorrectionSession s = new Corrector(client, List.of("mydict")).openSession()) {
            assertEquals("sess-1", s.id());
            assertEquals(600, s.idleTimeoutSeconds());
            assertEquals(3600, s.maxLifetimeSeconds());
            assertEquals(0, s.revision());
            assertEquals("가나!", s.correct("가나"));
            assertEquals(2, s.revision());
        }

        // 열 때 사용자 사전을 실었고, 교정 요청에는 세션 ID 를 실었다.
        var open = OpenSessionRequest.parseFrom(received.get(path("OpenSession")).get(0));
        assertEquals(List.of("mydict"), open.getCustomDictNamesList());
        assertEquals("sess-1", CorrectErrorRequest
                .parseFrom(received.get(path("CorrectError")).get(0)).getSessionId());
        assertEquals(1, received.get(path("CloseSession")).size());
    }

    @Test
    void 개정_번호가_없는_응답은_기존_값을_유지한다() throws Exception {
        var client = start(Map.of(
                path("OpenSession"), in -> opened(),
                path("CorrectError"), in -> CorrectErrorResponse.newBuilder().setRevised("x").build(),
                path("CloseSession"), in -> CloseSessionResponse.getDefaultInstance()));
        try (CorrectionSession s = new Corrector(client).openSession()) {
            s.correct("글");
            assertEquals(0, s.revision());
        }
    }

    @Test
    void 사건을_가져오고_개정_번호를_갱신한다() throws Exception {
        var client = start(Map.of(
                path("OpenSession"), in -> opened(),
                path("GetSessionEvents"), in -> GetSessionEventsResponse.newBuilder()
                        .setSessionRevision(2)
                        .addEvents(SessionEvent.newBuilder().setRevision(2)
                                .setKind(SessionEventKind.SESSION_EVENT_KIND_WORD_CONFIRMED)
                                .setWord("가나다")).build(),
                path("CloseSession"), in -> CloseSessionResponse.getDefaultInstance()));
        try (CorrectionSession s = new Corrector(client).openSession()) {
            var events = s.events(1);
            assertEquals(1, events.size());
            assertEquals("가나다", events.get(0).getWord());
            assertEquals(2, s.revision());
        }
        var req = GetSessionEventsRequest.parseFrom(received.get(path("GetSessionEvents")).get(0));
        assertEquals("sess-1", req.getSessionId());
        assertEquals(1, req.getSinceRevision());
    }

    @Test
    void 닫기는_한_번만_부르고_실패해도_던지지_않는다() throws Exception {
        // CloseSession 경로가 없으니 서버가 오류를 돌려준다.
        var client = start(Map.of(path("OpenSession"), in -> opened()));
        CorrectionSession s = new Corrector(client).openSession();
        s.close();
        s.close();
        assertEquals(1, received.get(path("CloseSession")).size());
    }

    @Test
    void 사라진_세션은_isLost_로_알아본다() throws Exception {
        var client = start(Map.of(
                path("OpenSession"), in -> opened(),
                path("CloseSession"), in -> CloseSessionResponse.getDefaultInstance()));
        try (CorrectionSession s = new Corrector(client).openSession()) {
            BareunException e = assertThrows(BareunException.class, () -> s.correct("글"));
            assertEquals(ConnectCode.NOT_FOUND, e.getCode());
            assertTrue(CorrectionSession.isLost(e));
        }
        assertFalse(CorrectionSession.isLost(
                new BareunException(ConnectCode.UNAVAILABLE, "x", 503, null)));
    }
}
