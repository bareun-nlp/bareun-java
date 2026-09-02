package ai.bareun.tagger;

import java.util.ArrayList;
import java.util.List;

import ai.bareun.client.BareunException;
import ai.bareun.protos.AnalyzeSyntaxResponse;
import ai.bareun.protos.Morpheme;
import ai.bareun.protos.Sentence;
import ai.bareun.protos.Token;

/**
 * 형태소 분석 결과를 다루기 쉽게 감싼 객체.
 *
 * <p>원본 응답이 필요하면 {@link #response()} 로 꺼낸다. 이 클래스는 흔히 쓰는
 * 형태(형태소 목록, 품사 붙인 목록, 명사·동사만)를 뽑는 일만 한다.
 *
 * <p>불변이다. 만들어진 뒤 내용이 바뀌지 않는다.
 */
public final class Tagged {

    private final String text;
    private final AnalyzeSyntaxResponse response;

    /**
     * @param text 분석에 넣은 원문
     * @param response 서버 응답
     */
    Tagged(String text, AnalyzeSyntaxResponse response) {
        this.text = text;
        this.response = response;
    }

    /** @return 분석에 넣은 원문 */
    public String text() {
        return text;
    }

    /** @return 서버 응답 원본 */
    public AnalyzeSyntaxResponse response() {
        return response;
    }

    /** @return 문장 목록 */
    public List<Sentence> sentences() {
        return response.getSentencesList();
    }

    /**
     * 형태소를 문장 구분 없이 늘어놓는다.
     *
     * @return 형태소 표층형 목록. 예: 아버지, 가, 방, 에, 들어가, 시, ㄴ다, .
     */
    public List<String> morphs() {
        List<String> out = new ArrayList<>();
        forEachMorpheme(m -> out.add(m.getText().getContent()));
        return out;
    }

    /**
     * 형태소에 품사를 붙여 늘어놓는다.
     *
     * @return {@code 형태소/품사} 목록. 예: 아버지/NNG, 가/JKS
     */
    public List<String> pos() {
        List<String> out = new ArrayList<>();
        forEachMorpheme(m -> out.add(m.getText().getContent() + "/" + m.getTag().name()));
        return out;
    }

    /**
     * 명사만 뽑는다.
     *
     * <p>일반명사(NNG)·고유명사(NNP)·의존명사(NNB)·대명사(NP)·수사(NR)를 명사로 본다.
     *
     * @return 명사 목록
     */
    public List<String> nouns() {
        return byTagPrefix("NN", "NP", "NR");
    }

    /**
     * 동사만 뽑는다.
     *
     * <p>동사(VV)만 본다. 형용사(VA)나 보조용언(VX)은 포함하지 않는다.
     *
     * @return 동사 목록
     */
    public List<String> verbs() {
        return byTagPrefix("VV");
    }

    /**
     * 동형이의어 의미 구분(WSD) 결과를 뽑는다.
     *
     * <p>{@code withSense} 를 켜고 분석했고, 서버에 WSD 모델이 실려 있으며, 그 형태소에
     * 의미가 부여된 경우에만 값이 있다. 조사·어미처럼 의미를 갖지 않는 형태소에는
     * 원래 붙지 않으므로, 대부분의 형태소는 여기에 나오지 않는다.
     *
     * @return 의미가 부여된 형태소 목록
     */
    public List<SenseEntry> senses() {
        List<SenseEntry> out = new ArrayList<>();
        forEachMorpheme(m -> {
            if (m.hasSense()) {
                out.add(new SenseEntry(
                        m.getText().getContent(),
                        m.getTag().name(),
                        m.getSense().getSenseNo(),
                        m.getSense().getMeaning(),
                        m.getSense().getProbability()));
            }
        });
        return out;
    }

    /**
     * 태그가 주어진 접두사 중 하나로 시작하는 형태소를 뽑는다.
     *
     * @param prefixes 태그 접두사들
     * @return 해당하는 형태소 표층형 목록
     */
    private List<String> byTagPrefix(String... prefixes) {
        List<String> out = new ArrayList<>();
        forEachMorpheme(m -> {
            String tag = m.getTag().name();
            for (String p : prefixes) {
                if (tag.startsWith(p)) {
                    out.add(m.getText().getContent());
                    return;
                }
            }
        });
        return out;
    }

    /**
     * 모든 문장의 모든 어절의 모든 형태소를 순서대로 훑는다.
     *
     * <p>응답이 문장 → 어절 → 형태소의 3중 구조라, 뽑아 쓰는 메서드마다 같은 3중
     * 반복을 쓰는 것을 피하려고 한 곳에 모았다.
     *
     * @param fn 형태소마다 부를 함수
     */
    private void forEachMorpheme(java.util.function.Consumer<Morpheme> fn) {
        for (Sentence s : response.getSentencesList()) {
            for (Token t : s.getTokensList()) {
                for (Morpheme m : t.getMorphemesList()) {
                    fn.accept(m);
                }
            }
        }
    }

    /**
     * 의미가 부여된 형태소 하나.
     *
     * @param morph 형태소 표층형
     * @param tag 품사
     * @param senseNo 우리말샘 어깨번호
     * @param meaning 뜻풀이
     * @param probability 모델 점수. 후보 집합 안에서의 확률이라 합이 1 이다
     *        ({@link Morpheme#getProbability()} 와 척도가 다르니 같이 비교하지 말 것).
     */
    public record SenseEntry(String morph, String tag, int senseNo, String meaning,
            float probability) {
    }

    /**
     * 결과가 비어 있는지 본다.
     *
     * <p>서버가 성공을 돌려줬는데 문장이 하나도 없는 경우가 있다(빈 입력 등).
     * 그때 {@link BareunException} 이 나지는 않으므로 호출한 쪽에서 확인해야 한다.
     *
     * @return 분석된 문장이 하나도 없으면 true
     */
    public boolean isEmpty() {
        return response.getSentencesCount() == 0;
    }
}
