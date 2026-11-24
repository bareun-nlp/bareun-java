# bareun-java

바른(bareun) 한국어 형태소 분석·맞춤법 교정 서버의 자바 클라이언트입니다.

바른에 대해서는 [bareun.ai](https://bareun.ai) 를 보세요.

## 설치

```xml
<dependency>
  <groupId>ai.bareun.tagger</groupId>
  <artifactId>bareun-client</artifactId>
  <version>2.1.0</version>
</dependency>
```

자바 17 이상이 필요합니다. 런타임 의존은 `protobuf-java` 하나뿐입니다.

## 서버 준비

1. [bareun.ai](https://bareun.ai) 에서 API 키를 발급받습니다.
2. 서버를 실행합니다. [설치 안내](https://docs.bareun.ai/install/overview/)

```
docker pull bareunai/bareun:latest
```

교정과 사전 검색은 맞춤법 교정(rev) 빌드의 서버에서만 동작합니다.

## 형태소 분석

```java
BareunClient client = BareunClient.builder()
        .host("localhost").port(5656)
        .apiKey("koba-...")
        .build();

Tagger tagger = new Tagger(client);
Tagged t = tagger.tag("아버지가 방에 들어가신다.");

t.pos();     // [아버지/NNG, 가/JKS, 방/NNG, 에/JKB, 들어가/VV, 시/EP, ㄴ다/EF, ./SF]
t.morphs();  // [아버지, 가, 방, 에, 들어가, 시, ㄴ다, .]
t.nouns();   // [아버지, 방]
t.verbs();   // [들어가]
```

공개 서비스에 붙을 때는 TLS 를 켭니다.

```java
BareunClient.builder().baseUrl("https://api.bareun.ai").apiKey("koba-...").build();
```

### 위치 정보

요청은 UTF-16 오프셋으로 보냅니다. 자바 문자열이 UTF-16 이라, 서버가 준 위치를
`substring` 에 그대로 넣을 수 있습니다.

```java
var span = t.sentences().get(0).getTokens(0).getText();
text.substring(span.getBeginOffset(), span.getBeginOffset() + span.getLength());
// "아버지가"
```

### 사용자 사전

```java
Tagger tagger = new Tagger(client, List.of("mydict"));
```

사전은 `client.customDictionary()` 로 만들고 지웁니다. 여럿을 주면 앞에 온 것이 우선합니다.

## 동형이의어 의미 구분 (WSD)

같은 글자가 여러 뜻을 가질 때 어느 뜻인지 골라 줍니다. 서버에 WSD 모델이 실려 있어야
하고, 추론이 한 번 더 돌아 느려집니다.

```java
for (Tagged.SenseEntry s : tagger.tag("나는 밤에 밤을 먹었다.", true).senses()) {
    System.out.println(s.morph() + "/" + s.tag() + " " + s.senseNo() + " " + s.meaning());
}
// 밤/NNG 2 밤나무의 열매. ...
// 먹/VV 2 음식 따위를 입을 통하여 뱃속에 들여보내다.
```

조사·어미처럼 의미를 갖지 않는 형태소에는 원래 붙지 않으므로, 대부분의 형태소는
결과에 나오지 않습니다.

## 맞춤법 교정

```java
Corrector c = new Corrector(client);

c.correct("이거 안되요. 학교에 갔읍니다.");
// "이거 안되요. 학교에 갔습니다."

for (Corrector.Change ch : c.changes("이거 안되요. 학교에 갔읍니다.")) {
    System.out.println(ch.origin() + " → " + ch.revised() + " (" + ch.category() + ")");
}
// 갔읍니다. → 갔습니다. (STANDARD)
```

중첩 교정이나 도움말까지 보려면 `c.raw(text)` 로 서버 응답을 그대로 받습니다.

## 우리말샘 사전 자소 검색

완성형 한글로는 "초성이 ㅅ 이고 종성이 ㄴ 인 음절" 같은 조건을 쓸 수 없습니다.
자소 슬롯 패턴으로 찾습니다.

```java
client.dictSearch().words("{ㅅ//ㄴ}다", DictSearchAnchor.DICT_SEARCH_ANCHOR_WORD, 10);
// [신다]

client.dictSearch().words("아지", DictSearchAnchor.DICT_SEARCH_ANCHOR_SUFFIX, 5);
// [아지, 가아지, 강아지, 개아지, 갱아지]
```

| 표기 | 뜻 |
| --- | --- |
| `다` | 그 음절 그대로 |
| `{초/중/종}` | 한 음절의 자소 조건. 비우거나 `.` 이면 아무거나 |
| `-` (종성 자리) | 받침 없음 |
| `+` (종성 자리) | 받침 있음 |
| `*` | 음절 0개 이상 |
| `?` | 음절 정확히 1개 |

## 긴 글 (9,000자 넘는 요청)

공개 서비스(`api.bareun.ai`)는 한 요청의 글이 **9,000자를 넘으면 거절하지 않고 앞 5,000자
안팎만 처리**합니다. 문장 끝, 없으면 공백에서 끊고(앞 500자 안에서 찾습니다), 그것도 없으면
글자 중간에서 끊습니다. 9,000자까지는 전부 처리합니다. 자체 설치한 서버는 기본으로 자르지 않습니다.

버린 부분은 응답의 잘림 정보로 알려 줍니다. 잘리지 않은 응답에는 이 정보가 없습니다.
결과의 위치(offset)는 잘렸어도 요청 글의 처음 기준 그대로입니다.

```java
Tagged t = tagger.tag(longText);
if (t.truncated()) {
    TruncationInfo info = t.truncation().orElseThrow();
    info.originalChars();      // 9001   요청 글 전체의 글자 수
    info.processedChars();     // 4991   처리한 글자 수(앞부분)
    info.droppedChars();       // 4010   버린 글자 수
    info.droppedBeginOffset(); // 4991   버린 구간이 시작하는 위치(String.substring 에 그대로)
    info.message();            // 안내 문구
    info.cutMidWord();         // true 면 글자 중간에서 잘라 마지막 어절을 믿기 어렵음

    // 처리하지 않은 뒷부분은 원문에서 이어서 다시 보냅니다.
    String rest = longText.substring(info.droppedBeginOffset());
}
```

교정은 `correct()` 가 앞부분만 교정한 문자열을 돌려주므로, 긴 글에는 잘림 정보를 함께 주는
`correctWithTruncation()` 을 씁니다.

```java
Corrector.Result r = corrector.correctWithTruncation(longText);
r.revised();     // 교정된 앞부분
r.truncation();  // Optional<TruncationInfo>
```

문장 목록(`tagBatch`)은 앞쪽 문장부터 처리합니다. 응답은 늘 요청과 1:1 이고, 처리하지 않은
문장은 빈 결과로 자리만 둡니다.

```java
TaggedBatch b = tagger.tagBatch(sentences, false);
b.truncated();                    // 앞쪽 문장만 처리했는지
b.droppedBeginSentenceIndex();    // 처리하지 않은 부분이 시작하는 문장 번호(0부터)
b.notProcessed(i);                // i 번째 문장이 전혀 처리되지 않았는지
```

기존 메서드(`tag`, `tagAll`, `correct`, `changes`)는 그대로입니다. 긴 글을 보내도 예외가
나지 않고 앞부분 결과만 돌아오니, 긴 글을 다룬다면 잘림 정보를 확인하세요.
서버 쪽 설명은 [공식 문서](https://docs.bareun.ai)를 보세요.

## 오류 처리

호출이 실패하면 `BareunException` 이 나오고, 종류는 `ConnectCode` 로 가릅니다.

```java
try {
    tagger.tag("문장");
} catch (BareunException e) {
    switch (e.getCode()) {
        case PERMISSION_DENIED -> System.err.println("API 키를 확인하세요");
        case UNAVAILABLE       -> System.err.println("서버 주소·기동 상태를 확인하세요");
        case UNIMPLEMENTED     -> System.err.println("이 서버는 그 기능을 제공하지 않습니다");
        default                -> System.err.println(e.getMessage());
    }
}
```

| 코드 | 언제 |
| --- | --- |
| `UNAVAILABLE` | 서버에 닿지 못함 (주소 오타·미기동·방화벽·타임아웃) |
| `PERMISSION_DENIED` | API 키가 유효하지 않거나 라이선스 만료 |
| `UNIMPLEMENTED` | 그 서버가 제공하지 않는 서비스 (교정 빌드가 아님) |
| `INTERNAL` | 서버 내부 오류, 또는 응답을 해석하지 못함 |

## 감싸지 않은 API

이 라이브러리가 아직 감싸지 않은 메서드는 저수준 클라이언트로 부릅니다.

```java
client.connect().call("bareun.LanguageService", "AnalyzeSyntax", request,
        AnalyzeSyntaxResponse.parser());
```

스트리밍 교정(`StreamCorrectError`)은 아직 지원하지 않습니다. Connect 의 스트리밍은
바디를 프레이밍해야 해서 단항과 전송 방식이 다릅니다.

## 어떻게 붙는가

Connect 의 단항 호출은 평범한 HTTP POST 입니다.

```
POST /bareun.LanguageService/AnalyzeSyntax
Content-Type: application/proto
api-key: koba-...
<직렬화된 요청 메시지>
```

그래서 gRPC 스택 없이 JDK 의 `java.net.http.HttpClient` 만으로 구현했습니다.
1.x 는 gRPC 1.21.0(2019) 에 묶여 있었고 grpc-netty-shaded·guava·opencensus 등
20여 개를 함께 들여왔습니다. 지금은 `protobuf-java` 하나입니다.

## 개발

```bash
mvn test                                   # 단위 테스트
BAREUN_API_KEY=koba-... mvn test -Dtest=LiveServerIT   # 서버가 있어야 도는 통합 테스트
```

글자 수 기준을 건 서버(운영 서비스처럼 9,000자)에서는 `BAREUN_TRUNCATION_LIMIT=9000` 을 함께 주면
긴 글 시험까지 돕니다.

통합 테스트는 `BAREUN_API_KEY` 가 없으면 통째로 건너뜁니다.
서버 주소는 `BAREUN_HOST`(기본 localhost)·`BAREUN_PORT`(기본 5656)로 줍니다.

## 라이선스

BSD 3-Clause
