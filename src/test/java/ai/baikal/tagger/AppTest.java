package ai.baikal.tagger;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        logger.info(str);
    }
}
