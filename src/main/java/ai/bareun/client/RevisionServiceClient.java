package ai.bareun.client;

import java.util.List;

import ai.bareun.protos.CloseSessionRequest;
import ai.bareun.protos.CloseSessionResponse;
import ai.bareun.protos.CorrectErrorRequest;
import ai.bareun.protos.CorrectErrorResponse;
import ai.bareun.protos.EncodingType;
import ai.bareun.protos.GetSessionEventsRequest;
import ai.bareun.protos.GetSessionEventsResponse;
import ai.bareun.protos.OpenSessionRequest;
import ai.bareun.protos.OpenSessionResponse;
import ai.bareun.protos.RevisionConfig;

/**
 * 맞춤법·띄어쓰기 교정 서비스({@code bareun.RevisionService}).
 *
 * <p>맞춤법 교정(rev) 빌드의 서버에서만 동작한다. 형태소 분석 전용 빌드에 요청하면
 * {@link ConnectCode#UNIMPLEMENTED} 로 떨어진다.
 *
 * <p>스트리밍 교정({@code StreamCorrectError})은 아직 감싸지 않았다. Connect 의
 * 스트리밍은 바디를 프레이밍해야 해서 단항과 전송 방식이 다르다.
 */
public final class RevisionServiceClient {

    static final String SERVICE = "bareun.RevisionService";

    private final ConnectClient connect;

    RevisionServiceClient(ConnectClient connect) {
        this.connect = connect;
    }

    /**
     * 맞춤법·띄어쓰기를 교정한다.
     *
     * @param request 교정 요청
     * @return 교정 결과
     * @throws BareunException 호출 실패
     */
    public CorrectErrorResponse correctError(CorrectErrorRequest request) {
        return connect.call(SERVICE, "CorrectError", request, CorrectErrorResponse.parser());
    }

    /**
     * 맞춤법·띄어쓰기를 교정한다.
     *
     * @param text 교정할 문장
     * @param customDictNames 쓸 사용자 사전 이름들. 없으면 빈 목록.
     * @param config 교정기 옵션. 기본값으로 두려면 null.
     * @return 교정 결과
     * @throws BareunException 호출 실패
     */
    public CorrectErrorResponse correctError(String text, List<String> customDictNames,
            RevisionConfig config) {
        CorrectErrorRequest.Builder b = CorrectErrorRequest.newBuilder()
                .setDocument(LanguageServiceClient.document(text))
                // 교정 결과의 위치도 자바 문자열 기준이어야 substring 이 맞는다.
                .setEncodingType(EncodingType.UTF16);
        if (customDictNames != null && !customDictNames.isEmpty()) {
            b.addAllCustomDictNames(customDictNames);
        }
        if (config != null) {
            b.setConfig(config);
        }
        return correctError(b.build());
    }

    /**
     * 맞춤법·띄어쓰기를 교정하고 교정된 문장만 돌려준다.
     *
     * @param text 교정할 문장
     * @return 교정된 문장. 고칠 것이 없으면 원문과 같다.
     * @throws BareunException 호출 실패
     */
    public String correct(String text) {
        return correctError(text, List.of(), null).getRevised();
    }

    /**
     * 세션을 연다. 긴 글을 여러 번에 나눠 교정할 때, 앞 호출에서 본 미등록 단어를 뒤 호출에서
     * 알려진 말로 쓰게 하는 상태를 서버에 만든다. 사용자 사전과 교정기 옵션은 여기서 정해지고
     * 세션이 닫힐 때까지 바뀌지 않는다. 쓰기 편한 진입점은 {@code Corrector#openSession()} 이다.
     *
     * @param customDictNames 세션 내내 쓸 사용자 사전 이름들. 없으면 빈 목록.
     * @param config 교정기 옵션. 기본값으로 두려면 null.
     * @return 세션 ID·수명 정보
     * @throws BareunException 호출 실패. 동시 세션 수가 상한을 넘으면
     *         {@link ConnectCode#RESOURCE_EXHAUSTED}.
     */
    public OpenSessionResponse openSession(List<String> customDictNames, RevisionConfig config) {
        OpenSessionRequest.Builder b = OpenSessionRequest.newBuilder();
        if (customDictNames != null && !customDictNames.isEmpty()) {
            b.addAllCustomDictNames(customDictNames);
        }
        if (config != null) {
            b.setConfig(config);
        }
        return connect.call(SERVICE, "OpenSession", b.build(), OpenSessionResponse.parser());
    }

    /**
     * 세션 안에서 교정한다. 요청의 사용자 사전·옵션은 세션을 열 때 정한 값이 쓰인다.
     *
     * @param text 교정할 글
     * @param sessionId {@link #openSession} 이 돌려준 세션 ID
     * @return 교정 결과. {@code session_revision} 이 함께 온다.
     * @throws BareunException 호출 실패. 세션이 닫혔거나 수명이 다했으면
     *         {@link ConnectCode#NOT_FOUND}(메시지 머리에 SESSION_NOT_FOUND·SESSION_EXPIRED).
     */
    public CorrectErrorResponse correctError(String text, String sessionId) {
        return correctError(CorrectErrorRequest.newBuilder()
                .setDocument(LanguageServiceClient.document(text))
                .setEncodingType(EncodingType.UTF16)
                .setSessionId(sessionId)
                .build());
    }

    /**
     * 세션을 닫고 서버가 물고 있는 상태를 정리한다. 마지막 호출 뒤 10분이 지나거나 열린 지
     * 1시간이 지나면 서버가 알아서 닫지만, 닫으면 동시 세션 수 상한을 바로 돌려받는다.
     *
     * @param sessionId 닫을 세션 ID
     * @return 닫을 때의 세션 개정 번호
     * @throws BareunException 호출 실패
     */
    public CloseSessionResponse closeSession(String sessionId) {
        return connect.call(SERVICE, "CloseSession",
                CloseSessionRequest.newBuilder().setSessionId(sessionId).build(),
                CloseSessionResponse.parser());
    }

    /**
     * 세션의 변경 사건을 가져온다. 단건 교정으로 앞 호출의 답을 받은 뒤, 뒤 호출 때문에 세션이
     * 바뀌었는지({@code session_revision} 이 올랐는지) 알아보는 길이다.
     *
     * @param sessionId 세션 ID
     * @param sinceRevision 이 개정 번호보다 뒤의 사건만 받는다. 처음부터 받으려면 0.
     * @return 현재 개정 번호와 사건 목록
     * @throws BareunException 호출 실패
     */
    public GetSessionEventsResponse getSessionEvents(String sessionId, long sinceRevision) {
        return connect.call(SERVICE, "GetSessionEvents",
                GetSessionEventsRequest.newBuilder().setSessionId(sessionId)
                        .setSinceRevision(sinceRevision).build(),
                GetSessionEventsResponse.parser());
    }
}
