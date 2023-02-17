package ai.bareun.tagger;

import ai.bareun.protos.CustomDictionary;
import ai.bareun.protos.CustomDictionaryMeta;
import ai.bareun.protos.CustomDictionaryServiceGrpc;
import ai.bareun.protos.DictSet;
import ai.bareun.protos.GetCustomDictionaryListResponse;
import ai.bareun.protos.GetCustomDictionaryRequest;
import ai.bareun.protos.GetCustomDictionaryResponse;
import ai.bareun.protos.RemoveCustomDictionariesRequest;
import ai.bareun.protos.RemoveCustomDictionariesResponse;
import ai.bareun.protos.UpdateCustomDictionaryRequest;
import ai.bareun.protos.UpdateCustomDictionaryResponse;
import ai.bareun.protos.DictType;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.CallOptions;

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

    public static DictSet build_dict_set(String domain, String name, Set<String> dict_set) {
        DictSet.Builder builder = DictSet.newBuilder();

        builder.setName(domain + "-" + name)
                .setType(DictType.WORD_LIST);

        for (String v : dict_set) {
            builder.putItems(v, 1);
        }
        return builder.build();

    }

    public CustomDictionaryServiceClient(String api_key) {
        super(api_key);
    }

    public CustomDictionaryServiceClient(String host, String api_key) {
        super(host, api_key);
    }

    public CustomDictionaryServiceClient(String host, int port, String api_key) {
        super(host, port, api_key);
    }

    public CustomDictionaryServiceClient(Host host, String api_key) {
        super(host, api_key);
    }

    public CustomDictionaryServiceClient(ManagedChannel channel, String api_key) {
        super(channel, api_key);
    }

    public List<CustomDictionaryMeta> get_list() {
        GetCustomDictionaryListResponse res = AccessController
                .doPrivileged((PrivilegedAction<GetCustomDictionaryListResponse>) () -> {
                    GetCustomDictionaryListResponse response = null;
                    try {
                        if (client == null)
                            client = CustomDictionaryServiceGrpc.newBlockingStub(loadChannel(api_key));
                        com.google.protobuf.Empty req = com.google.protobuf.Empty.getDefaultInstance();
                        CallOptions.Key<String> metaDataKey = CallOptions.Key.create("api-key");
                        response = client.withOption(metaDataKey, api_key).getCustomDictionaryList(req);
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
        GetCustomDictionaryResponse res = AccessController
                .doPrivileged((PrivilegedAction<GetCustomDictionaryResponse>) () -> {
                    GetCustomDictionaryResponse response = null;
                    try {
                        if (client == null)
                            client = CustomDictionaryServiceGrpc.newBlockingStub(loadChannel(api_key));

                        GetCustomDictionaryRequest.Builder builder = GetCustomDictionaryRequest.newBuilder();
                        GetCustomDictionaryRequest request = builder.setDomainName(domain).build();

                        CallOptions.Key<String> metaDataKey = CallOptions.Key.create("api-key");
                        response = client.withOption(metaDataKey, api_key).getCustomDictionary(request);
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

    public Boolean update(String domain, Set<String> np, Set<String> cp, Set<String> cp_caret, Set<String> vv,
            Set<String> va) {
        UpdateCustomDictionaryResponse res = AccessController
                .doPrivileged((PrivilegedAction<UpdateCustomDictionaryResponse>) () -> {
                    UpdateCustomDictionaryResponse response = null;
                    try {
                        if (client == null)
                            client = CustomDictionaryServiceGrpc.newBlockingStub(loadChannel(api_key));

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

                        CallOptions.Key<String> metaDataKey = CallOptions.Key.create("api-key");
                        response = client.withOption(metaDataKey, api_key).updateCustomDictionary(request);

                    } catch (StatusRuntimeException e) {
                        LOGGER.warning(e.getMessage());
                        return null;
                    } finally {
                        shutdownChannel();
                    }
                    return response;
                });
        return res != null && res.getUpdatedDomainName().equals(domain);
    }

    public List<String> remove_all() {
        RemoveCustomDictionariesResponse res = AccessController
                .doPrivileged((PrivilegedAction<RemoveCustomDictionariesResponse>) () -> {
                    RemoveCustomDictionariesResponse response = null;
                    try {
                        if (client == null)
                            client = CustomDictionaryServiceGrpc.newBlockingStub(loadChannel(api_key));

                        RemoveCustomDictionariesRequest.Builder builder = RemoveCustomDictionariesRequest.newBuilder();
                        RemoveCustomDictionariesRequest request = builder.setAll(true)
                                .build();
                        CallOptions.Key<String> metaDataKey = CallOptions.Key.create("api-key");
                        response = client.withOption(metaDataKey, api_key).removeCustomDictionaries(request);

                    } catch (StatusRuntimeException e) {
                        LOGGER.warning(e.getMessage());
                        return null;
                    } finally {
                        shutdownChannel();
                    }
                    return response;
                });
        return res != null ? new ArrayList<String>(res.getDeletedDomainNamesMap().keySet()) : null;
    }

    public List<String> remove(List<String> domains) {
        RemoveCustomDictionariesResponse res = AccessController
                .doPrivileged((PrivilegedAction<RemoveCustomDictionariesResponse>) () -> {
                    RemoveCustomDictionariesResponse response = null;
                    try {
                        if (client == null)
                            client = CustomDictionaryServiceGrpc.newBlockingStub(loadChannel(api_key));

                        RemoveCustomDictionariesRequest.Builder builder = RemoveCustomDictionariesRequest.newBuilder();
                        RemoveCustomDictionariesRequest request = builder.setAll(false)
                                .addAllDomainNames(domains)
                                .build();
                        CallOptions.Key<String> metaDataKey = CallOptions.Key.create("api-key");
                        response = client.withOption(metaDataKey, api_key).removeCustomDictionaries(request);

                    } catch (StatusRuntimeException e) {
                        LOGGER.warning(e.getMessage());
                        return null;
                    } finally {
                        shutdownChannel();
                    }
                    return response;
                });
        return res != null ? new ArrayList<String>(res.getDeletedDomainNamesMap().keySet()) : null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ai.bareun.protos.nlp.ClientBase#shutdownChannel()
     */
    @Override
    public void shutdownChannel() {
        super.shutdownChannel();
        client = null;
    }
}
