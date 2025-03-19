package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {



	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {
		class MyModel implements Model{
			private Board.GameState board = new MyGameStateFactory().build(setup, mrX, detectives);
			private final List<Observer> observers = new ArrayList<>();


			@Nonnull
			@Override
			public Board getCurrentBoard() {
				return board;
			}

			@Override
			public void registerObserver(@Nonnull Observer observer) {
				if(observer == null) {
					throw new NullPointerException();
				}
				if(observers.contains(observer)) {
					throw new IllegalArgumentException("Observer already registered");
				}
				observers.add(observer);
			}

			@Override
			public void unregisterObserver(@Nonnull Observer observer) {
				if (observer == null) {
					throw new NullPointerException();
				}
				if(!observers.contains(observer)) {
					throw new IllegalArgumentException("Observer not registered");
				}
				observers.remove(observer);

			}

			@Nonnull
			@Override
			public ImmutableSet<Observer> getObservers() {
				return ImmutableSet.copyOf(observers);
			}

			@Override
			public void chooseMove(@Nonnull Move move) {
				// TODO Advance the model with move, then notify all observers of what what just happened.
				//  you may want to use getWinner() to determine whether to send out Event.MOVE_MADE or Event.GAME_OVER
				board = board.advance(move);
				if(!board.getWinner().isEmpty()){
					for (Observer observer : observers) {
						observer.onModelChanged(board, Observer.Event.GAME_OVER);
					}
				}
				if(board.getWinner().isEmpty()){
					for(Observer observer : observers) {
						observer.onModelChanged(board, Observer.Event.MOVE_MADE);
					}
				}
			}
		}
		return new MyModel();
	}
}
