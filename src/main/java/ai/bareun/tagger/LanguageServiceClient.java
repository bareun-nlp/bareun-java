package ai.bareun.tagger;

import ai.bareun.protos.AnalyzeSyntaxRequest;
import ai.bareun.protos.AnalyzeSyntaxResponse;
import ai.bareun.protos.Document;
import ai.bareun.protos.EncodingType;
import ai.bareun.protos.LanguageServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.CallOptions;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LanguageServiceClient extends ClientBase {
    LanguageServiceGrpc.LanguageServiceBlockingStub client;
    private final static Logger LOGGER = Logger.getGlobal();

    protected AnalyzeSyntaxResponse lastResponse;

    public LanguageServiceClient() {
        super();
    }

    public LanguageServiceClient(String host) {
        super(host);
    }

    public LanguageServiceClient(String host, int port) {
        super(host, port);
    }

    public LanguageServiceClient(Host host) {
        super(host);
    }

    public LanguageServiceClient(ManagedChannel channel) {
        super(channel);
    }

    /**
     * @param text
     * @return AnalyzeSyntaxResponse
     */
    public AnalyzeSyntaxResponse analyze_syntax(String text, String api_key) {
        return analyze_syntax(text, "", api_key);
    }

    /**
     * @param text
     * @param domain
     * @return AnalyzeSyntaxResponse
     */
    public AnalyzeSyntaxResponse analyze_syntax(String text, String domain, String api_key) {
        return analyze_syntax(text, domain, true, api_key);
    }

    /**
     * @param text
     * @param domain
     * @param auto_split
     * @return AnalyzeSyntaxResponse
     */
    public AnalyzeSyntaxResponse analyze_syntax(String text, String domain, Boolean auto_split, String api_key) {
        lastResponse = null;
        if (text == null || text.isEmpty())
            return lastResponse;
        lastResponse = AccessController.doPrivileged((PrivilegedAction<AnalyzeSyntaxResponse>) () -> {
            AnalyzeSyntaxResponse response = null;
            try {
                if (client == null)
                    client = LanguageServiceGrpc.newBlockingStub(loadChannel(api_key));
                LOGGER.setLevel(Level.INFO);
                LOGGER.info("analyze - '" + text + "'");
                Document document = Document.newBuilder().setContent(text).setLanguage("ko-KR").build();

                ai.bareun.protos.AnalyzeSyntaxRequest.Builder builder = AnalyzeSyntaxRequest.newBuilder();
                if (domain != null && !domain.isEmpty())
                    builder.setCustomDomain(domain);
                AnalyzeSyntaxRequest request = builder.setDocument(document)
                        .setEncodingType(EncodingType.UTF32)
                        .build();
                CallOptions.Key<String> metaDataKey = CallOptions.Key.create("api-key");
                response = client.withOption(metaDataKey, api_key).analyzeSyntax(request);
            } catch (StatusRuntimeException e) {
                LOGGER.warning(e.getMessage());
                LOGGER.warning(text);
                return null;
            } finally {
                shutdownChannel();
            }
            return response;
        });
        return lastResponse;
    }

    public AnalyzeSyntaxResponse get() {
        return lastResponse;
    }

    /**
     * @return String
     */
    public String toJson() {
        return toJson(lastResponse);
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
