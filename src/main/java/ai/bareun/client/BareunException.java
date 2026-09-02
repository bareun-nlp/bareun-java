package ai.bareun.client;

/**
 * 바른 서버 호출이 실패했을 때 던지는 예외.
 *
 * <p>실패 종류를 {@link ConnectCode} 로 구분할 수 있다. 예전 클라이언트는 접속 실패와
 * 인증 실패와 서버 오류가 모두 같은 모양으로 떨어져 호출한 쪽에서 가릴 수 없었다.
 *
 * <pre>{@code
 * try {
 *     tagger.tag("문장");
 * } catch (BareunException e) {
 *     if (e.getCode() == ConnectCode.PERMISSION_DENIED) {
 *         // API 키를 확인한다
 *     } else if (e.getCode() == ConnectCode.UNAVAILABLE) {
 *         // 서버 주소·기동 상태를 확인한다
 *     }
 * }
 * }</pre>
 */
public class BareunException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ConnectCode code;
    private final int httpStatus;

    /**
     * @param code Connect 오류 코드
     * @param message 사람이 읽는 오류 메시지. 서버가 보낸 것이면 그대로 싣는다.
     * @param httpStatus HTTP 상태 코드. 접속 자체가 안 된 경우는 0.
     * @param cause 원인 예외. 없으면 null.
     */
    public BareunException(ConnectCode code, String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    /** @return Connect 오류 코드 */
    public ConnectCode getCode() {
        return code;
    }

    /** @return HTTP 상태 코드. 서버에 닿지도 못한 경우는 0 이다. */
    public int getHttpStatus() {
        return httpStatus;
    }
}
