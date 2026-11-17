package com.mizan.support;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.data.domain.Page;
import java.net.URI;
import java.util.Map;
@RestController
@RequestMapping("/api")
class TicketController {
    record CreateRequest(@NotBlank @Size(max=160) String title, @NotBlank @Size(max=6000) String description,
                         @NotBlank @Size(max=120) String environment) {}
    record StatusRequest(@NotNull Ticket.Status status) {}
    private final TicketService service;
    private final LlmClient llm;
    TicketController(TicketService service,LlmClient llm) { this.service=service; this.llm=llm; }
    @GetMapping("/session") Map<String,Object> session(CsrfToken csrf) {
        return Map.of("token",csrf.getToken(),"headerName",csrf.getHeaderName(),"llmEnabled",llm.enabled());
    }
    @GetMapping("/tickets") Page<Ticket> list(@RequestParam(defaultValue="0") int page) {
        if (page<0 || page>100000) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid page");
        return service.list(page);
    }
    @GetMapping("/tickets/{id}") Ticket get(@PathVariable long id) { return service.get(id); }
    @PostMapping("/tickets") ResponseEntity<Ticket> create(@Valid @RequestBody CreateRequest r) {
        Ticket t=service.create(r); return ResponseEntity.created(URI.create("/api/tickets/"+t.id)).body(t);
    }
    @PatchMapping("/tickets/{id}/status") Ticket status(@PathVariable long id,@Valid @RequestBody StatusRequest r) { return service.status(id,r.status()); }
    @PostMapping("/tickets/{id}/analyze") Ticket analyze(@PathVariable long id) { return service.analyze(id); }
}
