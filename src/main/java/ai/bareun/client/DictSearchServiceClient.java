package ai.bareun.client;

import java.util.List;

import ai.bareun.protos.DictSearchAnchor;
import ai.bareun.protos.SearchDictRequest;
import ai.bareun.protos.SearchDictResponse;

/**
 * 우리말샘 사전 자소(음소) 검색 서비스({@code bareun.DictSearchService}).
 *
 * <p>완성형 한글로는 "초성이 ㅅ 이고 종성이 ㄴ 인 음절" 같은 조건을 쓸 수 없다.
 * 서버가 슬롯 패턴을 NFD 정규식으로 바꿔 사전을 훑는다.
 *
 * <p>패턴 문법 요약
 * <ul>
 *   <li>{@code 다} 같은 음절은 그 음절 그대로</li>
 *   <li>{@code {초/중/종}} 은 한 음절의 자소 조건. 비우거나 {@code .} 이면 아무거나</li>
 *   <li>종성 자리의 {@code -} 는 받침 없음, {@code +} 는 받침 있음</li>
 *   <li>{@code *} 는 음절 0개 이상, {@code ?} 는 음절 정확히 1개</li>
 * </ul>
 * 예: {@code "{ㅅ//ㄴ}다"} 는 신다, {@code "*{//ㅎ}다"} 는 낳다·넣다·놓다.
 *
 * <p>맞춤법 교정(rev) 빌드의 서버에서만 동작한다.
 */
public final class DictSearchServiceClient {

    static final String SERVICE = "bareun.DictSearchService";

    private final ConnectClient connect;

    DictSearchServiceClient(ConnectClient connect) {
        this.connect = connect;
    }

    /**
     * 자소 패턴으로 표제어를 찾는다.
     *
     * @param request 검색 요청
     * @return 검색 결과
     * @throws BareunException 호출 실패
     */
    public SearchDictResponse searchDict(SearchDictRequest request) {
        return connect.call(SERVICE, "SearchDict", request, SearchDictResponse.parser());
    }

    /**
     * 자소 패턴으로 표제어를 찾는다.
     *
     * @param pattern 자소 슬롯 패턴
     * @param anchor 패턴이 표제어의 어느 위치에 걸리는지
     * @param pos 품사 필터. 우리말샘 표기("동사"·"명사") 또는 별칭("용언"·"체언"). 없으면 빈 목록.
     * @param limit 최대 표제어 수. 0 이면 서버 기본값 100, 상한 1000.
     * @return 검색 결과
     * @throws BareunException 호출 실패
     */
    public SearchDictResponse searchDict(String pattern, DictSearchAnchor anchor,
            List<String> pos, int limit) {
        SearchDictRequest.Builder b = SearchDictRequest.newBuilder()
                .setPattern(pattern)
                .setAnchor(anchor)
                .setLimit(limit);
        if (pos != null && !pos.isEmpty()) {
            b.addAllPos(pos);
        }
        return searchDict(b.build());
    }

    /**
     * 자소 패턴에 걸린 표제어만 돌려준다.
     *
     * @param pattern 자소 슬롯 패턴
     * @param anchor 패턴이 걸리는 위치
     * @param limit 최대 표제어 수
     * @return 표제어 목록
     * @throws BareunException 호출 실패
     */
    public List<String> words(String pattern, DictSearchAnchor anchor, int limit) {
        return searchDict(pattern, anchor, List.of(), limit)
                .getEntriesList().stream()
                .map(e -> e.getWord())
                .toList();
    }
}
