package me.schawe.multijsnake.snake;

import me.schawe.multijsnake.snake.ai.GreedyAutopilot;
import me.schawe.multijsnake.snake.ai.RandomAutopilot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {

    GameState gameState;

    @BeforeEach
    void setUp() {
        // ensure a well defined state to test against
        gameState = new GameState((x) -> {}, 42, 23);
        gameState.seed(42);
        gameState.reset();
    }

    @Test
    void getWidth() {
        assertEquals(
                42,
                gameState.getWidth(),
                "getWidth should work."
        );
    }

    @Test
    void getHeight() {
        assertEquals(
                23,
                gameState.getHeight(),
                "getWidth should work."
        );
    }

    @Test
    void getFood() {
        assertTrue(
                gameState.getFood().equals(new Coordinate(30, 1)),
                "getFood should work."
        );
    }

    @Test
    void isPaused() {
        assertTrue(
                gameState.isPaused(),
                "isPaused should work."
        );
        gameState.setPause(false);
        assertFalse(
                gameState.isPaused(),
                "isPaused should work."
        );
        gameState.setPause(true);
        assertTrue(
                gameState.isPaused(),
                "isPaused should work."
        );
    }

    @Test
    void isGameOver() {
        int idx = gameState.addSnake();
        assertFalse(
                gameState.isGameOver(),
                "isGameOver should work."
        );
        gameState.kill(idx);
        gameState.update();
        assertTrue(
                gameState.isGameOver(),
                "isGameOver should work."
        );
    }

    @Test
    void changeName() {
    }

    @Test
    void gen_id() {
        assertEquals(10, gameState.gen_id().length());
    }

    @Test
    void addSnake() {
    }

    @Test
    void addAISnake() {
    }

    @Test
    void isOccupied() {
    }

    @Test
    void isWall() {
    }

    @Test
    void add_food() {
    }

    @Test
    void turn() {
        int idx = gameState.addSnake();
        Snake snake = gameState.getSnakes().get(0);
        // this seed starts down
        assertEquals(Move.down, snake.headDirection);

        // try valid moves
        gameState.turn(snake.idx, Move.down);
        assertEquals(Move.down, snake.headDirection);

        gameState.turn(snake.idx, Move.left);
        assertEquals(Move.left, snake.headDirection);

        gameState.turn(snake.idx, Move.right);
        assertEquals(Move.right, snake.headDirection);

        // ensure that the previous direction is kept on invalid move
        gameState.turn(snake.idx, Move.up);
        assertEquals(Move.right, snake.headDirection);

        gameState.turn(snake.idx, Move.down);
        assertEquals(Move.down, snake.headDirection);

        gameState.turn(snake.idx, Move.up);
        assertEquals(Move.down, snake.headDirection);
    }

    @Test
    void move() {
        int idx = gameState.addSnake();
        gameState.setPause(false);
        Snake snake = gameState.getSnakes().get(0);
        // this seed starts down
        assertEquals(Move.down, snake.headDirection);
        assertTrue(snake.head.equals(new Coordinate(28, 1)));
        gameState.update();
        assertTrue(new Coordinate(28, 2).equals(new Coordinate(28, 1).add(Move.down.toCoord())));
        assertTrue(snake.head.equals(new Coordinate(28, 2)));
    }

    @Test
    void feed() {
        int idx = gameState.addSnake();
        gameState.setPause(false);
        Snake snake = gameState.getSnakes().get(0);
        assertEquals(2, snake.length, "initial length");
        // this seed starts down
        assertEquals(Move.down, snake.headDirection, "initial direction");
        assertTrue(snake.head.equals(new Coordinate(28, 1)), "initial position");
        assertTrue(gameState.getFood().equals(new Coordinate(30, 1)), "initial food position");
        gameState.turn(idx, Move.right);
        gameState.update();
        gameState.update();
        assertTrue(snake.head.equals(new Coordinate(30, 1)), "food reached");
        gameState.update();
        assertFalse(gameState.getFood().equals(new Coordinate(30, 1)), "new food");
        assertEquals(3, snake.length, "growth");
    }

    @Test
    void dieWall() {
        int idx = gameState.addSnake();
        gameState.setPause(false);
        Snake snake = gameState.getSnakes().get(0);
        // this seed starts down
        assertEquals(Move.down, snake.headDirection, "initial direction");
        assertTrue(snake.head.equals(new Coordinate(28, 1)), "initial position");
        gameState.turn(idx, Move.left);
        gameState.update();
        gameState.turn(idx, Move.up);
        gameState.update();
        assertTrue(snake.head.equals(new Coordinate(27, 0)), "intermediate position");
        assertFalse(snake.isDead());
        gameState.update();
        assertTrue(snake.isDead());
    }

    @Test
    void dieSelf() {
        int idx = gameState.addSnake();
        gameState.setPause(false);
        Snake snake = gameState.getSnakes().get(0);
        snake.length = 10;
        // this seed starts down
        assertEquals(Move.down, snake.headDirection, "initial direction");
        gameState.update();
        gameState.turn(idx, Move.left);
        gameState.update();
        assertFalse(snake.isDead());
        gameState.turn(idx, Move.up);
        gameState.update();
        assertFalse(snake.isDead());
        gameState.turn(idx, Move.right);
        gameState.update();
        assertTrue(snake.isDead());
    }

    @Test
    void dieOther() {
        int idx = gameState.addSnake();
        int otherIdx = gameState.addSnake();
        gameState.setPause(false);
        Snake snake = gameState.getSnakes().get(idx);
        Snake other = gameState.getSnakes().get(otherIdx);
        snake.length = 20;
        other.length = 10;
        // this seed starts down
        assertEquals(Move.down, snake.headDirection, "initial direction");
        assertEquals(Move.left, other.headDirection, "initial direction other");
        assertTrue(snake.head.equals(new Coordinate(28, 1)), "initial position");
        assertTrue(other.head.equals(new Coordinate(39, 6)), "initial position other");
        for(int i=0; i<10; ++i){
            gameState.update();
        }
        assertFalse(snake.isDead());
        assertFalse(other.isDead());
        assertTrue(other.head.equals(new Coordinate(29, 6)), "intermediate position other");
        gameState.update();
        assertFalse(snake.isDead());
        assertTrue(other.isDead());
    }

    @Test
    void addAi() {
        int idx1 = gameState.addAISnake(new RandomAutopilot());
        int idx2 = gameState.addAISnake(new GreedyAutopilot());
        assertEquals(2, gameState.getSnakes().size());
        gameState.setPause(false);
        gameState.update();
    }

    @Test
    void reset() {
        int idx = gameState.addSnake();
        gameState.setPause(false);
        Snake snake = gameState.getSnakes().get(0);
        // this seed starts down
        assertEquals(Move.down, snake.headDirection, "initial direction");
        assertTrue(snake.head.equals(new Coordinate(28, 1)), "initial position");
        gameState.turn(idx, Move.left);
        gameState.update();
        gameState.turn(idx, Move.up);
        gameState.update();
        assertTrue(snake.head.equals(new Coordinate(27, 0)), "intermediate position");
        assertFalse(snake.isDead(), "snake alive");
        gameState.update();

        assertTrue(snake.isDead(), "snake dies");
        assertTrue(gameState.isGameOver(), "game over");

        gameState.reset();

        assertFalse(snake.isDead(), "snake lives");
        assertFalse(gameState.isGameOver(), "game running");
    }
}