package com.mizan.support;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","app.llm.enabled=false"})
@AutoConfigureMockMvc
class TicketApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired TicketRepository repo;
    @BeforeEach void clean() { repo.deleteAll(); }
    @Test void requiresLogin() throws Exception { mvc.perform(get("/api/tickets")).andExpect(status().isUnauthorized()); }
    @Test @WithMockUser void requiresCsrf() throws Exception {
        mvc.perform(post("/api/tickets").contentType("application/json").content("{}" )).andExpect(status().isForbidden());
    }
    @Test @WithMockUser void completeTicketLifecycle() throws Exception {
        String json=mvc.perform(post("/api/tickets").with(csrf()).contentType("application/json")
            .content("{\"title\":\"MySQL connection refused\",\"description\":\"JDBC cannot connect to localhost\",\"environment\":\"Java 21\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.category").value("DATABASE"))
            .andExpect(jsonPath("$.status").value("OPEN")).andReturn().getResponse().getContentAsString();
        long id=mapper.readTree(json).path("id").asLong();
        mvc.perform(post("/api/tickets/"+id+"/analyze").with(csrf())).andExpect(status().isOk())
            .andExpect(jsonPath("$.analysisSource").value("OFFLINE")).andExpect(jsonPath("$.analysis").isNotEmpty());
        mvc.perform(patch("/api/tickets/"+id+"/status").with(csrf()).contentType("application/json").content("{\"status\":\"RESOLVED\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVED"));
        mvc.perform(get("/api/tickets/"+id)).andExpect(jsonPath("$.status").value("RESOLVED"));
        mvc.perform(get("/api/tickets")).andExpect(jsonPath("$.totalElements").value(1));
    }
    @Test @WithMockUser void rejectsInvalidInputs() throws Exception {
        mvc.perform(post("/api/tickets").with(csrf()).contentType("application/json").content(mapper.writeValueAsString(java.util.Map.of("title", " ", "description", "x", "environment", "test"))))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/tickets?page=-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/tickets/999999")).andExpect(status().isNotFound());
        mvc.perform(patch("/api/tickets/1/status").with(csrf()).contentType("application/json").content("{\"status\":\"INVALID\"}"))
            .andExpect(status().isBadRequest());
    }
    @Test @WithMockUser void sessionProvidesCsrfAndMode() throws Exception {
        mvc.perform(get("/api/session")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty()).andExpect(jsonPath("$.llmEnabled").value(false));
    }
}
