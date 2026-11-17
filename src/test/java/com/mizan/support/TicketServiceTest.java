package com.mizan.support;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
class TicketServiceTest {
    @Test void providerFailureFallsBackWithoutLeakingErrors() throws Exception {
        var repo=mock(TicketRepository.class);var llm=mock(LlmClient.class);
        var ticket=new Ticket("DNS timeout","Cannot resolve hostname","Linux","NETWORK");
        when(repo.findById(1L)).thenReturn(Optional.of(ticket));when(repo.save(ticket)).thenReturn(ticket);
        when(llm.enabled()).thenReturn(true);when(llm.analyze(ticket)).thenThrow(new RuntimeException("secret-provider-error"));
        var result=new TicketService(repo,new Troubleshooter(),llm).analyze(1L);
        assertEquals("OFFLINE_FALLBACK",result.analysisSource);assertFalse(result.analysis.contains("secret-provider-error"));
    }
    @Test void successfulProviderResponseIsSaved() throws Exception {
        var repo=mock(TicketRepository.class);var llm=mock(LlmClient.class);
        var ticket=new Ticket("Slow query","Query takes ten seconds","MySQL","DATABASE");
        when(repo.findById(1L)).thenReturn(Optional.of(ticket));when(repo.save(ticket)).thenReturn(ticket);
        when(llm.enabled()).thenReturn(true);when(llm.analyze(ticket)).thenReturn("Inspect the query plan.");
        var result=new TicketService(repo,new Troubleshooter(),llm).analyze(1L);
        assertEquals("LLM",result.analysisSource);assertEquals("Inspect the query plan.",result.analysis);
    }
}
