package ai.bareun.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;

/**
 * Connect 프로토콜의 단항(unary) 호출을 수행하는 최소 클라이언트.
 *
 * <p>Connect 의 단항 호출은 평범한 HTTP POST 다.
 *
 * <pre>
 *   POST /&lt;proto 패키지&gt;.&lt;서비스&gt;/&lt;메서드&gt;
 *   Content-Type: application/proto
 *   &lt;직렬화된 요청 메시지&gt;
 * </pre>
 *
 * <p>그래서 gRPC 스택 없이 JDK 의 {@link HttpClient} 만으로 구현할 수 있다. 바디를
 * 프레이밍하지 않고 메시지를 그대로 싣는 것이 스트리밍과 다른 점이다 —
 * 이 클래스는 단항만 다룬다.
 *
 * <p>인코딩은 {@code application/proto}(바이너리)를 쓴다. JSON 도 서버가 받지만,
 * 응답의 필드 이름 표기(camelCase)에 의존하지 않아도 되고 크기·속도에서 유리하다.
 *
 * <p>이 클래스는 불변이고 스레드 안전하다. {@link HttpClient} 자체가 스레드 안전하므로
 * 인스턴스 하나를 여러 스레드가 공유해도 된다.
 */
public final class ConnectClient {

    /** Connect 바이너리 인코딩. 서버는 application/json 도 받지만 이쪽을 쓴다. */
    private static final String CONTENT_TYPE = "application/proto";

    private final HttpClient http;
    private final String baseUrl;
    private final String apiKey;
    private final Duration timeout;

    /**
     * @param baseUrl 서버 기준 주소. 예: {@code http://localhost:5656}. 끝 슬래시는 지운다.
     * @param apiKey API 키. 모든 요청의 {@code api-key} 헤더로 실린다.
     * @param timeout 요청 하나의 제한 시간
     * @param http 쓸 HttpClient. 호출자가 프록시·인증 등을 이미 설정한 것을 넘길 수 있다.
     */
    ConnectClient(String baseUrl, String apiKey, Duration timeout, HttpClient http) {
        // 끝 슬래시가 붙으면 경로가 "//bareun.LanguageService/..." 가 되어 404 가 난다.
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl").replaceAll("/+$", "");
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        this.http = Objects.requireNonNull(http, "http");
    }

