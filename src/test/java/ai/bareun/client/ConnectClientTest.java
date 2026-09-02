package ai.bareun.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import ai.bareun.protos.AnalyzeSyntaxRequest;
import ai.bareun.protos.AnalyzeSyntaxResponse;

/**
 * {@link ConnectClient} 단위 테스트.
 *
 * <p>실제 바른 서버 없이, JDK 에 딸린 {@link HttpServer} 로 응답을 흉내 내 확인한다.
 * 서버가 있어야 도는 검증은 통합 테스트로 따로 둔다.
 */
class ConnectClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    /**
     * 주어진 상태·본문을 그대로 돌려주는 가짜 서버를 띄운다.
     *
     * @param status HTTP 상태
     * @param contentType 응답 Content-Type
     * @param body 응답 본문
     * @return 기준 주소
     */
    private String startServer(int status, String contentType, byte[] body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", contentType);
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private ConnectClient client(String baseUrl) {
        return new ConnectClient(baseUrl, "koba-TEST", Duration.ofSeconds(5),
                HttpClient.newHttpClient());
    }

    @Test
    void 정상_응답을_파싱한다() throws IOException {
        AnalyzeSyntaxResponse expected = AnalyzeSyntaxResponse.newBuilder()
                .setLanguage("ko_KR").build();
        String url = startServer(200, "application/proto", expected.toByteArray());

        AnalyzeSyntaxResponse got = client(url).call("bareun.LanguageService", "AnalyzeSyntax",
                AnalyzeSyntaxRequest.getDefaultInstance(), AnalyzeSyntaxResponse.parser());

        assertEquals("ko_KR", got.getLanguage());
    }

    @Test
    void 서버에_닿지_못하면_UNAVAILABLE() throws IOException {
        // 아무도 듣지 않는 포트를 잡아 둔 뒤 바로 닫아 접속을 실패시킨다.
        int port;
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }
        BareunException e = assertThrows(BareunException.class,
                () -> client("http://127.0.0.1:" + port).call("bareun.LanguageService",
                        "AnalyzeSyntax", AnalyzeSyntaxRequest.getDefaultInstance(),
                        AnalyzeSyntaxResponse.parser()));

        assertEquals(ConnectCode.UNAVAILABLE, e.getCode());
        assertEquals(0, e.getHttpStatus());
    }

    @Test
    void Connect_오류_본문에서_코드와_메시지를_읽는다() throws IOException {
        String json = "{\"code\":\"permission_denied\",\"message\":\"API키가 유효하지 않습니다\"}";
        String url = startServer(403, "application/json", json.getBytes(StandardCharsets.UTF_8));

        BareunException e = assertThrows(BareunException.class,
                () -> client(url).call("bareun.LanguageService", "AnalyzeSyntax",
                        AnalyzeSyntaxRequest.getDefaultInstance(), AnalyzeSyntaxResponse.parser()));

        assertEquals(ConnectCode.PERMISSION_DENIED, e.getCode());
        assertEquals(403, e.getHttpStatus());
        assertEquals("API키가 유효하지 않습니다", e.getMessage());
    }

    @Test
    void 오류_본문이_JSON이_아니면_HTTP_상태로_코드를_정한다() throws IOException {
        // 경로가 없을 때 서버는 평문 "404 page not found" 를 돌려준다.
        String url = startServer(404, "text/plain",
                "404 page not found".getBytes(StandardCharsets.UTF_8));

        BareunException e = assertThrows(BareunException.class,
                () -> client(url).call("bareun.LanguageService", "NoSuchMethod",
                        AnalyzeSyntaxRequest.getDefaultInstance(), AnalyzeSyntaxResponse.parser()));

        assertEquals(ConnectCode.UNIMPLEMENTED, e.getCode());
        assertTrue(e.getMessage().contains("404"));
    }

    @Test
    void 응답이_깨지면_INTERNAL() throws IOException {
        // 2xx 인데 파싱이 안 되는 바이트 — proto 가 어긋난 상황을 흉내 낸다.
        String url = startServer(200, "application/proto", new byte[] {(byte) 0xff, (byte) 0xff});

        BareunException e = assertThrows(BareunException.class,
                () -> client(url).call("bareun.LanguageService", "AnalyzeSyntax",
                        AnalyzeSyntaxRequest.getDefaultInstance(), AnalyzeSyntaxResponse.parser()));

        assertEquals(ConnectCode.INTERNAL, e.getCode());
    }

    @Test
    void 끝_슬래시가_있어도_경로가_겹치지_않는다() throws IOException {
        AnalyzeSyntaxResponse expected = AnalyzeSyntaxResponse.newBuilder()
                .setLanguage("ko_KR").build();
        String url = startServer(200, "application/proto", expected.toByteArray());

        // baseUrl 끝에 슬래시가 붙으면 "//bareun..." 가 되어 404 가 난다. 지워야 한다.
        ConnectClient c = new ConnectClient(url + "///", "koba-TEST", Duration.ofSeconds(5),
                HttpClient.newHttpClient());
        assertEquals("ko_KR", c.call("bareun.LanguageService", "AnalyzeSyntax",
                AnalyzeSyntaxRequest.getDefaultInstance(),
                AnalyzeSyntaxResponse.parser()).getLanguage());
    }

    @Test
    void JSON_문자열_추출() {
        assertEquals("abc", ConnectClient.extractJsonString("{\"m\":\"abc\"}", "m"));
        assertEquals("한글", ConnectClient.extractJsonString("{\"m\": \"한글\"}", "m"));
        assertEquals("a\"b", ConnectClient.extractJsonString("{\"m\":\"a\\\"b\"}", "m"));
        assertEquals("a\nb", ConnectClient.extractJsonString("{\"m\":\"a\\nb\"}", "m"));
        assertEquals("A", ConnectClient.extractJsonString("{\"m\":\"\\u0041\"}", "m"));
        assertNull(ConnectClient.extractJsonString("{\"x\":\"1\"}", "m"));
        assertNull(ConnectClient.extractJsonString("{\"m\":1}", "m"));
        assertNull(ConnectClient.extractJsonString("not json", "m"));
    }

    @Test
    void 모르는_코드는_UNKNOWN() {
        assertEquals(ConnectCode.UNKNOWN, ConnectCode.from(null));
        assertEquals(ConnectCode.UNKNOWN, ConnectCode.from("brand_new_code"));
        assertEquals(ConnectCode.UNIMPLEMENTED, ConnectCode.from("unimplemented"));
        assertEquals(ConnectCode.PERMISSION_DENIED, ConnectCode.from("  Permission_Denied "));
    }

    @Test
    void 빌더는_같은_HttpClient를_쓴다() {
        HttpClient given = HttpClient.newHttpClient();
        BareunClient c = BareunClient.builder().apiKey("koba-TEST").httpClient(given).build();
        // 서비스 클라이언트들이 하나의 ConnectClient 를 공유해야 연결이 재사용된다.
        assertSame(c.connect(), c.connect());
    }

    @Test
    void API_키가_없으면_빌드에서_막는다() {
        assertThrows(IllegalArgumentException.class, () -> BareunClient.builder().build());
        assertThrows(IllegalArgumentException.class,
                () -> BareunClient.builder().apiKey("  ").build());
    }
}
