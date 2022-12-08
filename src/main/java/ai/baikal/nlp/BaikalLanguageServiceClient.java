package ai.baikal.nlp;

import baikal.ai.AnalyzeSyntaxRequest;
import baikal.ai.AnalyzeSyntaxResponse;
import baikal.ai.Document;
import baikal.ai.EncodingType;
import baikal.ai.LanguageServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

// import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
// import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

public class BaikalLanguageServiceClient {
    LanguageServiceGrpc.LanguageServiceBlockingStub client;
    private final static Logger LOGGER = Logger.getGlobal();
    
    protected ManagedChannel channel;
    protected AnalyzeSyntaxResponse lastResponse;

    public final static int DEF_PORT = 5656;
    public final static String DEF_ADDRESS = "nlp.baikal.ai"; // "10.3.8.44";

    public static class Host {
        public String host;
        public int port;
        public Host(String host, int port) {
            this.host = host;
            this.port = port;
        }
        public Host(String host) {
            if( host.indexOf(":") >= 0 ) {
                String[] arr = host.split(":");
                this.host = arr[0];
                this.port = Integer.parseInt( arr[1] );
            }
            else {
                this.host = host;
                this.port = DEF_PORT;
            }
        }
    }
    protected Host host;

    public BaikalLanguageServiceClient() {
        this(DEF_ADDRESS, DEF_PORT)   ;
    }

    public BaikalLanguageServiceClient(String host) {
        this.host = new Host(host);
    }

    public BaikalLanguageServiceClient(String host, int port) {
        this.host = new Host(host, port);
    }

    public BaikalLanguageServiceClient(Host host) {
        this.host = host;
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
                channel = ManagedChannelBuilder.forAddress(host.host, host.port).usePlaintext().build();
                client = LanguageServiceGrpc.newBlockingStub(channel);
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
                channel.shutdown();
            }
            return response;
        });
        return lastResponse;
    }

    public AnalyzeSyntaxResponse get() { return lastResponse; }

    public void shutdownChannel() {
        channel.shutdown();
    }

    public static String toJson(MessageOrBuilder obj) {
        String jsonString = "";
        
        if( obj == null ) return jsonString;
        try {
            jsonString = JsonFormat.printer().includingDefaultValueFields().print(obj);            
        } catch(Exception e) {
            e.printStackTrace();
        }
        return jsonString;  
    }

    public String toJson() {
        return toJson(lastResponse);       
    } 
}
