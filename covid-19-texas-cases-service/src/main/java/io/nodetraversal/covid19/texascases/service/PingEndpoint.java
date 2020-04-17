package io.nodetraversal.covid19.texascases.service;

import com.google.common.collect.ImmutableMap;
import org.springframework.stereotype.Service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("ping")
@Service("ping-endpoint")
@Produces(APPLICATION_JSON)
public class PingEndpoint {
    @GET
    public Map<String, String> get() {
        return ImmutableMap.of("value", "pong!");
    }
}
