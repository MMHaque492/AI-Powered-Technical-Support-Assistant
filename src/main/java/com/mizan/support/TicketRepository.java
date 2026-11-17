package com.mizan.support;
import org.springframework.data.jpa.repository.JpaRepository;
interface TicketRepository extends JpaRepository<Ticket,Long> {}
