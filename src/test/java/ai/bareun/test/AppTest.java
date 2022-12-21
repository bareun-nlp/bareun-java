package ai.bareun.test;

import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import ai.bareun.tagger.*;
import bareun.ai.AnalyzeSyntaxRequest;
import bareun.ai.AnalyzeSyntaxResponse;
import bareun.ai.CustomDictionary;
import bareun.ai.CustomDictionaryMeta;
import bareun.ai.Document;
import bareun.ai.LanguageServiceGrpc;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    private static Logger logger = LoggerFactory.getLogger(AppTest.class.getSimpleName());

    void log(Object str){
       
        if( str instanceof String ) {
            logger.info(str.toString());
        }  else {
            Gson gson = new Gson();
            logger.info(gson.toJson(str));
        }
  
    }
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {

        LanguageServiceClient conn = new ai.bareun.tagger.LanguageServiceClient("localhost");
        AnalyzeSyntaxResponse response =  conn.analyze_syntax("아버지가 방에 들어가신다.");
        String str = conn.toJson();
        assertTrue( !str.isEmpty() );
        logger.info(str);
    }

    static String TestString = "아버지가 방에 들어가신다.";
    // "윤석열 대통령이 취임 220일차인 오는 15일 대국민 소통의 일환으로 ‘국정과제 점검회의’를 진행한다.\n 취임 후 처음 국민과 대면해 생방송으로 대화를 나눈다는 점에서 취지에 걸맞는 소통이 얼마나 이뤄질지 관심이다.";


    @Test
    public void pos() {
        Tagged tag = new Tagger("localhost").tag(TestString);
        
        List<?> ret;
        Boolean flatten = true, join = true, detail = true;
        ret = tag.pos(flatten, join, detail);
        assertTrue(!ret.isEmpty());
        log(String.format("flatten = %B, join = %B, detail = %B", flatten , join , detail));
        log(ret);


        flatten = true;
        join = true;
        detail = false;
        ret = tag.pos(flatten, join, detail);
        assertTrue(!ret.isEmpty());
        log(String.format("flatten = %B, join = %B, detail = %B", flatten , join , detail));
        log(ret);

        flatten = true;
        join = false;
        detail = true;
        ret = tag.pos(flatten, join, detail);
        assertTrue(!ret.isEmpty());
        log(String.format("flatten = %B, join = %B, detail = %B", flatten , join , detail));
        log(ret);


        flatten = false;
        join = true;
        detail = true;
        ret = tag.pos(flatten, join, detail);
        assertTrue(!ret.isEmpty());
        log(String.format("flatten = %B, join = %B, detail = %B", flatten , join , detail));
        log(ret);

        flatten = false;
        join = false;
        detail = true;
        ret = tag.pos(flatten, join, detail);
        assertTrue(!ret.isEmpty());
        log(String.format("flatten = %B, join = %B, detail = %B", flatten , join , detail));
        log(ret);
    }

    
    @Test
    public void morphs() {
        Tagged tag = new Tagger("localhost").tag(TestString);

        List<String> ret = tag.morphs();
        assertTrue(!ret.isEmpty());
        log("morphs()");
        log(ret);

        ret = tag.nouns();
        assertTrue(!ret.isEmpty());
        log("nouns()");
        log(ret);

        ret = tag.verbs();
        assertTrue(!ret.isEmpty());
        log("verbs()");
        log(ret);
    }

    @Test
    public void testCustomDict() {
        CustomDict dict = new CustomDict("game", "localhost");

       

        log("testCustomDict()");
        log("read file.");

        String curdir = System.getProperty("user.dir");
        Path path = Path.of( curdir, "testdict.txt");
        if( dict.read_np_set_from_file(path.toString()) <= 0 ) {
            log("file io error : " + path.toString());
            return ;
        }
        log(dict.getSet("np_set"));

        Boolean r = dict.update();
        assertTrue(r);

        dict.load();
        log("update and load.");
        log(dict.getSet("np_set"));

        log("get list ");
        List<CustomDictionaryMeta> list = dict.get_list();
        for(CustomDictionaryMeta meta: list) {
            log(meta.getDomainName());
        }
        
    }

    
}
