package ai.bareun.tagger;

import java.util.List;

import ai.bareun.client.BareunClient;
import ai.bareun.client.BareunException;
import ai.bareun.client.ConnectCode;
import ai.bareun.protos.CorrectErrorResponse;
import ai.bareun.protos.OpenSessionResponse;
import ai.bareun.protos.SessionEvent;

/**
 * 긴 문서를 여러 번에 나눠 교정하는 세션.
 *
 * <p>세션 안의 요청들은 한 문서로 이어진 것으로 다뤄진다. 앞 문단에서 본 미등록 단어와 문맥,
 * 사용자 사전, 옵션이 뒤 문단에 이어진다. 그래서 긴 글은 세션을 열고 문단 단위로 보내는 것이
 * 권장 방식이다. 공개 서비스는 한 요청이 5,000자를 넘는 부분을 처리하지 않으니(세션 안의
 * 각 요청에도 같다) 조각은 그 안에서 나눈다.
 * 지금 기준은 5,000자이며 서비스 운영 상황에 따라 바뀔 수 있다.
 * 숫자를 코드에 박지 말고 응답의 잘림 정보로 판단한다.
 *
 * <pre>{@code
 * try (CorrectionSession s = new Corrector(client).openSession()) {
 *     for (String chunk : chunks) {          // 문단·장 단위로 나눈 조각
 *         out.append(s.correct(chunk));
 *     }
 * }
 * }</pre>
 *
 * <p>사용자 사전과 교정기 옵션은 세션을 열 때 정해지고 닫힐 때까지 바뀌지 않는다. 세션은 연
 * API 키로만 쓸 수 있다. 마지막 호출 뒤 10분이 지나거나 열린 지 1시간이 지나면 서버가 닫는다.
 * 닫힌 세션으로 부르면 {@link ConnectCode#NOT_FOUND} 가 나오니({@link #isLost}) 새로 열어 이어간다.
 *
 * <p>{@link #close()} 는 한 번만 서버를 부르고, 닫는 데 실패해도 예외를 던지지 않는다(서버가
 * 어차피 수명이 다하면 닫는다). 스레드 안전성은 {@link BareunClient} 를 따른다.
 */
public final class CorrectionSession implements AutoCloseable {

    private final BareunClient client;
    private final String sessionId;
    private final int idleTimeoutSeconds;
    private final int maxLifetimeSeconds;
    private volatile long revision;
    private volatile boolean closed;

    /**
     * @param client 쓸 클라이언트
     * @param opened 서버가 열어 준 세션
     */
    CorrectionSession(BareunClient client, OpenSessionResponse opened) {
        this.client = client;
        this.sessionId = opened.getSessionId();
        this.revision = opened.getSessionRevision();
        this.idleTimeoutSeconds = opened.getIdleTimeoutSeconds();
        this.maxLifetimeSeconds = opened.getMaxLifetimeSeconds();
    }

    /** @return 세션 ID */
    public String id() {
        return sessionId;
    }

    /** @return 마지막으로 본 세션 개정 번호. 세션이 새 단어를 확정할 때마다 오른다. */
    public long revision() {
        return revision;
    }

    /** @return 마지막 호출 뒤 이 시간(초)이 지나면 서버가 세션을 닫는다. */
    public int idleTimeoutSeconds() {
        return idleTimeoutSeconds;
    }

    /** @return 열린 지 이 시간(초)이 지나면 서버가 세션을 닫는다. */
    public int maxLifetimeSeconds() {
        return maxLifetimeSeconds;
    }

    /**
     * 이 세션으로 한 조각을 교정한다.
     *
     * @param text 교정할 조각. 서버 기준 길이(공개 서비스 5,000자)를 넘기지 않게 나눠 보낸다.
     * @return 교정된 조각
     * @throws BareunException 호출 실패. 세션이 사라졌으면 {@link #isLost}.
     */
    public String correct(String text) {
        return raw(text).getRevised();
    }

    /**
     * 이 세션으로 한 조각을 교정하고 서버 응답을 그대로 받는다.
     *
     * @param text 교정할 조각
     * @return 서버 응답. {@code getSessionRevision()} 이 이 응답을 만든 시점의 세션 개정 번호다.
     * @throws BareunException 호출 실패
     */
    public CorrectErrorResponse raw(String text) {
        CorrectErrorResponse res = client.revision().correctError(text, sessionId);
        if (res.hasSessionRevision()) {
            revision = res.getSessionRevision();
        }
        return res;
    }

    /**
     * 처음부터 지금까지 세션이 새로 확정한 단어 사건을 받는다.
     *
     * @param sinceRevision 이 개정 번호보다 뒤의 사건만. 전부 받으려면 0.
     * @return 사건 목록
     * @throws BareunException 호출 실패
     */
    public List<SessionEvent> events(long sinceRevision) {
        var res = client.revision().getSessionEvents(sessionId, sinceRevision);
        revision = res.getSessionRevision();
        return res.getEventsList();
    }

    /**
     * 세션을 닫는다. 이미 닫았으면 아무것도 하지 않는다. 닫기 호출이 실패해도 던지지 않는다.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            client.revision().closeSession(sessionId);
        } catch (BareunException e) {
            // 서버가 수명이 다한 세션을 알아서 정리한다. 닫기 실패로 호출한 쪽을 막지 않는다.
        }
    }

    /**
     * 예외가 "세션이 닫혔거나 수명이 다했다" 는 뜻인지 본다. 그러면 새 세션을 열고 필요한
     * 앞부분을 다시 보내 이어간다.
     *
     * @param e 호출에서 나온 예외
     * @return 서버가 모르는 세션이면 true
     */
    public static boolean isLost(BareunException e) {
        return e.getCode() == ConnectCode.NOT_FOUND;
    }
}
