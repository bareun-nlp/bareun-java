package ai.bareun.tagger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.grpc.ManagedChannel;

public class Tagger {
    protected LanguageServiceClient.Host host;
    protected String domain;
    protected String api_key;
    protected LanguageServiceClient client;
    protected Map<String, CustomDict> custom_dicts = new HashMap<String, CustomDict>();

    public Tagger(String api_key) {
        this(LanguageServiceClient.DEF_ADDRESS, api_key);
    }

    public Tagger(String host, String api_key) {
        this(host, "", api_key);
    }

    public Tagger(String host, String domain, String api_key) {
        this(new LanguageServiceClient.Host(host), domain, api_key);
    }

    public Tagger(String host, int port, String api_key) {
        this(host, port, "", api_key);
    }

    public Tagger(String host, int port, String domain, String api_key) {
        this(new LanguageServiceClient.Host(host, port), domain, api_key);
    }

    public Tagger(LanguageServiceClient.Host host, String domain, String api_key) {
        this.host = host;
        this.domain = domain;
        this.api_key = api_key;
        client = new LanguageServiceClient(host);
    }

    public Tagger(ManagedChannel channel, String api_key) {
        this(channel, "", api_key);
    }

    public Tagger(ManagedChannel channel, String domain, String api_key) {
        this.domain = domain;
        this.api_key = api_key;
        client = new LanguageServiceClient(channel);
    }

    /**
     * @param domain
     * @return Tagger
     */
    public Tagger set_domain(String domain) {
        this.domain = domain;
        return this;
    }

    /**
     * @param domain
     * @return CustomDict
     * @throws NullPointerException
     */
    public CustomDict custom_dict(String domain) throws NullPointerException {
        if (custom_dicts.get(domain) != null)
            return custom_dicts.get(domain);
        CustomDict dict = new CustomDict(domain, host, this.api_key);
        custom_dicts.put(domain, dict);
        return dict;
    }

    /**
     * @param phrase
     * @return Tagged
     */
    public Tagged tag(String phrase) {
        return tag(phrase, true);
    }

    /**
     * @param phrase
     * @param auto_split
     * @return Tagged
     */
    public Tagged tag(String phrase, Boolean auto_split) {
        if (phrase == null || phrase.isEmpty())
            return new Tagged();

        return new Tagged(phrase, client.analyze_syntax(phrase, domain, auto_split, this.api_key));
    }

    /**
     * @param phrase
     * @return Tagged
     */
    public Tagged tags(List<String> phrase) {
        if (phrase == null || phrase.isEmpty())
            return new Tagged();

        String p = String.join("\n", phrase);

        return tag(p);
    }

    /**
     * @param phrase
     * @return List
     */
    public List<?> pos(String phrase) {
        return pos(phrase, false);
    }

    /**
     * @param phrase
     * @param join
     * @return List
     */
    public List<?> pos(String phrase, Boolean join) {
        return pos(phrase, join, false);
    }

    /**
     * @param phrase
     * @param join
     * @param detail
     * @return List
     */
    public List<?> pos(String phrase, Boolean join, Boolean detail) {
        return tag(phrase).pos(join, detail);
    }

    /**
     * @param phrase
     * @param flatten
     * @param join
     * @param detail
     * @return List
     */
    public List<?> pos(String phrase, Boolean flatten, Boolean join, Boolean detail) {
        return tag(phrase).pos(flatten, join, detail);
    }

    /**
     * @param phrase
     * @return List<String>
     */
    public List<String> morphs(String phrase) {
        return tag(phrase).morphs();
    }

    /**
     * @param phrase
     * @return List<String>
     */
    public List<String> nouns(String phrase) {
        return tag(phrase).nouns();
    }

    /**
     * @param phrase
     * @return List<String>
     */
    public List<String> verbs(String phrase) {
        return tag(phrase).nouns();
    }
}
