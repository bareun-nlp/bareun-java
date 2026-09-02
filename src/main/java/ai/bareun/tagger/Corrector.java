package ai.bareun.tagger;

import java.util.ArrayList;
import java.util.List;

import ai.bareun.client.BareunClient;
import ai.bareun.client.BareunException;
import ai.bareun.client.ConnectCode;
import ai.bareun.protos.CorrectErrorResponse;
import ai.bareun.protos.RevisedBlock;
import ai.bareun.protos.Revision;

/**
 * 맞춤법·띄어쓰기 교정을 짧게 쓰는 진입점.
 *
 * <pre>{@code
 * Corrector c = new Corrector(client);
 * c.correct("이거 안되요. 학교에 갔읍니다.");   // "이거 안되요. 학교에 갔습니다."
 * c.changes("이거 안되요. 학교에 갔읍니다.");   // [갔읍니다. → 갔습니다. (STANDARD)]
 * }</pre>
 *
 * <p>교정은 맞춤법 교정(rev) 빌드의 서버에서만 동작한다. 아닌 서버에 요청하면
 * {@link BareunException} 이 {@link ConnectCode#UNIMPLEMENTED} 로 나온다.
 */
public final class Corrector {

    private final BareunClient client;
    private final List<String> customDictNames;

    /**
     * @param client 쓸 클라이언트
     */
    public Corrector(BareunClient client) {
        this(client, List.of());
    }

    /**
     * @param client 쓸 클라이언트
     * @param customDictNames 교정에 함께 쓸 사용자 사전 이름들
     */
    public Corrector(BareunClient client, List<String> customDictNames) {
        this.client = client;
        this.customDictNames = List.copyOf(customDictNames);
    }

    /**
     * 문장을 교정한다.
     *
     * @param text 교정할 문장
     * @return 교정된 문장. 고칠 것이 없으면 원문과 같다.
     * @throws BareunException 호출 실패
     */
    public String correct(String text) {
        return client.revision().correctError(text, customDictNames, null).getRevised();
    }

    /**
     * 무엇이 어떻게 바뀌었는지 목록으로 받는다.
     *
     * <p>한 어절에 여러 교정이 겹치면 서버가 블럭을 중첩해 보낸다({@code nested}).
     * 여기서는 겉의 대표 교정만 담는다 — 중첩까지 보려면 {@link #raw(String)} 을 쓴다.
     *
     * @param text 교정할 문장
     * @return 교정 내역
     * @throws BareunException 호출 실패
     */
    public List<Change> changes(String text) {
        CorrectErrorResponse res = client.revision().correctError(text, customDictNames, null);
        List<Change> out = new ArrayList<>();
        for (RevisedBlock b : res.getRevisedBlocksList()) {
            // revisions 는 후보 목록이다. 대표 교정은 block.revised 이고,
            // 분류·도움말은 그 대표가 나온 근거인 첫 후보에서 가져온다.
            String category = "";
            String helpId = "";
            if (b.getRevisionsCount() > 0) {
                Revision first = b.getRevisions(0);
                category = first.getCategory().name();
                helpId = first.getHelpId();
            }
            out.add(new Change(b.getOrigin().getContent(), b.getRevised(), category, helpId,
                    b.getOrigin().getBeginOffset(), b.getOrigin().getLength()));
        }
        return out;
    }

    /**
     * 서버 응답을 그대로 받는다. 중첩 블럭·도움말·문장 단위 결과가 필요할 때 쓴다.
     *
     * @param text 교정할 문장
     * @return 서버 응답
     * @throws BareunException 호출 실패
     */
    public CorrectErrorResponse raw(String text) {
        return client.revision().correctError(text, customDictNames, null);
    }

    /**
     * 교정 하나.
     *
     * @param origin 원문 조각
     * @param revised 교정된 조각
     * @param category 교정 분류. 예: SPACING, STANDARD, TYPO
     * @param helpId 도움말 식별자
     * @param beginOffset 원문에서의 시작 위치. 자바 문자열 기준(UTF-16)이라
     *        {@link String#substring(int, int)} 에 그대로 넣을 수 있다.
     * @param length 원문 조각의 길이
     */
    public record Change(String origin, String revised, String category, String helpId,
            int beginOffset, int length) {
    }
}
