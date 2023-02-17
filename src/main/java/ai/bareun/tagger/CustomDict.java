package ai.bareun.tagger;

import ai.bareun.protos.CustomDictionary;
import io.grpc.ManagedChannel;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class CustomDict extends CustomDictionaryServiceClient {
    private final static Logger LOGGER = Logger.getGlobal();
    protected String domain;
    protected String api_key;
    protected Set<String> cp_set = new HashSet<String>();
    protected Set<String> np_set = new HashSet<String>();
    protected Set<String> cp_caret_set = new HashSet<String>();
    protected Set<String> vv_set = new HashSet<String>();
    protected Set<String> va_set = new HashSet<String>();

    public static Set<String> read_dict_file(String fn) {
        Set<String> ret = new HashSet<String>();

        try {
            BufferedReader inFile = new BufferedReader(new FileReader(fn));

            String sLine = null;
            while ((sLine = inFile.readLine()) != null) {
                if (sLine.startsWith("#"))
                    continue;
                sLine = sLine.trim();
                if (sLine.length() > 0)
                    ret.add(sLine);
            }
            inFile.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            LOGGER.warning(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.warning(e.getMessage());
        }
        return ret;
    }

    public CustomDict(String domain, String api_key) {
        super();
        _init(domain, api_key);
    }

    public CustomDict(String domain, String host, String api_key) {
        super(host);
        _init(domain, api_key);
    }

    public CustomDict(String domain, String host, int port, String api_key) {
        super(host, port);
        _init(domain, api_key);
    }

    public CustomDict(String domain, Host host, String api_key) {
        super(host);
        _init(domain, api_key);
    }

    public CustomDict(String domain, ManagedChannel channel, String api_key) {
        super(channel);
        _init(domain, api_key);
    }

    private void _init(String domain, String api_key) {
        if (domain == null || domain.isEmpty())
            throw new NullPointerException();

        this.domain = domain;
        this.api_key = api_key;
    }

    public Set<String> getSet(String set_name) {
        switch (set_name) {
            case "np_set":
                return np_set;
            case "cp_set":
                return cp_set;
            case "cp_caret_set":
                return cp_caret_set;
            case "vv_set":
                return vv_set;
            case "va_set":
                return va_set;
            default:
                return null;
        }
    }

    /**
     * @param fn
     * @return Integer
     */
    public Integer read_np_set_from_file(String fn) {
        np_set = read_dict_file(fn);
        return np_set.size();
    }

    /**
     * @param fn
     * @return Integer
     */
    public Integer read_cp_set_from_file(String fn) {
        cp_set = read_dict_file(fn);
        return cp_set.size();
    }

    /**
     * @param fn
     * @return Integer
     */
    public Integer read_cp_caret_set_from_file(String fn) {
        cp_caret_set = read_dict_file(fn);
        return cp_caret_set.size();
    }

    /**
     * @param fn
     * @return Integer
     */
    public Integer read_vv_set_from_file(String fn) {
        vv_set = read_dict_file(fn);
        return vv_set.size();
    }

    /**
     * @param fn
     * @return Integer
     */
    public Integer read_va_set_from_file(String fn) {
        va_set = read_dict_file(fn);
        return va_set.size();
    }

    /**
     * @param dict_set
     */
    public void copy_np_set(Set<String> dict_set) {
        np_set = dict_set;
    }

    /**
     * @param dict_set
     */
    public void copy_cp_set(Set<String> dict_set) {
        cp_set = dict_set;
    }

    /**
     * @param dict_set
     */
    public void copy_cp_caret_set(Set<String> dict_set) {
        cp_caret_set = dict_set;
    }

    /**
     * @param dict_set
     */
    public void copy_vv_set(Set<String> dict_set) {
        vv_set = dict_set;
    }

    /**
     * @param dict_set
     */
    public void copy_va_set(Set<String> dict_set) {
        va_set = dict_set;
    }

    /**
     * @return Boolean
     */
    public Boolean update() {
        return super.update(domain, np_set, cp_set, cp_caret_set, vv_set, va_set, this.api_key);
    }

    /**
     * @return CustomDictionary
     */
    public CustomDictionary get() {
        return super.get(domain, this.api_key);
    }

    public void load() {
        CustomDictionary d = get();

        np_set = d.getNpSet().getItemsMap().keySet();
        cp_set = d.getCpSet().getItemsMap().keySet();
        cp_caret_set = d.getCpCaretSet().getItemsMap().keySet();
        vv_set = d.getVvSet().getItemsMap().keySet();
        va_set = d.getVaSet().getItemsMap().keySet();

    }

    /**
     * @return List<String>
     */
    public List<String> clear() {
        np_set.clear();
        cp_set.clear();
        cp_caret_set.clear();
        vv_set.clear();
        va_set.clear();

        List<String> domains = new ArrayList<>();
        domains.add(domain);
        return remove(domains, this.api_key);
    }
}
