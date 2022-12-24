package ai.bareun.tagger;

import bareun.ai.CustomDictionary;
import bareun.ai.CustomDictionaryMeta;
import bareun.ai.CustomDictionaryServiceGrpc;
import bareun.ai.DictSet;
import bareun.ai.GetCustomDictionaryListResponse;
import bareun.ai.GetCustomDictionaryRequest;
import bareun.ai.GetCustomDictionaryResponse;
import bareun.ai.RemoveCustomDictionariesRequest;
import bareun.ai.RemoveCustomDictionariesResponse;
import bareun.ai.UpdateCustomDictionaryRequest;
import bareun.ai.UpdateCustomDictionaryResponse;
import bareun.ai.DictType;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;

// import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;


public class CustomDictionaryServiceClient extends ClientBase {
    CustomDictionaryServiceGrpc.CustomDictionaryServiceBlockingStub client;
    private final static Logger LOGGER = Logger.getGlobal();

    public static DictSet build_dict_set(String domain, String name, Set<String> dict_set ) {
        DictSet.Builder builder = DictSet.newBuilder();

        builder.setName(domain + "-" + name)
            .setType(DictType.WORD_LIST);

        for( String v: dict_set) {
            builder.putItems(v, 1);
        }
        return builder.build();

    }

    public CustomDictionaryServiceClient() {
        super();
    }

    public CustomDictionaryServiceClient(String host) {
        super(host);        
    }

    public CustomDictionaryServiceClient(String host, int port) {
        super(host, port);
    }

    public CustomDictionaryServiceClient(Host host) {
        super(host);
    }

    public CustomDictionaryServiceClient(ManagedChannel channel) {
        super(channel);
    }

    public List<CustomDictionaryMeta> get_list() {
        GetCustomDictionaryListResponse res = AccessController.doPrivileged((PrivilegedAction<GetCustomDictionaryListResponse>) () -> {
            GetCustomDictionaryListResponse response = null;
            try {
                if( client == null )
                    client = CustomDictionaryServiceGrpc.newBlockingStub(loadChannel());
                com.google.protobuf.Empty req = com.google.protobuf.Empty.getDefaultInstance();
                response = client.getCustomDictionaryList(req);
            } catch (StatusRuntimeException e) {
                LOGGER.warning(e.getMessage());
                return null;
            } finally {
                shutdownChannel();
            }
            return response;
        });
        return res != null ? res.getDomainDictsList() : null;
    }


    public CustomDictionary get(String domain) {
        GetCustomDictionaryResponse res = AccessController.doPrivileged((PrivilegedAction<GetCustomDictionaryResponse>) () -> {
            GetCustomDictionaryResponse response = null;
            try {
                if( client == null )
                    client = CustomDictionaryServiceGrpc.newBlockingStub(loadChannel());
                
                GetCustomDictionaryRequest.Builder builder = GetCustomDictionaryRequest.newBuilder();
                GetCustomDictionaryRequest request = builder.setDomainName(domain).build();

                response = client.getCustomDictionary(request) ;

            } catch (StatusRuntimeException e) {
                LOGGER.warning(e.getMessage());
                return null;
            } finally {
                shutdownChannel();
            }
            return response;
        });
        return res != null ? res.getDict() : null;
    }

    public Boolean update(String domain, Set<String> np, Set<String> cp, Set<String> cp_caret, Set<String> vv, Set<String> va ) {
        UpdateCustomDictionaryResponse res = AccessController.doPrivileged((PrivilegedAction<UpdateCustomDictionaryResponse>) () -> {
            UpdateCustomDictionaryResponse response = null;
            try {
                if( client == null )
                    client = CustomDictionaryServiceGrpc.newBlockingStub(loadChannel());
                
                UpdateCustomDictionaryRequest.Builder builder = UpdateCustomDictionaryRequest.newBuilder();
                
                CustomDictionary.Builder dict_builder = builder.getDictBuilder();
                dict_builder.setDomainName(domain)
                            .setNpSet(build_dict_set(domain, "np-set", np))
                            .setCpSet(build_dict_set(domain, "cp-set", cp))
                            .setCpCaretSet(build_dict_set(domain, "cp-caret-set", cp_caret))
                            .setVvSet(build_dict_set(domain, "vv-set", vv))
                            .setVaSet(build_dict_set(domain, "va-set", va));
                UpdateCustomDictionaryRequest request = builder.setDomainName(domain)
                                                        .setDict(dict_builder)
                                                        .build();

                response = client.updateCustomDictionary(request);

            } catch (StatusRuntimeException e) {
                LOGGER.warning(e.getMessage());
                return null;
            } finally {
                shutdownChannel();
            }
            return response;
        });
        return res != null && res.getUpdatedDomainName().equals( domain) ;
    }


    public List<String> remove_all() {
        RemoveCustomDictionariesResponse res = AccessController.doPrivileged((PrivilegedAction<RemoveCustomDictionariesResponse>) () -> {
            RemoveCustomDictionariesResponse response = null;
            try {
                if( client == null )
                    client = CustomDictionaryServiceGrpc.newBlockingStub(loadChannel());
                
                RemoveCustomDictionariesRequest.Builder builder = RemoveCustomDictionariesRequest.newBuilder();
                RemoveCustomDictionariesRequest request = builder.setAll(true)
                                                        .build();

                response = client.removeCustomDictionaries(request);

            } catch (StatusRuntimeException e) {
                LOGGER.warning(e.getMessage());
                return null;
            } finally {
                shutdownChannel();
            }
            return response;
        });
        return res != null ? new ArrayList<String>(res.getDeletedDomainNamesMap().keySet())  : null ;
    }


    public List<String> remove(List<String> domains) {
        RemoveCustomDictionariesResponse res = AccessController.doPrivileged((PrivilegedAction<RemoveCustomDictionariesResponse>) () -> {
            RemoveCustomDictionariesResponse response = null;
            try {
                if( client == null )
                    client = CustomDictionaryServiceGrpc.newBlockingStub(loadChannel());
                
                RemoveCustomDictionariesRequest.Builder builder = RemoveCustomDictionariesRequest.newBuilder();
                RemoveCustomDictionariesRequest request = builder.setAll(false)
                                                        .addAllDomainNames(domains)
                                                        .build();

                response = client.removeCustomDictionaries(request);

            } catch (StatusRuntimeException e) {
                LOGGER.warning(e.getMessage());
                return null;
            } finally {
                shutdownChannel();
            }
            return response;
        });
        return res != null ? new ArrayList<String>(res.getDeletedDomainNamesMap().keySet())  : null ;
    }


     /* (non-Javadoc)
     * @see ai.bareun.nlp.ClientBase#shutdownChannel()
     */
    @Override
    public void shutdownChannel() {
        super.shutdownChannel();
        client = null;
    }
}
