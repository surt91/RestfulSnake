package me.schawe.multijsnake.snake.ai;

import me.schawe.multijsnake.snake.GameState;
import me.schawe.multijsnake.snake.Move;
import me.schawe.multijsnake.snake.Snake;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// https://github.com/surt91/rsnake
public interface Autopilot {
    String generateName();
    Move suggest(GameState gameState, Snake snake);

    default List<Move> possibleMoves(GameState gameState, Snake snake) {
        Move[] allMoves = {Move.up, Move.down, Move.left, Move.right};

        return Arrays.stream(allMoves)
                .filter(move -> isSafeMove(gameState, snake, move))
                .collect(Collectors.toList());
    }

    default boolean isSafeMove(GameState gameState, Snake snake, Move move) {
        return !(
                move.isOpposite(snake.headDirection)
                || gameState.isOccupied(move.toCoord().add(snake.head))
                || gameState.isWall(move.toCoord().add(snake.head))
        );
    }
}
