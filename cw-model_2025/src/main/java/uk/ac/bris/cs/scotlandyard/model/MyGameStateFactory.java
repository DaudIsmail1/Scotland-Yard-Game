package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

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
			Set<Move.SingleMove> mrXSingleMoves = makeSingleMoves(setup, detectives, mrX, mrX.location());
//			Set<Move.DoubleMove> mrXDoubleMoves = makeDoubleMoves(setup, detectives, mrX, mrX.location());
			ImmutableSet.Builder<Move> builder = ImmutableSet.builder();
			moves = builder.addAll(mrXSingleMoves).build();


			if(setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
			if(detectives.isEmpty()) throw new NullPointerException("Detectives is empty!");
			if(isDetectivesDouble()) throw new IllegalArgumentException("Detectives must be have double tickets!");
			if(isDetectivesSecret()) throw new IllegalArgumentException("Detectives must be have secret tickets!");
			if(setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph is empty!");
			if(isDetectivesLocation()) throw new IllegalArgumentException("Detectives are overlapping");
			if(mrX == null) throw new NullPointerException(" There is no MrX!");
//


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

		private static Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){

			// TODO create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate
			HashSet<Move.SingleMove> moves = new HashSet<>();

			for(int destination : setup.graph.adjacentNodes(source)) {
				// TODO find out if destination is occupied by a detective
				//  if the location is occupied, don't add to the collection of moves to return
				boolean shouldAdd = true;
				for (Player detective : detectives){
					if (detective.location() == destination) {
						shouldAdd = false;
					}
				}
				if (!shouldAdd) continue;
				for(ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()) ) {
					// TODO find out if the player has the required tickets
					//  if it does, construct a SingleMove and add it the collection of moves to return
					for (Player detective : detectives){
						if (detective.has(t.requiredTicket())) {
							shouldAdd = true;
						}
						shouldAdd = false;
					}
					if (player.isMrX()){
                        shouldAdd = player.has(t.requiredTicket());
					}

					if(shouldAdd) {
						moves.add(new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination));
					}
				}
				// TODO consider the rules of secret moves here
				//  add moves to the destination via a secret ticket if there are any left with the player
				boolean isSecret = true;
				if (player.isMrX()){
					if(player.has(SECRET)){
						isSecret = true;
					}
					isSecret = false;
				}
				if (isSecret) {
					moves.add(new Move.SingleMove(player.piece(), source, SECRET, destination));
				}
			}

			// TODO return the collection of moves
			return moves;
		}
		private static Set<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source){

			// TODO create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate
			HashSet<Move.DoubleMove> moves = new HashSet<>();

			for(int destination : setup.graph.adjacentNodes(source)) {
				// TODO find out if destination is occupied by a detective
				//  if the location is occupied, don't add to the collection of moves to return

				for(ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()) ) {
					// TODO find out if the player has the required tickets
					//  if it does, construct a SingleMove and add it the collection of moves to return
				}

				// TODO consider the rules of secret moves here
				//  add moves to the destination via a secret ticket if there are any left with the player
			}

			// TODO return the collection of moves
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
			return null;
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
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);






			return null;  }



	}


	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);

	}

}
