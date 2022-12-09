package ai.baikal.nlp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;




public class Tagger {
    protected BaikalLanguageServiceClient.Host host;
    protected String domain;
    protected BaikalLanguageServiceClient client;
    protected Map<String, CustomDict> custom_dicts = new HashMap<String, CustomDict>();
    
    public Tagger() {
        this(BaikalLanguageServiceClient.DEF_ADDRESS);
    }

    public Tagger(String host) {
        this(host, "");
    }

    public Tagger(String host, String domain) {
        this(new BaikalLanguageServiceClient.Host(host), domain);
    }

    public Tagger(String host, int port) {
        this(host, port, "");
    }

    public Tagger(String host, int port, String domain) {
        this(new BaikalLanguageServiceClient.Host(host, port), domain );
    }

    public Tagger(BaikalLanguageServiceClient.Host host, String domain) {
        this.host = host;
        this.domain = domain;
        client = new BaikalLanguageServiceClient(host);
    }



    public Tagger set_domain(String domain) {
        this.domain = domain;
        return this;
    }


    public CustomDict custom_dict(String domain) throws NullPointerException {
        if( custom_dicts.get(domain) != null ) return custom_dicts.get(domain);
        CustomDict dict = new CustomDict(domain, host);
        custom_dicts.put(domain, dict);
        return dict; 
    }
    
    public Tagged tag(String phrase ) {
        return tag(phrase, false);
    }

    public Tagged tag(String phrase, Boolean auto_split ) {
        if( phrase == null || phrase.isEmpty() )
            return new Tagged();
        
        return new Tagged(phrase, client.analyze_syntax(phrase, domain, auto_split));
    }

    public Tagged tags(List<String> phrase ) {
        if( phrase == null || phrase.isEmpty() )
            return new Tagged();
        
        String p = String.join("\n", phrase);

        return tag(p);
    }

    public List<?> pos(String phrase ) {
        return pos(phrase, false);
    }
    public List<?> pos(String phrase, Boolean join ) {
        return pos(phrase, join, false);
    }
    public List<?> pos(String phrase, Boolean join, Boolean detail ) {
        return tag(phrase).pos(join, detail);
    }
    public List<?> pos(String phrase, Boolean flatten, Boolean join, Boolean detail ) {
        return tag(phrase).pos(flatten, join, detail);
    }

    public List<String> morphs(String phrase) {
        return tag(phrase).morphs();
    }

    public List<String> nouns(String phrase) {
        return tag(phrase).nouns();
    }
    
    public List<String> verbs(String phrase) {
        return tag(phrase).nouns();
    }
}
