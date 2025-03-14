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
				final List<Player> detectives)
		{
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.moves = ImmutableSet.of();
			this.winner = ImmutableSet.of();
			Set<Move.DoubleMove> mrXDoubleMoves = makeDoubleMoves(setup, detectives, mrX, mrX.location());
			Set<Move.SingleMove> mrXSingleMoves = makeSingleMoves(setup, detectives, mrX, mrX.location());
			Set<Move.SingleMove> detectiveSingleMoves = generateDetectiveMoves(detectives);
			ImmutableSet.Builder<Move> builder = ImmutableSet.builder();
			moves = builder.addAll(mrXSingleMoves).addAll(detectiveSingleMoves).addAll(mrXDoubleMoves).build();


			if(setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
			if(detectives.isEmpty()) throw new NullPointerException("Detectives is empty!");
			if(isDetectivesDouble()) throw new IllegalArgumentException("Detectives must be have double tickets!");
			if(isDetectivesSecret()) throw new IllegalArgumentException("Detectives must be have secret tickets!");
			if(setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph is empty!");
			if(isDetectivesLocation()) throw new IllegalArgumentException("Detectives are overlapping");


		}
		public boolean isDetectivesDouble() {
			for (Player player : detectives) {
				if(player.has(DOUBLE)) return true;
			}
			return false;
		}

		public boolean isDetectivesSecret(){
			for (Player player : detectives) {
				if(player.has(SECRET)) return true;
			}
			return false;
		}

		public boolean isDetectivesLocation(){
			for (int i = 0; i < detectives.size(); i++) {
				for (int j = i + 1; j < detectives.size(); j++) {
					if(Objects.equals(detectives.get(i).location(), detectives.get(j).location()))
						return true;
				}
			}
			return false;
		}

		private Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){

			// TODO create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate
			HashSet<Move.SingleMove> moves = new HashSet<>();
			HashSet<Integer> destinations = new HashSet<>();
			for (Player detective : detectives) {
				destinations.add(detective.location());
			}

			for(int destination : setup.graph.adjacentNodes(source)) {
				// TODO find out if destination is occupied by a detective
				//  if the location is occupied, don't add to the collection of moves to return
				if(destinations.contains(destination)) continue;
				for(ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()) ) {
						// TODO find out if the player has the required tickets
						//  if it does, construct a SingleMove and add it the collection of moves to return
					if (player.isMrX()){
						if(player.has(t.requiredTicket())){
							moves.add(new Move.SingleMove(player.piece(),source, t.requiredTicket(), destination));
						}
					}
						// TODO consider the rules of secret moves here
						//  add moves to the destination via a secret ticket if there are any left with the player
					if (player.isMrX() && player.has(SECRET)){
						moves.add(new Move.SingleMove(player.piece(), player.location(), SECRET, destination));
					}
					if (player.isDetective()){
						if (remaining.contains(player.piece())){
							moves.add(new Move.SingleMove(player.piece(),player.location(), t.requiredTicket(), destination));
						}
					}
				}
			}
			return moves;
		}
		private  Set<Move.SingleMove> generateDetectiveMoves (List<Player> detectives) {
			HashSet<Move.SingleMove> detectiveMoves = new HashSet<>();
			for (Player detective : detectives){
				detectiveMoves.addAll(makeSingleMoves(setup, detectives, detective,detective.location()));
			}
			return detectiveMoves;
		}

		private Set<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source){

			// TODO create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate
			HashSet<Move.DoubleMove> moves = new HashSet<>();
			HashSet<Integer> destinations = new HashSet<>();
			for (Player detective : detectives) {
				destinations.add(detective.location());
			}

			for(int destination1 : setup.graph.adjacentNodes(source)) {
				// TODO find out if destination is occupied by a detective
				//  if the location is occupied, don't add to the collection of moves to return
				if (!player.isMrX() || !player.has(DOUBLE) || setup.moves.size() - log.size() < 2) continue;
				if(destinations.contains(destination1)) continue;
				for(ScotlandYard.Transport t1 : setup.graph.edgeValueOrDefault(source, destination1, ImmutableSet.of()) ) {
					// TODO find out if the player has the required tickets
					//  if it does, construct a SingleMove and add it the collection of moves to return
					if (player.isMrX() && (!player.has(t1.requiredTicket()) && (!player.has(SECRET)))) continue;
					for(int destination2 : setup.graph.adjacentNodes(destination1)) {
						if (destinations.contains(destination2)) continue;
						for (ScotlandYard.Transport t2 : setup.graph.edgeValueOrDefault(destination1, destination2, ImmutableSet.of())){
							if(player.isMrX()){
								if(t1.requiredTicket().equals(t2.requiredTicket())){
									if(player.hasAtLeast(t1.requiredTicket(), 2)){
										moves.add(new Move.DoubleMove(player.piece(),player.location(), t1.requiredTicket(), destination1, t2.requiredTicket(), destination2));
									}
								} else {
									if (player.has(t1.requiredTicket()) && player.has(t2.requiredTicket())){
										moves.add(new Move.DoubleMove(player.piece(), player.location(), t1.requiredTicket(), destination1, t2.requiredTicket(), destination2));
									}
								}
							}
							if(player.isMrX() && player.has(t1.requiredTicket()) && player.has(SECRET)){
								moves.add(new Move.DoubleMove(player.piece(), player.location(), t1.requiredTicket(), destination1, SECRET, destination2));
							}
							if(player.isMrX() && player.has(t2.requiredTicket()) && player.has(SECRET)){
								moves.add(new Move.DoubleMove(player.piece(), player.location(),SECRET, destination1, t2.requiredTicket(), destination2));
							}
							if(player.isMrX() && player.hasAtLeast(SECRET, 2)){
								moves.add(new Move.DoubleMove(player.piece(), player.location(),SECRET, destination1, SECRET, destination2));
							}
						}
					}
				}
			}
			return moves;
		}


		@Override public GameSetup getSetup() {  return setup; }

		@Override  public ImmutableSet<Piece> getPlayers() {
			return ImmutableSet.<Piece>builder()
					.addAll(remaining)
					.addAll(detectives.stream().map(player -> player.piece()).toList())
					.build();

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
			if(mrX.piece() == piece) {
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
			return ImmutableSet.of();
		}

		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			return moves;
		}

		@Override public GameState advance(Move move) {
			if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);
			return move.accept(new Move.Visitor<GameState>() {

				@Override
				public GameState visit(Move.SingleMove move) {
					if (move.commencedBy().equals(mrX.piece())){

					}
					return null;
				}

				@Override
				public GameState visit(Move.DoubleMove move) {
					return null;
				}
			});
		}
	}

	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);

	}

}
