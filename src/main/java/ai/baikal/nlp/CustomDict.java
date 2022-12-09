package ai.baikal.nlp;

import baikal.ai.CustomDictionary;
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
    protected Set<String> cp_set = new HashSet<String>();
    protected Set<String> np_set = new HashSet<String>();
    protected Set<String> cp_caret_set = new HashSet<String>();

    public static Set<String> read_dict_file(String fn) {
        Set<String> ret = new HashSet<String>();

        try {
            BufferedReader inFile = new BufferedReader(new FileReader(fn));

            String sLine = null;
            while( (sLine = inFile.readLine()) != null ) {
                if( sLine.startsWith("#") ) continue;
                sLine = sLine.trim();
                if( sLine.length() > 0 )
                    ret.add(sLine);
            }
            inFile.close();
        } catch(FileNotFoundException e) {
            e.printStackTrace();
            LOGGER.warning(e.getMessage());
        } catch(IOException e) {
            e.printStackTrace();
            LOGGER.warning(e.getMessage());    
        }
        return ret;
    }

    public CustomDict(String domain) {
        super();
        _init(domain);
    }

    public CustomDict(String domain, String host) {
        super(host);  
        _init(domain);      
    }

    public CustomDict(String domain, String host, int port) {
        super(host, port);
        _init(domain);
    }

    public CustomDict(String domain, Host host) {
        super(host);
        _init(domain);
    }

    private void _init(String domain) {
        if( domain == null || domain.isEmpty() ) 
            throw new NullPointerException();
        
        this.domain = domain;
    }


    public void read_np_set_from_file(String fn) {
        np_set = read_dict_file(fn);
    }
    public void read_cp_set_from_file(String fn) {
        cp_set = read_dict_file(fn);
    }
    public void read_cp_caret_set_from_file(String fn) {
        cp_caret_set = read_dict_file(fn);
    }

    public void copy_np_set( Set<String> dict_set ) {
        np_set = dict_set;
    }

    public void copy_cp_set( Set<String> dict_set ) {
        cp_set = dict_set;
    }

    public void copy_cp_caret_set( Set<String> dict_set ) {
        cp_caret_set = dict_set;
    }

    public Boolean update() {
        return super.update(domain, np_set, cp_set, cp_caret_set);
    } 

    public CustomDictionary get() {
        return super.get(domain);
    }

    public void load() {
        CustomDictionary d = get();

        np_set = d.getNpSet().getItemsMap().keySet();
        cp_set = d.getCpSet().getItemsMap().keySet();
        cp_caret_set = d.getCpCaretSet().getItemsMap().keySet();

    }

    public List<String> clear() {
        np_set.clear();
        cp_set.clear();
        cp_caret_set.clear();

        List<String> domains = new ArrayList<>();
        domains.add(domain);
        return remove( domains );
    }
}
