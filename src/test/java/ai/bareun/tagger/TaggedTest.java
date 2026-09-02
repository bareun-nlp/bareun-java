package ai.bareun.tagger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import ai.bareun.protos.AnalyzeSyntaxResponse;
import ai.bareun.protos.Morpheme;
import ai.bareun.protos.Sense;
import ai.bareun.protos.Sentence;
import ai.bareun.protos.Morpheme.Tag;
import ai.bareun.protos.TextSpan;
import ai.bareun.protos.Token;

/**
 * {@link Tagged} 단위 테스트. 응답을 손으로 만들어 서버 없이 확인한다.
 */
class TaggedTest {

    /**
     * 형태소 하나를 만든다.
     *
     * @param text 표층형
     * @param tag 품사
     * @return 형태소
     */
    private static Morpheme morph(String text, Tag tag) {
        return Morpheme.newBuilder()
                .setText(TextSpan.newBuilder().setContent(text))
                .setTag(tag)
                .build();
    }

    /**
     * "아버지가 방에" 를 흉내 낸 응답 하나를 만든다.
     *
     * @return 분석 결과
     */
    private static Tagged sample() {
        Token t1 = Token.newBuilder()
                .setText(TextSpan.newBuilder().setContent("아버지가"))
                .addMorphemes(morph("아버지", Tag.NNG))
                .addMorphemes(morph("가", Tag.JKS))
                .build();
        Token t2 = Token.newBuilder()
                .setText(TextSpan.newBuilder().setContent("들어가신다"))
                .addMorphemes(morph("들어가", Tag.VV))
                .addMorphemes(morph("시", Tag.EP))
                .build();
        AnalyzeSyntaxResponse res = AnalyzeSyntaxResponse.newBuilder()
                .addSentences(Sentence.newBuilder().addTokens(t1).addTokens(t2))
                .setLanguage("ko_KR")
                .build();
        return new Tagged("아버지가 들어가신다", res);
    }

    @Test
    void 형태소를_문장_구분_없이_늘어놓는다() {
        assertEquals(List.of("아버지", "가", "들어가", "시"), sample().morphs());
    }

    @Test
    void 품사를_붙인다() {
        assertEquals(List.of("아버지/NNG", "가/JKS", "들어가/VV", "시/EP"), sample().pos());
    }

    @Test
    void 명사만_뽑는다() {
        assertEquals(List.of("아버지"), sample().nouns());
    }

    @Test
    void 동사만_뽑는다() {
        assertEquals(List.of("들어가"), sample().verbs());
    }

    @Test
    void 대명사와_수사도_명사로_센다() {
        AnalyzeSyntaxResponse res = AnalyzeSyntaxResponse.newBuilder()
                .addSentences(Sentence.newBuilder().addTokens(Token.newBuilder()
                        .addMorphemes(morph("나", Tag.NP))
                        .addMorphemes(morph("하나", Tag.NR))
                        .addMorphemes(morph("것", Tag.NNB))
                        .addMorphemes(morph("는", Tag.JX))))
                .build();
        assertEquals(List.of("나", "하나", "것"), new Tagged("", res).nouns());
    }

    @Test
    void 형용사는_동사에_넣지_않는다() {
        AnalyzeSyntaxResponse res = AnalyzeSyntaxResponse.newBuilder()
                .addSentences(Sentence.newBuilder().addTokens(Token.newBuilder()
                        .addMorphemes(morph("예쁘", Tag.VA))
                        .addMorphemes(morph("있", Tag.VX))))
                .build();
        assertTrue(new Tagged("", res).verbs().isEmpty());
    }

    @Test
    void sense가_붙은_형태소만_뽑는다() {
        Morpheme withSense = Morpheme.newBuilder()
                .setText(TextSpan.newBuilder().setContent("밤"))
                .setTag(Tag.NNG)
                .setSense(Sense.newBuilder()
                        .setSenseNo(2)
                        .setMeaning("밤나무의 열매.")
                        .setProbability(0.55f))
                .build();
        AnalyzeSyntaxResponse res = AnalyzeSyntaxResponse.newBuilder()
                .addSentences(Sentence.newBuilder().addTokens(Token.newBuilder()
                        .addMorphemes(morph("나", Tag.NP))
                        .addMorphemes(withSense)))
                .build();

        List<Tagged.SenseEntry> senses = new Tagged("", res).senses();
        assertEquals(1, senses.size());
        assertEquals("밤", senses.get(0).morph());
        assertEquals("NNG", senses.get(0).tag());
        assertEquals(2, senses.get(0).senseNo());
        assertEquals("밤나무의 열매.", senses.get(0).meaning());
    }

    @Test
    void with_sense를_켜지_않으면_sense가_비어_있다() {
        assertTrue(sample().senses().isEmpty());
    }

    @Test
    void 빈_결과를_구분한다() {
        assertTrue(new Tagged("", AnalyzeSyntaxResponse.getDefaultInstance()).isEmpty());
        assertFalse(sample().isEmpty());
        assertTrue(new Tagged("", AnalyzeSyntaxResponse.getDefaultInstance()).morphs().isEmpty());
    }

    @Test
    void 원문과_응답을_그대로_들고_있다() {
        Tagged t = sample();
        assertEquals("아버지가 들어가신다", t.text());
        assertEquals("ko_KR", t.response().getLanguage());
        assertEquals(1, t.sentences().size());
    }
}
