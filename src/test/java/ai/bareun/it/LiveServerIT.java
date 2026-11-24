package ai.bareun.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import ai.bareun.client.BareunClient;
import ai.bareun.protos.DictSearchAnchor;
import ai.bareun.tagger.Corrector;
import ai.bareun.tagger.Tagged;
import ai.bareun.tagger.Tagger;
import ai.bareun.tagger.TaggedBatch;
import ai.bareun.tagger.TruncationInfo;

/**
 * 살아 있는 서버가 있어야 도는 통합 테스트.
 *
 * <p>{@code BAREUN_API_KEY} 가 설정돼 있을 때만 돈다. CI 에서 키 없이 돌 때는
 * 통째로 건너뛴다 — 서버가 없다는 이유로 빌드가 빨개지면 아무도 보지 않게 된다.
 *
 * <p>서버 주소는 {@code BAREUN_HOST}(기본 localhost)·{@code BAREUN_PORT}(기본 5656)로 준다.
 * 교정과 사전 검색은 맞춤법 교정(rev) 빌드에서만 동작하므로, 아닌 서버에서는
 * 해당 테스트가 실패한다.
 */
@EnabledIfEnvironmentVariable(named = "BAREUN_API_KEY", matches = ".+")
class LiveServerIT {

    private static BareunClient client;

    @BeforeAll
    static void setUp() {
        client = BareunClient.builder()
                .host(System.getenv().getOrDefault("BAREUN_HOST", "localhost"))
                .port(Integer.parseInt(System.getenv().getOrDefault("BAREUN_PORT", "5656")))
                .apiKey(System.getenv("BAREUN_API_KEY"))
                .build();
    }

    @Test
    void 형태소_분석() {
        Tagged t = new Tagger(client).tag("아버지가 방에 들어가신다.");
        assertEquals(List.of("아버지", "방"), t.nouns());
        assertEquals(List.of("들어가"), t.verbs());
        assertTrue(t.pos().contains("아버지/NNG"));
    }

    @Test
    void 오프셋이_자바_문자열_기준이다() {
        // UTF16 으로 요청하므로 서버가 준 위치를 substring 에 그대로 넣을 수 있어야 한다.
        String text = "아버지가 방에 들어가신다.";
        var token = new Tagger(client).tag(text).sentences().get(0).getTokens(0).getText();
        assertEquals("아버지가",
                text.substring(token.getBeginOffset(), token.getBeginOffset() + token.getLength()));
    }

    @Test
    void 동형이의어_의미_구분() {
        List<Tagged.SenseEntry> senses = new Tagger(client).tag("나는 밤에 밤을 먹었다.", true).senses();
        assertFalse(senses.isEmpty(), "WSD 모델이 실린 서버에서는 sense 가 붙어야 한다");
        assertTrue(senses.stream().anyMatch(s -> !s.meaning().isBlank()));
    }

    @Test
    void 맞춤법_교정() {
        Corrector c = new Corrector(client);
        assertTrue(c.correct("이거 안되요. 학교에 갔읍니다.").endsWith("학교에 갔습니다."));

        List<Corrector.Change> changes = c.changes("이거 안되요. 학교에 갔읍니다.");
        Corrector.Change ch = changes.stream()
                .filter(x -> x.origin().equals("갔읍니다.")).findFirst().orElseThrow();
        assertEquals("갔습니다.", ch.revised());
        assertEquals("STANDARD", ch.category());
    }

    @Test
    void 사전_자소_검색() {
        assertEquals(List.of("신다"),
                client.dictSearch().searchDict("{ㅅ//ㄴ}다", DictSearchAnchor.DICT_SEARCH_ANCHOR_WORD,
                        List.of("동사"), 10).getEntriesList().stream().map(e -> e.getWord()).toList());

        assertTrue(client.dictSearch()
                .words("아지", DictSearchAnchor.DICT_SEARCH_ANCHOR_SUFFIX, 10)
                .contains("강아지"));
    }

    @Test
    void 사용자_사전_목록() {
        // 사전이 하나도 없어도 호출 자체는 성공해야 한다.
        client.customDictionary().list();
    }

    @Test
    void 어절_분절() {
        assertFalse(client.language().tokenize("아버지가 방에 들어가신다.")
                .getSentencesList().isEmpty());
    }

    /**
     * 글자 수 기준: 서버가 {@code request_char_limit} 을 건 경우(운영 서비스 9,000자)만 의미가 있다.
     * 그런 서버에 시험하려면 {@code BAREUN_TRUNCATION_LIMIT=9000} 을 준다. 자체 설치 서버는 자르지 않는다.
     */
    private static String longText(int chars) {
        String unit = "오늘은 날씨가 맑아서 공원에 산책을 다녀왔다. ";
        return unit.repeat(chars / unit.length() + 1).substring(0, chars);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "BAREUN_TRUNCATION_LIMIT", matches = "\\d+")
    void 기준_이하_글은_잘리지_않는다() {
        int limit = Integer.parseInt(System.getenv("BAREUN_TRUNCATION_LIMIT"));
        String text = longText(limit);
        assertFalse(new Tagger(client).tag(text).truncated());
        var r = new Corrector(client).correctWithTruncation(text);
        assertFalse(r.truncated());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "BAREUN_TRUNCATION_LIMIT", matches = "\\d+")
    void 기준을_넘는_글은_앞부분만_처리하고_알린다() {
        int limit = Integer.parseInt(System.getenv("BAREUN_TRUNCATION_LIMIT"));
        String text = longText(limit + 1);

        TruncationInfo tagInfo = new Tagger(client).tag(text).truncation().orElseThrow();
        assertEquals(limit + 1, tagInfo.originalChars());
        assertEquals(tagInfo.originalChars(), tagInfo.processedChars() + tagInfo.droppedChars());
        assertFalse(tagInfo.message().isBlank());

        var r = new Corrector(client).correctWithTruncation(text);
        TruncationInfo info = r.truncation().orElseThrow();
        // UTF-16 으로 요청하므로 버린 구간 시작을 substring 에 그대로 쓸 수 있다.
        assertEquals(text.length() - info.droppedChars(), info.droppedBeginOffset());
        assertTrue(text.substring(info.droppedBeginOffset()).length() == info.droppedChars());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "BAREUN_TRUNCATION_LIMIT", matches = "\\d+")
    void 문장_목록은_요청과_일대일이고_빠진_문장은_비어_있다() {
        int limit = Integer.parseInt(System.getenv("BAREUN_TRUNCATION_LIMIT"));
        String sentence = longText(100);
        List<String> sentences = java.util.Collections.nCopies(limit / 100 + 1, sentence);

        TaggedBatch batch = new Tagger(client).tagBatch(sentences, false);

        assertTrue(batch.truncated());
        assertEquals(sentences.size(), batch.items().size());
        assertTrue(batch.droppedBeginSentenceIndex() > 0);
        assertTrue(batch.notProcessed(sentences.size() - 1));
        assertFalse(batch.notProcessed(0));
        assertEquals(0, batch.items().get(sentences.size() - 1).morphs().size());
    }
}
