package ai.bareun.tagger;

import java.util.List;
import java.util.Optional;

/**
 * 문장 목록 분석({@link Tagger#tagBatch})의 결과.
 *
 * <p>{@link #items()} 는 요청한 문장과 1:1 이다. 서버가 글이 길다고 앞쪽만 처리하면
 * 처리하지 않은 문장은 빈 결과로 자리만 남는다({@link #notProcessed(int)} 로 가른다).
 *
 * @param items 문장별 결과. 입력 순서와 같다.
 * @param truncation 잘림 정보. 잘리지 않았으면 빈 값.
 * @param droppedBeginSentenceIndex 처리하지 않은 부분이 시작하는 문장의 번호(0부터).
 *        그 문장은 앞쪽만 처리됐을 수 있고({@link TruncationInfo#droppedBeginOffset()} 이
 *        그 문장 안의 위치), 그 뒤 문장은 모두 처리되지 않았다. 잘리지 않았으면 -1.
 */
public record TaggedBatch(List<Tagged> items, Optional<TruncationInfo> truncation,
        int droppedBeginSentenceIndex) {

    /**
     * 그 문장이 전혀 처리되지 않았는지 본다. 서버는 처리하지 않은 문장을 빈 문장 하나로 채워
     * 보내므로 {@link Tagged#isEmpty()} 로는 알 수 없다.
     *
     * <p>{@code droppedBeginSentenceIndex} 번째 문장은 앞쪽만 처리됐을 수 있어 false 를 돌려준다.
     *
     * @param index 문장 번호(0부터)
     * @return 처리되지 않은 문장이면 true. 잘리지 않았으면 늘 false.
     */
    public boolean notProcessed(int index) {
        return truncation.isPresent() && index > droppedBeginSentenceIndex;
    }

    /** @return 앞쪽 문장만 처리했으면 true */
    public boolean truncated() {
        return truncation.isPresent();
    }
}
