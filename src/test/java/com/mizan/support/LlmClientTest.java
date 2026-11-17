package com.mizan.support;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;
class LlmClientTest {
    @SuppressWarnings("unchecked")
    @Test void parsesChatCompletionAndSendsAuthorization() throws Exception {
        HttpClient http=mock(HttpClient.class);
        HttpResponse<String> response=mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"choices\":[{\"message\":{\"content\":\"Check DNS resolution.\"}}]}");
        when(http.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(response);
        var client=new LlmClient(true,"https://example.com/chat/completions","test-key","test-model",new ObjectMapper(),http);
        assertEquals("Check DNS resolution.",client.analyze(new Ticket("DNS error","Host not found","Linux","NETWORK")));
        var request=ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(request.capture(),any(HttpResponse.BodyHandler.class));
        assertEquals("Bearer test-key",request.getValue().headers().firstValue("Authorization").orElseThrow());
        assertEquals(25,request.getValue().timeout().orElseThrow().toSeconds());
    }
    @SuppressWarnings("unchecked")
    @Test void rejectsMissingContentAndHttpFailure() throws Exception {
        HttpClient http=mock(HttpClient.class); HttpResponse<String> response=mock(HttpResponse.class);
        when(http.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(response);
        var client=new LlmClient(true,"https://example.com/chat/completions","key","model",new ObjectMapper(),http);
        var ticket=new Ticket("x","x","x","APPLICATION");
        when(response.statusCode()).thenReturn(200);when(response.body()).thenReturn("{}");
        assertThrows(IllegalStateException.class,()->client.analyze(ticket));
        when(response.statusCode()).thenReturn(429);
        assertThrows(IllegalStateException.class,()->client.analyze(ticket));
    }
    @Test void rejectsInsecureEnabledConfiguration() {
        assertThrows(IllegalArgumentException.class,()->new LlmClient(true,"http://example.com","key","model",new ObjectMapper()));
    }
}
