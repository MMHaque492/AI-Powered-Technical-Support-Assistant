package com.mizan.support;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.*;
import java.time.Instant;
import java.util.concurrent.Semaphore;
@Service
class TicketService {
    private final TicketRepository repo;
    private final Troubleshooter offline;
    private final LlmClient llm;
    private final Semaphore slots=new Semaphore(2);
    TicketService(TicketRepository repo, Troubleshooter offline, LlmClient llm) { this.repo=repo; this.offline=offline; this.llm=llm; }
    Page<Ticket> list(int page) { return repo.findAll(PageRequest.of(page,20,Sort.by("createdAt").descending().and(Sort.by("id").descending()))); }
    Ticket get(long id) { return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Ticket not found")); }
    Ticket create(TicketController.CreateRequest r) {
        return repo.save(new Ticket(r.title().trim(), r.description().trim(), r.environment().trim(), offline.category(r.title()+" "+r.description())));
    }
    Ticket status(long id, Ticket.Status status) {
        Ticket t=get(id); t.status=status; t.updatedAt=Instant.now(); return repo.save(t);
    }
    Ticket analyze(long id) {
        if (!slots.tryAcquire()) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Analysis busy; try again shortly");
        try {
            Ticket t=get(id);
            if (llm.enabled()) {
                try { t.analysis=llm.analyze(t); t.analysisSource="LLM"; }
                catch (Exception e) {
                    if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                    t.analysis=offline.suggest(t.category); t.analysisSource="OFFLINE_FALLBACK";
                }
            } else { t.analysis=offline.suggest(t.category); t.analysisSource="OFFLINE"; }
            t.updatedAt=Instant.now(); return repo.save(t);
        } finally { slots.release(); }
    }
}
