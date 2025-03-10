package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

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
			for (Player player : detectives) {
				if(player.tickets().isEmpty()) return ImmutableSet.of();
			};
          return getAvailableMoves();
		}

		@Override public GameState advance(Move move) {  return null;  }
	}


	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);

	}

}
