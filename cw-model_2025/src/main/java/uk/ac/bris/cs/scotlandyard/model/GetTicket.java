package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableMap;

import javax.annotation.Nonnull;

public class GetTicket implements Board.TicketBoard {
    public ImmutableMap<ScotlandYard.Ticket, Integer> ticketBoard;
    public GetTicket() {
        this.ticketBoard = ImmutableMap.of();
    }

    @Override
    public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
        return ticketBoard.get(ticket); //may need to get number of tickets later
    }

    

}

