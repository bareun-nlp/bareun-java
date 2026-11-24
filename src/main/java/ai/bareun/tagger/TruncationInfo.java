package ai.bareun.tagger;

import java.util.Optional;

import ai.bareun.protos.Truncation;

/**
 * 요청 글이 서버 기준보다 길어 앞부분만 처리됐을 때의 안내.
 *
 * <p>서버는 기준(운영 서비스는 5,000자)을 넘는 요청을 거절하지 않고 앞 5,000자 안쪽만
 * 처리한 뒤, 버린 부분을 이 정보로 알린다. 글자 수는 유니코드 코드 포인트 기준이다.
 * 잘리지 않은 응답에는 이 정보가 없으므로 {@link #of(boolean, Truncation)} 이 빈 값을 돌려준다.
 *
 * <p>서버가 자르지 않게 설정돼 있으면(자체 설치 서버 기본) 항상 비어 있다.
 *
 * @param originalChars 요청 글 전체의 글자 수
 * @param processedChars 처리한 글자 수(앞부분)
 * @param droppedChars 처리하지 않고 버린 글자 수
 * @param droppedBeginOffset 버린 구간이 시작하는 위치. 요청의 encoding_type 기준이고, 이
 *        라이브러리는 UTF-16 으로 요청하므로 자바 {@link String#substring(int)} 에 그대로
 *        넣을 수 있다. 문장 목록 호출에서는 {@code droppedBeginSentenceIndex} 번째 문장 안의 위치다.
 * @param message 사람이 읽는 안내 문구(한국어, 괄호 안에 영어)
 * @param cutMidWord 문장 끝이나 공백을 찾지 못해 글자 중간에서 잘랐으면 true.
 *        마지막 어절의 결과를 믿기 어렵다.
 */
public record TruncationInfo(int originalChars, int processedChars, int droppedChars,
        int droppedBeginOffset, String message, boolean cutMidWord) {

    /**
     * 응답의 잘림 정보를 감싼다.
     *
     * @param present 응답에 잘림 정보가 있는지({@code hasTruncation()} 값)
     * @param t 응답의 잘림 정보. {@code present} 가 false 면 쓰지 않는다.
     * @return 잘렸으면 정보, 아니면 빈 값
     */
    static Optional<TruncationInfo> of(boolean present, Truncation t) {
        if (!present) {
            return Optional.empty();
        }
        return Optional.of(new TruncationInfo(t.getOriginalChars(), t.getProcessedChars(),
                t.getDroppedChars(), t.getDroppedBeginOffset(), t.getMessage(),
                t.getCutMidWord()));
    }
}
