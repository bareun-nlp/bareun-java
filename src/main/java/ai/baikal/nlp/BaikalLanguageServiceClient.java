package ai.baikal.nlp;

import baikal.ai.AnalyzeSyntaxRequest;
import baikal.ai.AnalyzeSyntaxResponse;
import baikal.ai.Document;
import baikal.ai.EncodingType;
import baikal.ai.LanguageServiceGrpc;
import io.grpc.StatusRuntimeException;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BaikalLanguageServiceClient extends ClientBase {
    LanguageServiceGrpc.LanguageServiceBlockingStub client;
    private final static Logger LOGGER = Logger.getGlobal();

    protected AnalyzeSyntaxResponse lastResponse; 

    public BaikalLanguageServiceClient() {
        super();
    }

    public BaikalLanguageServiceClient(String host) {
        super(host);        
    }

    public BaikalLanguageServiceClient(String host, int port) {
        super(host, port);
    }

    public BaikalLanguageServiceClient(Host host) {
        super(host);
    }

    public AnalyzeSyntaxResponse analyze_syntax(String text) {
        return analyze_syntax(text, "");
    }

    public AnalyzeSyntaxResponse analyze_syntax(String text, String domain) {
        return analyze_syntax(text, domain, false);
    }

    public AnalyzeSyntaxResponse analyze_syntax(String text, String domain, Boolean auto_split) {   
        lastResponse = null;
        if( text == null || text.isEmpty() ) return lastResponse;
        lastResponse = AccessController.doPrivileged((PrivilegedAction<AnalyzeSyntaxResponse>) () -> {
            AnalyzeSyntaxResponse response = null;
            try {
                if( client == null )
                    client = LanguageServiceGrpc.newBlockingStub(loadChannel());
                LOGGER.setLevel(Level.INFO);
                LOGGER.info("analyze - '"+text+"'");
                Document document = Document.newBuilder().setContent(text).setLanguage("ko-KR").build();

                baikal.ai.AnalyzeSyntaxRequest.Builder builder = AnalyzeSyntaxRequest.newBuilder();
                if( domain != null && !domain.isEmpty())
                    builder.setCustomDomain(domain);
                AnalyzeSyntaxRequest request = builder.setDocument(document)
                                                .setEncodingType(EncodingType.UTF32)
                                                .build();
            
                response = client.analyzeSyntax(request);

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

    public AnalyzeSyntaxResponse get() { return lastResponse; }

    

    public String toJson() {
        return toJson(lastResponse);       
    }

    /* (non-Javadoc)
     * @see ai.baikal.nlp.ClientBase#shutdownChannel()
     */
    @Override
    public void shutdownChannel() {
        super.shutdownChannel();
        client = null;
    }
}
