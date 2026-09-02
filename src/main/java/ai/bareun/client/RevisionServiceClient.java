package ai.bareun.client;

import java.util.List;

import ai.bareun.protos.CorrectErrorRequest;
import ai.bareun.protos.CorrectErrorResponse;
import ai.bareun.protos.EncodingType;
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
}
