package ai.bareun.tagger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.google.protobuf.MessageLite;

import ai.bareun.client.BareunClient;
import ai.bareun.protos.AnalyzeSyntaxListResponse;
import ai.bareun.protos.AnalyzeSyntaxResponse;
import ai.bareun.protos.CorrectErrorResponse;
import ai.bareun.protos.Sentence;
import ai.bareun.protos.Truncation;

/**
 * 잘림(truncation) 응답을 읽는 {@link Tagger}·{@link Corrector}·{@link TruncationInfo} 단위 테스트.
 *
 * <p>JDK 의 {@link HttpServer} 로 서버 응답을 흉내 낸다. 실제 서버로는 통합 테스트에서 확인한다.
 */
class TruncationTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    /**
     * 모든 요청에 같은 본문을 돌려주는 가짜 서버를 띄우고 그에 붙는 클라이언트를 만든다.
     *
     * @param body 응답 메시지
     * @return 그 서버에 붙는 클라이언트
     */
    private BareunClient clientReturning(MessageLite body) throws IOException {
        byte[] bytes = body.toByteArray();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            ex.getResponseHeaders().add("Content-Type", "application/proto");
            ex.sendResponseHeaders(200, bytes.length == 0 ? -1 : bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        return BareunClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("koba-TEST").build();
    }

    private static Truncation sampleTruncation() {
        return Truncation.newBuilder()
                .setOriginalChars(9001).setProcessedChars(4991).setDroppedChars(4010)
                .setDroppedBeginOffset(4991).setMessage("앞 4991자만 처리했습니다.")
                .setCutMidWord(true).build();
    }

    @Test
    void 분석_응답의_잘림_정보를_읽는다() throws IOException {
        var client = clientReturning(AnalyzeSyntaxResponse.newBuilder()
                .setLanguage("ko_KR").setTruncation(sampleTruncation()).build());

        Tagged t = new Tagger(client).tag("글");

        assertTrue(t.truncated());
        TruncationInfo info = t.truncation().orElseThrow();
        assertEquals(9001, info.originalChars());
        assertEquals(4991, info.processedChars());
        assertEquals(4010, info.droppedChars());
        assertEquals(4991, info.droppedBeginOffset());
        assertEquals("앞 4991자만 처리했습니다.", info.message());
        assertTrue(info.cutMidWord());
    }

    @Test
    void 잘리지_않은_분석_응답은_잘림_정보가_없다() throws IOException {
        var client = clientReturning(AnalyzeSyntaxResponse.newBuilder().setLanguage("ko_KR").build());

        Tagged t = new Tagger(client).tag("글");

        assertFalse(t.truncated());
        assertTrue(t.truncation().isEmpty());
    }

    @Test
    void 문장_목록_응답의_잘림_정보와_문장_번호를_읽는다() throws IOException {
        var client = clientReturning(AnalyzeSyntaxListResponse.newBuilder()
                .setLanguage("ko_KR")
                .addSentences(Sentence.getDefaultInstance())
                .addSentences(Sentence.getDefaultInstance())
                .setTruncation(sampleTruncation())
                .setDroppedBeginSentenceIndex(1).build());

        TaggedBatch batch = new Tagger(client).tagBatch(List.of("첫째", "둘째"), false);

        assertTrue(batch.truncated());
        assertEquals(1, batch.droppedBeginSentenceIndex());
        assertEquals(4010, batch.truncation().orElseThrow().droppedChars());
        assertFalse(batch.notProcessed(0));
        assertFalse(batch.notProcessed(1));
        assertEquals(2, batch.items().size());
        assertEquals("둘째", batch.items().get(1).text());
    }

    @Test
    void 잘린_뒤_문장은_처리되지_않았다고_알린다() throws IOException {
        var client = clientReturning(AnalyzeSyntaxListResponse.newBuilder()
                .addSentences(Sentence.getDefaultInstance())
                .addSentences(Sentence.getDefaultInstance())
                .addSentences(Sentence.getDefaultInstance())
                .setTruncation(sampleTruncation())
                .setDroppedBeginSentenceIndex(1).build());

        TaggedBatch batch = new Tagger(client).tagBatch(List.of("가", "나", "다"), false);

        assertFalse(batch.notProcessed(0));
        assertFalse(batch.notProcessed(1));
        assertTrue(batch.notProcessed(2));
    }

    @Test
    void 잘리지_않은_문장_목록은_번호가_마이너스_일이다() throws IOException {
        var client = clientReturning(AnalyzeSyntaxListResponse.newBuilder()
                .addSentences(Sentence.getDefaultInstance()).build());

        TaggedBatch batch = new Tagger(client).tagBatch(List.of("하나"), false);

        assertFalse(batch.truncated());
        assertEquals(-1, batch.droppedBeginSentenceIndex());
        assertFalse(batch.notProcessed(5));
    }

    @Test
    void 응답_문장이_더_많아도_원문은_빈_문자열로_채운다() throws IOException {
        var client = clientReturning(AnalyzeSyntaxListResponse.newBuilder()
                .addSentences(Sentence.getDefaultInstance())
                .addSentences(Sentence.getDefaultInstance()).build());

        List<Tagged> all = new Tagger(client).tagAll(List.of("하나"), false);

        assertEquals(List.of("하나", ""), all.stream().map(Tagged::text).toList());
    }

    @Test
    void 교정_응답의_잘림_정보를_읽는다() throws IOException {
        var client = clientReturning(CorrectErrorResponse.newBuilder()
                .setRevised("앞부분").setTruncation(sampleTruncation()).build());

        Corrector.Result r = new Corrector(client).correctWithTruncation("긴 글");

        assertEquals("앞부분", r.revised());
        assertTrue(r.truncated());
        assertEquals(4991, r.truncation().orElseThrow().droppedBeginOffset());
    }

    @Test
    void 잘리지_않은_교정_응답은_잘림_정보가_없다() throws IOException {
        var client = clientReturning(CorrectErrorResponse.newBuilder().setRevised("고침").build());

        Corrector.Result r = new Corrector(client).correctWithTruncation("글");

        assertEquals("고침", r.revised());
        assertFalse(r.truncated());
    }
}
