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
        assertEquals("이거 안되요. 학교에 갔습니다.", c.correct("이거 안되요. 학교에 갔읍니다."));

        List<Corrector.Change> changes = c.changes("이거 안되요. 학교에 갔읍니다.");
        assertEquals(1, changes.size());
        assertEquals("갔읍니다.", changes.get(0).origin());
        assertEquals("갔습니다.", changes.get(0).revised());
        assertEquals("STANDARD", changes.get(0).category());
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
}
