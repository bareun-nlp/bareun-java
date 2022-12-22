package ai.bareun.tagger;

import bareun.ai.AnalyzeSyntaxResponse;
import bareun.ai.*;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;


public class Tagged {
    AnalyzeSyntaxResponse r;
    String phrase;
    
    public Tagged() {
        this(null, null);
    }

    public Tagged(String phrase, AnalyzeSyntaxResponse res) {
        this.phrase = phrase;
        this.r = res;

        if( r == null ) {
            r = AnalyzeSyntaxResponse.getDefaultInstance();
            this.phrase = "";
        }
    }

    
    /** 
     * @return AnalyzeSyntaxResponse
     */
    public AnalyzeSyntaxResponse msg() {
        return r;
    }

    
    /** 
     * @return List<Sentence>
     */
    public List<Sentence> sentences() {
        return r.getSentencesList();
    }

    
    /** 
     * @return String
     */
    public String as_json_str() {
        return LanguageServiceClient.toJson(r);
    }

    /*
    public List<List<String[]>> as_json() {
        List<List<String[]>> ret = new ArrayList<>();
        for(Sentence s: r.getSentencesList()) 
            for( Token t : s.getTokensList() ) { 
                List<String[]> token = new ArrayList<>();  
                ret.add(token);
                for( Morpheme m : t.getMorphemesList()) {                    
                    token.add( _pos_array(m, true) );
                }
            }
        return ret;
    }
     */

    public void print_as_json( ) {
        print_as_json(System.out);
    }

    
    /** 
     * @param out
     */
    public void print_as_json( PrintStream out ) {
        out.println(as_json_str());
    }

    
    /** 
     * @param m
     * @param detail
     * @return String
     */
    static String _pos_str(Morpheme m, Boolean detail ) {            
        String ret =  String.format("%s/%s", m.getText().getContent(), m.getTag().name());

        if( detail ) {
            String p = m.getProbability() > 0 ? String.format(":%5.3f", m.getProbability()) : "";
            String oov = m.getOutOfVocab().getNumber() != 0 ? ("#" + m.getOutOfVocab().name()) : "";
            ret += p+oov ;
        }
        return ret;
    }

    
    /** 
     * @param m
     * @param detail
     * @return String[]
     */
    static String[] _pos_array(Morpheme m, Boolean detail ) {
        if( !detail ) 
            return new String[] {m.getText().getContent(), m.getTag().name()};
        
        return new String[] {
            m.getText().getContent(), 
            m.getTag().name(),
            m.getProbability() > 0 ? String.format("%5.3f", m.getProbability()) : "",
            m.getOutOfVocab().getNumber() != 0 ? m.getOutOfVocab().name() : ""
        };
    }

    
    /** 
     * @param join
     * @param detail
     * @return List
     */
    public List<List<?>> pos_structured(Boolean join, Boolean detail) {
        List<List<?>> ret = new ArrayList<>();
        for(Sentence s: r.getSentencesList()) 
            for( Token t : s.getTokensList() ) { 
                List<?> token = new ArrayList<>();
                List<String[]> _t_arr = new ArrayList<String[]>();
                List<String> _t_str = new ArrayList<String>();
                if( join ) 
                    token = _t_str;
                else 
                    token = _t_arr;  
                ret.add(token);
                for( Morpheme m : t.getMorphemesList()) {         
                    if( join ) 
                        _t_str.add( _pos_str(m, detail) );
                    else
                        _t_arr.add( _pos_array(m, detail) );;
                }
            }
        return ret;
    }

    
    /** 
     * @return List<String>
     */
    public List<String> pos() {
        return pos(false);
    }

    
    /** 
     * @param detail
     * @return List<String>
     */
    public List<String> pos(Boolean detail ) {
        List<String> ret = new ArrayList<String>();
        List<?> r = pos(  true, detail );
        for(Object o: r) {
            ret.add(o.toString());
        }
        return ret;
    }

    
    /** 
     * @return List<Token>
     */
    public List<Token> tokens() {
        List<Token> ret = new ArrayList<Token>();

        for(Sentence s: r.getSentencesList()) {
            for( Token t : s.getTokensList() ) 
                ret.add(t);
        }
        return ret;
    }

    
    /** 
     * @param t
     * @param join
     * @param detail
     * @return 
     */
    public List<?> pos( Token t, Boolean join, Boolean detail ) {
        
        if( join ) {
            List<String> ret = new ArrayList<String>();
            for( Morpheme m : t.getMorphemesList()) 
                ret.add( _pos_str(m, detail) );
            return ret;
        }
        else {
            List<String[]> ret = new ArrayList<String[]>();
            for( Morpheme m : t.getMorphemesList()) 
                ret.add( _pos_array(m, detail) );
            return ret;
        }        
    }

    
    /** 
     * @param join
     * @param detail
     * @return List
     */
    public List<?> pos(Boolean join, Boolean detail ) {
        if( join ) {
            List<String> ret = new ArrayList<String>();
            for(Sentence s: r.getSentencesList()) 
                for( Token t : s.getTokensList() ) 
                    for( Morpheme m : t.getMorphemesList()) 
                        ret.add( _pos_str(m, detail) );
                    
            return ret;
        }
        else {
            List<String[]> ret = new ArrayList<String[]>();
            for(Sentence s: r.getSentencesList()) 
                for( Token t : s.getTokensList() )
                    for( Morpheme m : t.getMorphemesList()) 
                        ret.add( _pos_array(m, detail) );
            return ret;
        }
    }

    
    /** 
     * @param flatten
     * @param join
     * @param detail
     * @return List
     */
    public List<?> pos(Boolean  flatten, Boolean join, Boolean detail ) {
        if( flatten )
            return pos(join, detail);
        else 
            return pos_structured(join, detail);
    }

    
    /** 
     * @return List<String>
     */
    public List<String> morphs() {
        List<String> ret = new ArrayList<String>();
        for(Sentence s: r.getSentencesList()) 
            for( Token t : s.getTokensList() ) 
                for( Morpheme m : t.getMorphemesList()) 
                    ret.add( m.getText().getContent() );
        return ret;
    }

    
    /** 
     * @param arr
     * @param val
     * @return Integer
     */
    private static <T> Integer indexOf(T[] arr, T val) {
        for(int i=0; i<arr.length; i++) {
            if( arr[i].equals(val) ) return i;
        }
        return -1;
    }

    
    /** 
     * @return List<String>
     */
    public List<String> nouns() {
        Morpheme.Tag[] noun_tags = new  Morpheme.Tag[] {Morpheme.Tag.NNP, Morpheme.Tag.NNG, Morpheme.Tag.NP, Morpheme.Tag.NNB } ;
        List<String> ret = new ArrayList<String>();
        for(Sentence s: r.getSentencesList()) 
            for( Token t : s.getTokensList() ) 
                for( Morpheme m : t.getMorphemesList()) 
                    if( indexOf(noun_tags, m.getTag()) >= 0 )
                        ret.add( m.getText().getContent() );
        return ret;
    }

    
    /** 
     * @return List<String>
     */
    public List<String> verbs() {        
        List<String> ret = new ArrayList<String>();
        for(Sentence s: r.getSentencesList()) 
            for( Token t : s.getTokensList() ) 
                for( Morpheme m : t.getMorphemesList()) 
                    if( m.getTag() == Morpheme.Tag.VV )
                        ret.add( m.getText().getContent() );
        return ret;
    }
}

