package ai.bareun.tagger;

import java.util.List;

import ai.bareun.client.BareunClient;
import ai.bareun.client.BareunException;

/**
 * 형태소 분석을 짧게 쓰는 진입점.
 *
 * <pre>{@code
 * BareunClient client = BareunClient.builder().apiKey("koba-...").build();
 * Tagger tagger = new Tagger(client);
 *
 * Tagged t = tagger.tag("아버지가 방에 들어가신다.");
 * t.pos();    // [아버지/NNG, 가/JKS, 방/NNG, 에/JKB, 들어가/VV, 시/EP, ㄴ다/EF, ./SF]
 * t.nouns();  // [아버지, 방]
 * }</pre>
 *
 * <p>세밀한 옵션이 필요하면 {@link BareunClient#language()} 로 내려가 요청 메시지를
 * 직접 만든다.
 *
 * <p>스레드 안전하다. 상태를 갖지 않고 {@link BareunClient} 에 위임하기만 한다.
 */
public final class Tagger {

    private final BareunClient client;
    private final List<String> customDictNames;

    /**
     * @param client 쓸 클라이언트
     */
    public Tagger(BareunClient client) {
        this(client, List.of());
    }

    /**
     * @param client 쓸 클라이언트
     * @param customDictNames 모든 분석에 함께 쓸 사용자 사전 이름들. 앞에 온 것이 우선한다.
     */
    public Tagger(BareunClient client, List<String> customDictNames) {
        this.client = client;
        this.customDictNames = List.copyOf(customDictNames);
    }

    /**
     * 문장을 분석한다.
     *
     * @param text 분석할 문장. 여러 문장이면 줄바꿈으로 나눠도 되고, 이어 붙여도
     *        서버가 문장을 나눈다.
     * @return 분석 결과
     * @throws BareunException 호출 실패
     */
    public Tagged tag(String text) {
        return tag(text, false);
    }

    /**
     * 문장을 분석한다.
     *
     * @param text 분석할 문장
     * @param withSense 동형이의어 의미 구분(WSD) 결과를 함께 받을지.
     *        켜면 추론이 한 번 더 돌아 느려진다.
     * @return 분석 결과
     * @throws BareunException 호출 실패
     */
    public Tagged tag(String text, boolean withSense) {
        return new Tagged(text,
                client.language().analyzeSyntax(text, true, withSense, customDictNames));
    }

    /**
     * 문장 목록을 한 번에 분석한다. 문장 경계가 이미 정해져 있을 때 왕복을 줄인다.
     *
     * @param sentences 문장 목록
     * @param withSense WSD 결과를 함께 받을지
     * @return 분석 결과들. 입력 순서와 같다.
     * @throws BareunException 호출 실패
     */
    public List<Tagged> tagAll(List<String> sentences, boolean withSense) {
        var res = client.language().analyzeSyntaxList(sentences, withSense);
        // 응답은 문장별 결과를 한 응답 안에 담아 준다. 입력과 짝지어 돌려주려고
        // 문장 하나짜리 응답으로 다시 쪼갠다.
        List<Tagged> out = new java.util.ArrayList<>(res.getSentencesCount());
        for (int i = 0; i < res.getSentencesCount(); i++) {
            var one = ai.bareun.protos.AnalyzeSyntaxResponse.newBuilder()
                    .addSentences(res.getSentences(i))
                    .setLanguage(res.getLanguage())
                    .build();
            String src = (i < sentences.size()) ? sentences.get(i) : "";
            out.add(new Tagged(src, one));
        }
        return out;
    }

    /**
     * 형태소 표층형만 뽑는다.
     *
     * @param text 분석할 문장
     * @return 형태소 목록
     * @throws BareunException 호출 실패
     */
    public List<String> morphs(String text) {
        return tag(text).morphs();
    }

    /**
     * 형태소에 품사를 붙여 뽑는다.
     *
     * @param text 분석할 문장
     * @return {@code 형태소/품사} 목록
     * @throws BareunException 호출 실패
     */
    public List<String> pos(String text) {
        return tag(text).pos();
    }

    /**
     * 명사만 뽑는다.
     *
     * @param text 분석할 문장
     * @return 명사 목록
     * @throws BareunException 호출 실패
     */
    public List<String> nouns(String text) {
        return tag(text).nouns();
    }

    /**
     * 동사만 뽑는다.
     *
     * @param text 분석할 문장
     * @return 동사 목록
     * @throws BareunException 호출 실패
     */
    public List<String> verbs(String text) {
        return tag(text).verbs();
    }
}
