package ai.bareun.tagger;

import bareun.ai.AnalyzeSyntaxRequest;
import bareun.ai.AnalyzeSyntaxResponse;
import bareun.ai.Document;
import bareun.ai.EncodingType;
import bareun.ai.LanguageServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;

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

    public LanguageServiceClient( ManagedChannel channel) {
        super(channel);
    }

    
    /** 
     * @param text
     * @return AnalyzeSyntaxResponse
     */
    public AnalyzeSyntaxResponse analyze_syntax(String text) {
        return analyze_syntax(text, "");
    }

    
    /** 
     * @param text
     * @param domain
     * @return AnalyzeSyntaxResponse
     */
    public AnalyzeSyntaxResponse analyze_syntax(String text, String domain) {
        return analyze_syntax(text, domain, false);
    }

    
    /** 
     * @param text
     * @param domain
     * @param auto_split
     * @return AnalyzeSyntaxResponse
     */
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

                bareun.ai.AnalyzeSyntaxRequest.Builder builder = AnalyzeSyntaxRequest.newBuilder();
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

    

    
    /** 
     * @return String
     */
    public String toJson() {
        return toJson(lastResponse);       
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
