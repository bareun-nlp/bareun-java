package ai.bareun.client;

import java.util.List;

import com.google.protobuf.Empty;

import ai.bareun.protos.CheckConflictRequest;
import ai.bareun.protos.CheckConflictResponse;
import ai.bareun.protos.CustomDictionary;
import ai.bareun.protos.GetCustomDictionaryListResponse;
import ai.bareun.protos.GetCustomDictionaryRequest;
import ai.bareun.protos.GetCustomDictionaryResponse;
import ai.bareun.protos.RemoveCustomDictionariesRequest;
import ai.bareun.protos.RemoveCustomDictionariesResponse;
import ai.bareun.protos.UpdateCustomDictionaryRequest;
import ai.bareun.protos.UpdateCustomDictionaryResponse;

/**
 * 사용자 사전 서비스({@code bareun.CustomDictionaryService}).
 *
 * <p>사전을 만들어 두면 분석·교정 요청에서 이름으로 골라 쓴다
 * ({@code customDictNames}).
 */
public final class CustomDictionaryServiceClient {

    static final String SERVICE = "bareun.CustomDictionaryService";

    private final ConnectClient connect;

    CustomDictionaryServiceClient(ConnectClient connect) {
        this.connect = connect;
    }

    /**
     * 등록된 사전 목록을 가져온다.
     *
     * @return 사전 목록
     * @throws BareunException 호출 실패
     */
    public GetCustomDictionaryListResponse list() {
        return connect.call(SERVICE, "GetCustomDictionaryList",
                Empty.getDefaultInstance(), GetCustomDictionaryListResponse.parser());
    }

    /**
     * 사전 하나를 가져온다.
     *
     * @param domainName 사전 이름
     * @return 사전 내용
     * @throws BareunException 호출 실패
     */
    public GetCustomDictionaryResponse get(String domainName) {
        return connect.call(SERVICE, "GetCustomDictionary",
                GetCustomDictionaryRequest.newBuilder().setDomainName(domainName).build(),
                GetCustomDictionaryResponse.parser());
    }

    /**
     * 사전을 만들거나 덮어쓴다.
     *
     * <p>부분 갱신이 아니라 통째로 바꾼다. 기존 내용에 더하려면 {@link #get(String)} 으로
     * 받아 고쳐서 다시 넣는다.
     *
     * @param domainName 사전 이름
     * @param dict 사전 내용
     * @return 갱신 결과
     * @throws BareunException 호출 실패
     */
    public UpdateCustomDictionaryResponse update(String domainName, CustomDictionary dict) {
        return connect.call(SERVICE, "UpdateCustomDictionary",
                UpdateCustomDictionaryRequest.newBuilder()
                        .setDomainName(domainName)
                        .setDict(dict)
                        .build(),
                UpdateCustomDictionaryResponse.parser());
    }

    /**
     * 사전들을 지운다.
     *
     * @param domainNames 지울 사전 이름들
     * @return 삭제 결과
     * @throws BareunException 호출 실패
     */
    public RemoveCustomDictionariesResponse remove(List<String> domainNames) {
        return connect.call(SERVICE, "RemoveCustomDictionaries",
                RemoveCustomDictionariesRequest.newBuilder()
                        .addAllDomainNames(domainNames)
                        .build(),
                RemoveCustomDictionariesResponse.parser());
    }

    /**
     * 사전들 사이의(그리고 사전 안의) 충돌을 점검한다.
     *
     * @param domainNames 점검할 사전 이름들. 하나만 줘도 그 사전 안의 충돌을 본다.
     * @return 충돌 목록
     * @throws BareunException 호출 실패
     */
    public CheckConflictResponse checkConflict(List<String> domainNames) {
        return connect.call(SERVICE, "CheckConflict",
                CheckConflictRequest.newBuilder().addAllDomainNames(domainNames).build(),
                CheckConflictResponse.parser());
    }
}