    /**
     * 단항 호출을 한 번 수행한다.
     *
     * @param service proto 서비스 전체 이름. 예: {@code bareun.LanguageService}
     * @param method 메서드 이름. 예: {@code AnalyzeSyntax}
     * @param request 요청 메시지
     * @param parser 응답 메시지 파서. 보통 {@code Foo.parser()}.
     * @param <T> 응답 메시지 타입
     * @return 파싱된 응답
     * @throws BareunException 접속 실패, 서버 오류, 응답 파싱 실패 모두 이 예외로 나온다.
     *         종류는 {@link BareunException#getCode()} 로 가른다.
     */
    public <T extends Message> T call(String service, String method, Message request, Parser<T> parser) {
        URI uri = URI.create(baseUrl + "/" + service + "/" + method);

        HttpRequest req = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Content-Type", CONTENT_TYPE)
                .header("api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofByteArray(request.toByteArray()))
                .build();

        HttpResponse<byte[]> res;
        try {
            res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            // 주소 오타·서버 미기동·방화벽·타임아웃이 모두 여기로 온다.
            throw new BareunException(ConnectCode.UNAVAILABLE,
                    "바른 서버에 접속하지 못했습니다 (" + uri + "): " + e.getMessage(), 0, e);
        } catch (InterruptedException e) {
            // 인터럽트 상태를 삼키면 호출한 쪽의 취소가 먹지 않는다. 반드시 되살린다.
            Thread.currentThread().interrupt();
            throw new BareunException(ConnectCode.CANCELED, "호출이 중단되었습니다: " + uri, 0, e);
        }

        if (res.statusCode() / 100 != 2) {
            throw toException(res);
        }

        try {
            return parser.parseFrom(res.body());
        } catch (InvalidProtocolBufferException e) {
            // 2xx 인데 파싱이 안 된다면 서버와 클라이언트의 proto 가 어긋난 것이다.
            throw new BareunException(ConnectCode.INTERNAL,
                    "응답을 해석하지 못했습니다. 서버와 클라이언트의 proto 버전이 다를 수 있습니다: "
                            + e.getMessage(), res.statusCode(), e);
        }
    }

    /**
     * 실패 응답을 예외로 바꾼다.
     *
     * <p>Connect 는 오류를 {@code {"code":"...","message":"..."}} JSON 으로 돌려준다.
     * 다만 라우팅 단계에서 떨어지면(경로가 없을 때 등) 그냥 평문이 오므로, 파싱에
     * 실패해도 HTTP 상태로 코드를 짐작해 예외를 만든다 — 오류 처리 도중에 다시
     * 죽지 않게 하는 것이 목적이다.
     *
     * @param res 2xx 가 아닌 응답
     * @return 던질 예외
     */
    private BareunException toException(HttpResponse<byte[]> res) {
        String body = new String(res.body(), StandardCharsets.UTF_8);
        String code = extractJsonString(body, "code");
        String message = extractJsonString(body, "message");

        ConnectCode cc = (code != null) ? ConnectCode.from(code) : fromHttpStatus(res.statusCode());
        if (message == null || message.isEmpty()) {
            message = body.isEmpty()
                    ? ("바른 서버가 HTTP " + res.statusCode() + " 를 돌려주었습니다")
                    : body.strip();
        }
        return new BareunException(cc, message, res.statusCode(), null);
    }

    /**
     * HTTP 상태만으로 Connect 코드를 짐작한다.
     *
     * <p>Connect 규약의 상태 대응을 뒤집은 것이다. JSON 오류 본문이 없을 때만 쓴다.
     *
     * @param status HTTP 상태 코드
     * @return 대응하는 코드
     */
    private static ConnectCode fromHttpStatus(int status) {
        switch (status) {
            case 400: return ConnectCode.INVALID_ARGUMENT;
            case 401: return ConnectCode.UNAUTHENTICATED;
            case 403: return ConnectCode.PERMISSION_DENIED;
            case 404: return ConnectCode.UNIMPLEMENTED;
            case 408: return ConnectCode.DEADLINE_EXCEEDED;
            case 429: return ConnectCode.RESOURCE_EXHAUSTED;
            case 501: return ConnectCode.UNIMPLEMENTED;
            case 502:
            case 503:
            case 504: return ConnectCode.UNAVAILABLE;
            default: return status >= 500 ? ConnectCode.INTERNAL : ConnectCode.UNKNOWN;
        }
    }

    /**
     * 평평한 JSON 오브젝트에서 문자열 필드 하나를 꺼낸다.
     *
     * <p>오류 본문 두 필드를 읽자고 JSON 라이브러리를 의존에 더하지 않으려고 직접 읽는다.
     * Connect 오류 본문은 최상위에 {@code code}·{@code message} 가 있는 단순한 모양이고,
     * 여기서 잘못 읽어도 최악이 "메시지가 덜 예쁘다" 이므로 이 정도로 충분하다.
     * 다만 JSON 이스케이프(따옴표·역슬래시·개행·유니코드 이스케이프)는 풀어야 한국어 메시지가 제대로 보인다.
     *
     * @param json JSON 문자열
     * @param field 꺼낼 필드 이름
     * @return 값. 없으면 null.
     */
    static String extractJsonString(String json, String field) {
        String needle = "\"" + field + "\"";
        int k = json.indexOf(needle);
        if (k < 0) {
            return null;
        }
        int i = json.indexOf(':', k + needle.length());
        if (i < 0) {
            return null;
        }
        i++;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        if (i >= json.length() || json.charAt(i) != '"') {
            return null;
        }
        i++;

        StringBuilder sb = new StringBuilder();
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\' && i + 1 < json.length()) {
                char n = json.charAt(++i);
                switch (n) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'u':
                        if (i + 4 < json.length()) {
                            sb.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16));
                            i += 4;
                        }
                        break;
                    default: sb.append(n); break;
                }
            } else {
                sb.append(c);
            }
            i++;
        }
        // 닫는 따옴표를 못 찾았다 — 잘린 본문이다. 읽은 데까지 돌려준다.
        return sb.toString();
    }
}
