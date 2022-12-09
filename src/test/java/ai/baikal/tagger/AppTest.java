package ai.baikal.tagger;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import ai.baikal.nlp.*;
import baikal.ai.AnalyzeSyntaxRequest;
import baikal.ai.AnalyzeSyntaxResponse;
import baikal.ai.Document;
import baikal.ai.LanguageServiceGrpc;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    private static Logger logger = LoggerFactory.getLogger(AppTest.class.getSimpleName());
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {

        BaikalLanguageServiceClient conn = new ai.baikal.nlp.BaikalLanguageServiceClient();
        AnalyzeSyntaxResponse response =  conn.analyze_syntax("아버지가 방에 들어가신다.");
        String str = conn.toJson();
        assertTrue( !str.isEmpty() );
        //logger.info(str);
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
        Gson gson = new Gson(); 
        logger.info(String.format("flatten = %B, join = %B, detail = %B", flatten , join , detail));
        logger.info(gson.toJson(ret));


        flatten = true;
        join = true;
        detail = false;
        ret = tag.pos(flatten, join, detail);
        assertTrue(!ret.isEmpty());
        logger.info(String.format("flatten = %B, join = %B, detail = %B", flatten , join , detail));
        logger.info(gson.toJson(ret));

        flatten = true;
        join = false;
        detail = true;
        ret = tag.pos(flatten, join, detail);
        assertTrue(!ret.isEmpty());
        logger.info(String.format("flatten = %B, join = %B, detail = %B", flatten , join , detail));
        logger.info(gson.toJson(ret));


        flatten = false;
        join = true;
        detail = true;
        ret = tag.pos(flatten, join, detail);
        assertTrue(!ret.isEmpty());
        logger.info(String.format("flatten = %B, join = %B, detail = %B", flatten , join , detail));
        logger.info(gson.toJson(ret));

        flatten = false;
        join = false;
        detail = true;
        ret = tag.pos(flatten, join, detail);
        assertTrue(!ret.isEmpty());
        logger.info(String.format("flatten = %B, join = %B, detail = %B", flatten , join , detail));
        logger.info(gson.toJson(ret));
    }

    
    @Test
    public void morphs() {
        Tagged tag = new Tagger("localhost").tag(TestString);
        Gson gson = new Gson(); 

        List<String> ret = tag.morphs();
        assertTrue(!ret.isEmpty());
        logger.info("morphs()");
        logger.info(gson.toJson(ret));

        ret = tag.nouns();
        assertTrue(!ret.isEmpty());
        logger.info("nouns()");
        logger.info(gson.toJson(ret));

        ret = tag.verbs();
        assertTrue(!ret.isEmpty());
        logger.info("verbs()");
        logger.info(gson.toJson(ret));
    }

    
}
