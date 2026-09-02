package ai.bareun.client;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * 바른 서버 접속의 진입점.
 *
 * <p>서비스별 클라이언트를 여기서 얻는다. 인스턴스 하나를 만들어 두고 공유하면 된다
 * (스레드 안전하고, 내부 {@link HttpClient} 가 연결을 재사용한다).
 *
 * <pre>{@code
 * BareunClient client = BareunClient.builder()
 *         .host("localhost").port(5656)
 *         .apiKey("koba-...")
 *         .build();
 *
 * Tagger tagger = new Tagger(client);
 * List<String> nouns = tagger.tag("아버지가 방에 들어가신다.").nouns();
 * }</pre>
 *
 * <p>교정과 사전 검색은 맞춤법 교정(rev) 빌드의 서버에서만 동작한다. 그렇지 않은
 * 서버에 요청하면 {@link ConnectCode#UNIMPLEMENTED} 로 떨어진다.
 */
public final class BareunClient {

    private final ConnectClient connect;
    private final LanguageServiceClient language;
    private final RevisionServiceClient revision;
    private final CustomDictionaryServiceClient customDict;
    private final DictSearchServiceClient dictSearch;

    private BareunClient(ConnectClient connect) {
        this.connect = connect;
        this.language = new LanguageServiceClient(connect);
        this.revision = new RevisionServiceClient(connect);
        this.customDict = new CustomDictionaryServiceClient(connect);
        this.dictSearch = new DictSearchServiceClient(connect);
    }

    /** @return 형태소 분석 서비스 */
    public LanguageServiceClient language() {
        return language;
    }

    /** @return 맞춤법 교정 서비스 (rev 빌드 전용) */
    public RevisionServiceClient revision() {
        return revision;
    }

    /** @return 사용자 사전 서비스 */
    public CustomDictionaryServiceClient customDictionary() {
        return customDict;
    }

    /** @return 우리말샘 사전 검색 서비스 (rev 빌드 전용) */
    public DictSearchServiceClient dictSearch() {
        return dictSearch;
    }

    /** @return 저수준 Connect 클라이언트. 이 라이브러리가 아직 감싸지 않은 메서드를 부를 때 쓴다. */
    public ConnectClient connect() {
        return connect;
    }

    /** @return 새 빌더 */
    public static Builder builder() {
        return new Builder();
    }

    /** {@link BareunClient} 빌더. */
    public static final class Builder {
        private String host = "localhost";
        private int port = 5656;
        private boolean useTls = false;
        private String baseUrl;
        private String apiKey = "";
        private Duration timeout = Duration.ofSeconds(30);
        private HttpClient http;

        private Builder() {
        }

        /**
         * 서버 호스트 이름이나 IP. URL 이 아니라 이름만 준다.
         *
         * @param host 예: {@code localhost}, {@code nlp.bareun.ai}
         * @return this
         */
        public Builder host(String host) {
            this.host = host;
            return this;
        }

        /**
         * 서버 포트.
         *
         * @param port 네이티브 설치본은 5658, 도커는 5656 이 관례다
         * @return this
         */
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * TLS 사용 여부. 공개 서비스(api.bareun.ai:443)에 붙을 때 켠다.
         *
         * @param useTls true 면 https
         * @return this
         */
        public Builder useTls(boolean useTls) {
            this.useTls = useTls;
            return this;
        }

        /**
         * 기준 주소를 통째로 지정한다. 주면 host·port·useTls 보다 우선한다.
         *
         * @param baseUrl 예: {@code https://api.bareun.ai}
         * @return this
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * API 키.
         *
         * @param apiKey bareun.ai 에서 발급받은 키
         * @return this
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * 요청 하나의 제한 시간. 기본 30초.
         *
         * <p>긴 문서를 교정하면 30초를 넘길 수 있다. 그런 용도라면 늘린다.
         *
         * @param timeout 제한 시간
         * @return this
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * 쓸 {@link HttpClient} 를 직접 준다. 프록시·인증·커넥션 풀을 이미 맞춰 둔
         * 것을 그대로 쓰고 싶을 때 사용한다.
         *
         * @param http HttpClient
         * @return this
         */
        public Builder httpClient(HttpClient http) {
            this.http = http;
            return this;
        }

        /**
         * @return 만들어진 클라이언트
         * @throws IllegalArgumentException API 키가 비어 있으면
         */
        public BareunClient build() {
            if (apiKey == null || apiKey.isBlank()) {
                // 키 없이 만들면 첫 호출에서 permission_denied 로 떨어지는데, 그때는
                // 원인이 설정 누락인지 키 오류인지 구분이 안 된다. 여기서 막는다.
                throw new IllegalArgumentException("apiKey 는 비어 있을 수 없습니다.");
            }
            String url = (baseUrl != null && !baseUrl.isBlank())
                    ? baseUrl
                    : (useTls ? "https://" : "http://") + host + ":" + port;

            HttpClient client = (http != null) ? http
                    : HttpClient.newBuilder()
                            .connectTimeout(timeout)
                            .version(HttpClient.Version.HTTP_1_1)
                            .build();

            return new BareunClient(new ConnectClient(url, apiKey, timeout, client));
        }
    }
}
