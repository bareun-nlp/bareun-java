package ai.bareun.tagger;

import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ClientBase {
    protected ManagedChannel channel;

    public final static int DEF_PORT = 5656;
    public final static String DEF_ADDRESS = "nlp.bareun.ai"; // "10.3.8.44";

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

    public ClientBase() {
        this(DEF_ADDRESS, DEF_PORT)   ;
    }

    public ClientBase(String host) {
        this.host = new Host(host);
    }

    public ClientBase(String host, int port) {
        this.host = new Host(host, port);
    }

    public ClientBase(Host host) {
        this.host = host;
    }

    public ManagedChannel loadChannel() {
        channel = ManagedChannelBuilder.forAddress(host.host, host.port).usePlaintext().build();
        return channel;
    }


    public void shutdownChannel() {
        channel.shutdown();
        channel = null;
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
}
