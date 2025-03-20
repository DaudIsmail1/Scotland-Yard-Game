package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.*;

import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket.*;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {
    private final class MyGameState implements GameState {
        private GameSetup setup;
        private ImmutableSet<Piece> remaining;
        private ImmutableList<LogEntry> log;
        private Player mrX;
        private List<Player> detectives;
        private ImmutableSet<Move> moves;
        private ImmutableSet<Piece> winner;


        private MyGameState(
                final GameSetup setup,
                final ImmutableSet<Piece> remaining,
                final ImmutableList<LogEntry> log,
                final Player mrX,
                final List<Player> detectives) {
            this.setup = setup;
            this.remaining = remaining;
            this.log = log;
            this.mrX = mrX;
            this.detectives = detectives;
            this.moves = ImmutableSet.of();
            this.winner = ImmutableSet.of();
            Set<Move.DoubleMove> mrXDoubleMoves = makeDoubleMoves(setup, detectives, mrX, mrX.location());
            Set<Move.SingleMove> mrXSingleMoves = makeSingleMoves(setup, detectives, mrX, mrX.location());
            ImmutableSet.Builder<Move> builder = ImmutableSet.builder();
            // Generate moves for remaining detectives and add to builder
            for (Player detective : detectives) {
                if (remaining.contains(detective.piece())) {
                    Set<Move.SingleMove> detectiveSingleMoves = generateDetectiveMoves(setup, detectives, detective, detective.location());
                    builder.addAll(detectiveSingleMoves);
                }
            }
            // If it's Mr.X's turn then add his moves
            if (remaining.contains(mrX.piece())) {
                builder.addAll(mrXSingleMoves).addAll(mrXDoubleMoves).build();
            }
            moves = builder.build();


            if (setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
            if (detectives.isEmpty()) throw new NullPointerException("Detectives is empty!");
            if (isDetectivesDouble()) throw new IllegalArgumentException("Detectives must be have double tickets!");
            if (isDetectivesSecret()) throw new IllegalArgumentException("Detectives must be have secret tickets!");
            if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph is empty!");
            if (isDetectivesLocation()) throw new IllegalArgumentException("Detectives are overlapping");


        }

        public boolean isDetectivesDouble() {
            for (Player player : detectives) {
                if (player.has(DOUBLE)) return true;
            }
            return false;
        }

        public boolean isDetectivesSecret() {
            for (Player player : detectives) {
                if (player.has(SECRET)) return true;
            }
            return false;
        }

        public boolean isDetectivesLocation() {
            for (int i = 0; i < detectives.size(); i++) {
                for (int j = i + 1; j < detectives.size(); j++) {
                    if (Objects.equals(detectives.get(i).location(), detectives.get(j).location()))
                        return true;
                }
            }
            return false;
        }

        private Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {

            // Create a hashsets which stores collection of moves and the detectives' locations
            HashSet<Move.SingleMove> moves = new HashSet<>();
            HashSet<Integer> detectivesLocation = new HashSet<>();
            for (Player detective : detectives) {
                detectivesLocation.add(detective.location());
            }
            // Check if the destination is occupied by a detective
            for (int destination : setup.graph.adjacentNodes(source)) {
                if (detectivesLocation.contains(destination)) continue;
                // For each transport type between the source and the destination check if player has required ticket
                for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
                    // If the player has the required ticket, add a SingleMove to the collection of moves to return
                    if (player.isMrX()) {
                        if (player.has(t.requiredTicket())) {
                            moves.add(new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination));
                        }
                    }
                    if (player.isMrX() && player.has(SECRET)) {
                        moves.add(new Move.SingleMove(player.piece(), player.location(), SECRET, destination));
                    }
                    if (player.isDetective()) {
                        if (player.hasAtLeast(t.requiredTicket(), 1)) {
                            moves.add(new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination));
                        }
                    }
                }
            }
            return moves;
        }

        // Generate the detectives' moves using makeSingleMoves function
        private Set<Move.SingleMove> generateDetectiveMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
            // Create a hashsets which stores collection of detectives' moves and the detectives' locations
            HashSet<Move.SingleMove> detectiveSingleMoves = new HashSet<>();
            HashSet<Integer> destinations = new HashSet<>();
            for (Player detective : detectives) {
                destinations.add(detective.location());
            }
            // Check if the destination is occupied by a detective
            for (int destination : setup.graph.adjacentNodes(source)) {
                if (destinations.contains(destination)) continue;
                // For each transport type between the source and the destination check if player has required ticket
                for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
                    // If the player has the required ticket, add a SingleMove to the collection of moves to return
                    if (player.has(t.requiredTicket())) {
                        detectiveSingleMoves.add(new Move.SingleMove(player.piece(), player.location(), t.requiredTicket(), destination));
                    }
                }
            }
            return detectiveSingleMoves;
        }

        private Set<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {

            // Create a hashsets which stores collection of moves and the detectives' locations
            HashSet<Move.DoubleMove> moves = new HashSet<>();
            HashSet<Integer> destinations = new HashSet<>();
            for (Player detective : detectives) {
                destinations.add(detective.location());
            }

            // Check if the destination is occupied by a detective
            for (int destination1 : setup.graph.adjacentNodes(source)) {
                // Check if player has a double ticket and there at least two rounds left in the game
                if (!player.isMrX() || !player.has(DOUBLE) || setup.moves.size() - log.size() < 2) continue;
                // Check if the destination is occupied by a detective
                if (destinations.contains(destination1)) continue;
                // For each transport type between the source and the destination check if player has required ticket.
                for (ScotlandYard.Transport t1 : setup.graph.edgeValueOrDefault(source, destination1, ImmutableSet.of())) {
                    // If the MrX has the required ticket, add a DoubleMove to the collection of moves to return
                    if (player.isMrX() && (!player.has(t1.requiredTicket()) && (!player.has(SECRET)))) continue;
                    for (int destination2 : setup.graph.adjacentNodes(destination1)) {
                        if (destinations.contains(destination2)) continue;
                        for (ScotlandYard.Transport t2 : setup.graph.edgeValueOrDefault(destination1, destination2, ImmutableSet.of())) {
                                if (t1.requiredTicket().equals(t2.requiredTicket())) {
                                    if (player.hasAtLeast(t1.requiredTicket(), 2)) {
                                        moves.add(new Move.DoubleMove(player.piece(), player.location(), t1.requiredTicket(), destination1, t2.requiredTicket(), destination2));
                                    }
                                } else {
                                    if (player.has(t1.requiredTicket()) && player.has(t2.requiredTicket())) {
                                        moves.add(new Move.DoubleMove(player.piece(), player.location(), t1.requiredTicket(), destination1, t2.requiredTicket(), destination2));
                                    }
                                }
                            if (player.has(t1.requiredTicket()) && player.has(SECRET)) {
                                moves.add(new Move.DoubleMove(player.piece(), player.location(), t1.requiredTicket(), destination1, SECRET, destination2));
                            }
                            if (player.has(t2.requiredTicket()) && player.has(SECRET)) {
                                moves.add(new Move.DoubleMove(player.piece(), player.location(), SECRET, destination1, t2.requiredTicket(), destination2));
                            }
                            if (player.hasAtLeast(SECRET, 2)) {
                                moves.add(new Move.DoubleMove(player.piece(), player.location(), SECRET, destination1, SECRET, destination2));
                            }
                        }
                    }
                }
            }
            return moves;
        }

        @Override
        public GameSetup getSetup() {
            return setup;
        }

        @Override
        public ImmutableSet<Piece> getPlayers() {
           ImmutableSet.Builder<Piece> builder = ImmutableSet.builder();
           for(Player detective : detectives){
               builder.add(detective.piece());
           }
           builder.add(mrX.piece());
           return builder.build();
        }

        @Nonnull
        @Override
        public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
            for (Player player : detectives) {
                if ((player.piece()) == detective)
                    return Optional.of(player.location());
            }
            return Optional.empty();
        }

        @Nonnull
        @Override
        public Optional<TicketBoard> getPlayerTickets(Piece piece) {
            if (mrX.piece() == piece) {
                return Optional.of(new TicketBoard() {
                    @Override
                    public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
                        return mrX.tickets().getOrDefault(ticket, 0);
                    }
                });
            }
            for (Player player : detectives) {
                if ((player.piece()) == piece)
                    return Optional.of(new TicketBoard() {
                        @Override
                        public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
                            return player.tickets().getOrDefault(ticket, 0);
                        }
                    });
            }
            return Optional.empty();
        }

        @Nonnull
        @Override
        public ImmutableList<LogEntry> getMrXTravelLog() {
            return log;
        }

        @Nonnull
        @Override
        public ImmutableSet<Piece> getWinner() {
            for (Player detective : detectives) {
                if (detective.location() == mrX.location()) {
                    moves = ImmutableSet.of();
                    return getDetectives();
                }
            }
            if (makeSingleMoves(setup, detectives, mrX, mrX.location()).isEmpty()) {
                return getDetectives();
            }
            if (log.size() == setup.moves.size() && remaining.contains(mrX.piece())) {
                moves = ImmutableSet.of();
                return ImmutableSet.of(mrX.piece());
            }
            boolean detectiveStuck = true;
            for (Player detective : detectives) {
                if (!generateDetectiveMoves(setup, detectives, detective, detective.location()).isEmpty()) {
                    detectiveStuck = false;
                }
            }
            if (detectiveStuck) {
                moves = ImmutableSet.of();
                return ImmutableSet.of(mrX.piece());
            }
            return ImmutableSet.of();
        }

        @Nonnull
        @Override
        public ImmutableSet<Move> getAvailableMoves() {
            return moves;
        }

        public ImmutableSet<Piece> getDetectives() {
            ImmutableSet.Builder<Piece> builder = ImmutableSet.builder();
            for (Player player : detectives) {
                builder.add(player.piece());
            }
            return builder.build();
        }

        @Override
        public GameState advance(Move move) {
            // Check if move is contained in our collection fo valid moves
            if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);
            return move.accept(new Move.Visitor<GameState>() {

                @Override
                public GameState visit(Move.SingleMove move) {
                    if (move.commencedBy().equals(mrX.piece())) {
                        // Use ticket and move to new location
                        Player updatedMrX = mrX.use(move.ticket).at(move.destination);
                        // Update travel log for reveal/hidden
                        ImmutableList<LogEntry> updatedLog;
                        ImmutableList.Builder<LogEntry> builder = ImmutableList.builder();
                        int currentRound = log.size();
                        // Add current travel log so far to updated log
                        builder.addAll(log);
                        if (setup.moves.get(currentRound)) {
                            builder.add(LogEntry.reveal(move.ticket, move.destination));
                        } else {
                            builder.add(LogEntry.hidden(move.ticket));
                        }
                        updatedLog = builder.build();
                        // Check if detective is not stuck and add them to remaining
                        Set<Piece> updatedRemaining = new HashSet<>();
                        for (Player detective : detectives) {
                            if (!generateDetectiveMoves(setup, detectives, detective, detective.location()).isEmpty()) {
                                updatedRemaining.add(detective.piece());
                            }
                        }
                        return new MyGameState(setup, ImmutableSet.copyOf(updatedRemaining), updatedLog, updatedMrX, detectives);
                    }
                    for (int i = 0; i < detectives.size(); i++) {
                        if (move.commencedBy().equals(detectives.get(i).piece())) {
                            // Use ticket and move detective to new location
                            Player updatedDetective = detectives.get(i).use(move.ticket).at(move.destination);
                            // Give ticket to mrX
                            Player updatedMrX = mrX.give(move.ticket);
                            // Remove the detective who made the move from remaining
                            Set<Piece> updatedRemaining = new HashSet<>(remaining);
                            updatedRemaining.remove(move.commencedBy());
                            // When all detective move change to mrX turn
                            if (updatedRemaining.isEmpty()) {
                                updatedRemaining.add(mrX.piece());
                            }
                            // Create a list with current detectives and update detective at 'i' with updated detective
                            List<Player> updatedDetectives = new ArrayList<>(detectives);
                            updatedDetectives.set(i, updatedDetective);
                            return new MyGameState(setup, ImmutableSet.copyOf(updatedRemaining), log, updatedMrX, updatedDetectives);
                        }
                    }
                    throw new IllegalArgumentException("Illegal move: " + move);
                }

                @Override
                public GameState visit(Move.DoubleMove move) {
                    // Use both tickets and a double ticket
                    Player updatedMrX = mrX.use(move.ticket1).use(move.ticket2).use(DOUBLE);
                    // Move to new location
                    updatedMrX = updatedMrX.at(move.destination1).at(move.destination2);
                    // Update travel log twice for reveal/hidden
                    ImmutableList<LogEntry> updatedLog;
                    int currentRound = log.size();
                    ImmutableList.Builder<LogEntry> builder = new ImmutableList.Builder<>();
                    // Add current log so far to updated log
                    builder.addAll(log);
                    if (setup.moves.get(currentRound)) {
                        builder.add(LogEntry.reveal(move.ticket1, move.destination1));
                    } else {
                        builder.add(LogEntry.hidden(move.ticket1));
                    }
                    if (setup.moves.get(currentRound + 1)) {
                        builder.add(LogEntry.reveal(move.ticket2, move.destination2));
                    } else {
                        builder.add(LogEntry.hidden(move.ticket2));
                    }
                    updatedLog = builder.build();
                    return new MyGameState(setup, getDetectives(), updatedLog, updatedMrX, detectives);
                }
            });
        }
    }

    @Nonnull
    @Override
    public GameState build(
            GameSetup setup,
            Player mrX,
            ImmutableList<Player> detectives) {
        return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);

    }

}
