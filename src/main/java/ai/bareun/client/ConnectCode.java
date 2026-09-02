package ai.bareun.client;

/**
 * Connect 프로토콜이 정의한 오류 코드.
 *
 * <p>서버는 실패할 때 HTTP 상태와 함께 {@code {"code":"permission_denied","message":"..."}}
 * 모양의 JSON 을 돌려준다. 그 {@code code} 문자열을 그대로 옮긴 것이다. 문자열 비교
 * 대신 이 열거형으로 다루면 호출한 쪽에서 실패 종류를 갈라 처리할 수 있다.
 *
 * @see <a href="https://connectrpc.com/docs/protocol#error-codes">Connect error codes</a>
 */
public enum ConnectCode {
    CANCELED,
    UNKNOWN,
    INVALID_ARGUMENT,
    DEADLINE_EXCEEDED,
    NOT_FOUND,
    ALREADY_EXISTS,
    /** API 키가 유효하지 않거나 라이선스가 만료됐을 때 온다. */
    PERMISSION_DENIED,
    RESOURCE_EXHAUSTED,
    FAILED_PRECONDITION,
    ABORTED,
    OUT_OF_RANGE,
    /** 그 서버가 제공하지 않는 서비스다. 교정·사전 검색은 rev 빌드에만 있다. */
    UNIMPLEMENTED,
    INTERNAL,
    UNAVAILABLE,
    DATA_LOSS,
    UNAUTHENTICATED;

    /**
     * 서버가 보낸 코드 문자열을 열거형으로 바꾼다.
     *
     * <p>모르는 코드가 와도 예외를 던지지 않고 {@link #UNKNOWN} 으로 둔다. 서버가
     * 나중에 코드를 추가했을 때 옛 클라이언트가 오류 처리 도중에 다시 죽는 것이
     * 더 나쁘기 때문이다.
     *
     * @param s 서버가 보낸 코드 문자열. null 이거나 모르는 값이면 UNKNOWN.
     * @return 대응하는 코드
     */
    public static ConnectCode from(String s) {
        if (s == null) {
            return UNKNOWN;
        }
        try {
            return valueOf(s.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
