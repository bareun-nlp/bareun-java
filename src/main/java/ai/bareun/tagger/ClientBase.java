package ai.bareun.tagger;

import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.ClientInterceptor;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ForwardingClientCall;
import io.grpc.MethodDescriptor;
import io.grpc.Metadata;
import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

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
            if (host.indexOf(":") >= 0) {
                String[] arr = host.split(":");
                this.host = arr[0];
                this.port = Integer.parseInt(arr[1]);
            } else {
                this.host = host;
                this.port = DEF_PORT;
            }
        }
    }

    protected Host host;

    public ClientBase() {
        this(DEF_ADDRESS, DEF_PORT);
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

    public ClientBase(ManagedChannel channel) {
        this.channel = channel;
    }

    protected class serviceClientInterceptor implements ClientInterceptor {
        String api_key = "";

        serviceClientInterceptor(String api_key) {
            this.api_key = api_key;
        }

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                CallOptions callOptions, Channel next) {
            return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
                @Override
                public void start(Listener<RespT> responseListener, Metadata headers) {
                    headers.put(Metadata.Key.of("api-key", ASCII_STRING_MARSHALLER), api_key);
                    super.start(responseListener, headers);
                }
            };
        }
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    public ManagedChannel loadChannel(String api_key) {
        channel = ManagedChannelBuilder.forAddress(host.host, host.port).usePlaintext()
                .intercept(new serviceClientInterceptor(api_key)).build();
        return channel;
    }

    public void shutdownChannel() {
        channel.shutdown();
        channel = null;
    }

    public static String toJson(MessageOrBuilder obj) {
        String jsonString = "";

        if (obj == null)
            return jsonString;
        try {
            jsonString = JsonFormat.printer().includingDefaultValueFields().print(obj);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonString;
    }
}
