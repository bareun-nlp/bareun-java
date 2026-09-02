package ai.bareun.client;

import java.util.List;

import ai.bareun.protos.AnalyzeSyntaxListRequest;
import ai.bareun.protos.AnalyzeSyntaxListResponse;
import ai.bareun.protos.AnalyzeSyntaxRawRequest;
import ai.bareun.protos.AnalyzeSyntaxRequest;
import ai.bareun.protos.AnalyzeSyntaxResponse;
import ai.bareun.protos.Document;
import ai.bareun.protos.EncodingType;
import ai.bareun.protos.TokenizeRequest;
import ai.bareun.protos.TokenizeResponse;

/**
 * 형태소 분석 서비스({@code bareun.LanguageService}).
 *
 * <p>요청 메시지를 직접 만들어 넘기는 메서드와, 흔한 경우를 짧게 쓰는 편의 메서드를
 * 함께 둔다. 편의 메서드로 표현되지 않는 옵션이 필요하면 요청 메시지를 만들어 넘긴다.
 */
public final class LanguageServiceClient {

    /** proto 서비스 전체 이름. Connect 경로의 앞부분이 된다. */
    static final String SERVICE = "bareun.LanguageService";

    /**
     * 자바 문자열은 UTF-16 이므로 오프셋도 UTF-16 기준이어야 한다.
     *
     * <p>서버가 돌려주는 {@code begin_offset}·{@code length} 를 그대로
     * {@link String#substring(int, int)} 에 넣으려면 이 값이어야 한다. UTF8 로 받으면
     * 한글이 섞인 문장에서 위치가 어긋난다 — 오류가 아니라 엉뚱한 글자를 잘라 내므로
     * 알아채기 어렵다.
     */
    public static final EncodingType JAVA_ENCODING = EncodingType.UTF16;

    private final ConnectClient connect;

    LanguageServiceClient(ConnectClient connect) {
        this.connect = connect;
    }

    /**
     * 문장을 형태소 분석한다.
     *
     * @param request 분석 요청
     * @return 분석 결과
     * @throws BareunException 호출 실패
     */
    public AnalyzeSyntaxResponse analyzeSyntax(AnalyzeSyntaxRequest request) {
        return connect.call(SERVICE, "AnalyzeSyntax", request, AnalyzeSyntaxResponse.parser());
    }

    /**
     * 문장을 형태소 분석한다.
     *
     * @param text 분석할 문장. 여러 문장이면 줄바꿈으로 나눈다.
     * @param autoSplitSentence 줄바꿈이 없어도 문장을 자동으로 나눌지
     * @param withSense 동형이의어 의미 구분(WSD) 결과를 함께 받을지.
     *        서버에 WSD 모델이 실려 있을 때만 형태소에 sense 가 붙고, 추론이 한 번 더 돈다.
     * @param customDictNames 쓸 사용자 사전 이름들. 앞에 온 것이 우선한다. 없으면 빈 목록.
     * @return 분석 결과
     * @throws BareunException 호출 실패
     */
    public AnalyzeSyntaxResponse analyzeSyntax(String text, boolean autoSplitSentence,
            boolean withSense, List<String> customDictNames) {
        AnalyzeSyntaxRequest.Builder b = AnalyzeSyntaxRequest.newBuilder()
                .setDocument(document(text))
                .setEncodingType(JAVA_ENCODING)
                .setAutoSplitSentence(autoSplitSentence)
                .setWithSense(withSense);
        if (customDictNames != null && !customDictNames.isEmpty()) {
            b.addAllCustomDictNames(customDictNames);
        }
        return analyzeSyntax(b.build());
    }

    /**
     * 문장을 형태소 분석한다. 문장 자동 분리를 켜고 나머지는 기본값으로 둔다.
     *
     * @param text 분석할 문장
     * @return 분석 결과
     * @throws BareunException 호출 실패
     */
    public AnalyzeSyntaxResponse analyzeSyntax(String text) {
        return analyzeSyntax(text, true, false, List.of());
    }

    /**
     * 여러 문장을 한 번에 분석한다. 문장 경계가 이미 정해져 있을 때 쓴다.
     *
     * @param sentences 문장 목록
     * @param withSense WSD 결과를 함께 받을지
     * @return 분석 결과
     * @throws BareunException 호출 실패
     */
    public AnalyzeSyntaxListResponse analyzeSyntaxList(List<String> sentences, boolean withSense) {
        return analyzeSyntaxList(AnalyzeSyntaxListRequest.newBuilder()
                .addAllSentences(sentences)
                .setLanguage("ko_KR")
                .setEncodingType(JAVA_ENCODING)
                .setWithSense(withSense)
                .build());
    }

    /**
     * 여러 문장을 한 번에 분석한다.
     *
     * @param request 분석 요청
     * @return 분석 결과
     * @throws BareunException 호출 실패
     */
    public AnalyzeSyntaxListResponse analyzeSyntaxList(AnalyzeSyntaxListRequest request) {
        return connect.call(SERVICE, "AnalyzeSyntaxList", request, AnalyzeSyntaxListResponse.parser());
    }

    /**
     * 후처리 없이 모델의 원시 출력을 받는다. 분석기 동작을 들여다볼 때 쓴다.
     *
     * @param text 분석할 문장
     * @param withSense WSD 결과를 함께 받을지
     * @return 분석 결과
     * @throws BareunException 호출 실패
     */
    public AnalyzeSyntaxResponse analyzeSyntaxRaw(String text, boolean withSense) {
        return connect.call(SERVICE, "AnalyzeSyntaxRaw",
                AnalyzeSyntaxRawRequest.newBuilder()
                        .setDocument(document(text))
                        .setEncodingType(JAVA_ENCODING)
                        .setAutoSplitSentence(true)
                        .setWithSense(withSense)
                        .build(),
                AnalyzeSyntaxResponse.parser());
    }

    /**
     * 문장을 어절 단위로 자른다.
     *
     * @param text 자를 문장
     * @return 분절 결과
     * @throws BareunException 호출 실패
     */
    public TokenizeResponse tokenize(String text) {
        return connect.call(SERVICE, "Tokenize",
                TokenizeRequest.newBuilder()
                        .setDocument(document(text))
                        .setEncodingType(JAVA_ENCODING)
                        .build(),
                TokenizeResponse.parser());
    }

    /**
     * 요청에 실을 문서를 만든다.
     *
     * @param text 문서 내용
     * @return 문서 메시지
     */
    static Document document(String text) {
        return Document.newBuilder().setContent(text).setLanguage("ko_KR").build();
    }
}
