package me.schawe.RestfulSnake;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameState {
    String id;
    int width;
    int height;
    Coordinate food;
    List<Snake> snakes;
    int score;
    boolean paused;
    boolean gameOver;

    GameState() {
        id = gen_id();
        width = 10;
        height = 10;
        score = 0;
        snakes = new ArrayList<>();
        snakes.add(new Snake());
        add_food();
        paused = true;
        gameOver = false;
    }

    public String getId() {
        return id;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Coordinate getFood() {
        return food;
    }

    public int getScore() {
        return score;
    }

    public List<Snake> getSnakes() {
        return snakes;
    }

    public void setPause(boolean paused) {
        this.paused = paused;
    }

    public static String gen_id() {
        // https://www.baeldung.com/java-random-string
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 10;
        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public int addSnake() {
        snakes.add(new Snake());
        return snakes.size() - 1;
    }

    private boolean occupied(Coordinate site) {
        return snakes.stream().anyMatch(snake ->
            snake.tail.stream().anyMatch(c -> c.equals(site)) || snake.head.equals(site)
        );
    }

    private Coordinate randomSite() {
        Coordinate site;
        do {
            site = new Coordinate((int) (Math.random() * width), (int) (Math.random() * height));
        } while (occupied(site));
        return site;
    }

    public void add_food() {
        food = randomSite();
    }

    public void turn(int idx, Move move) {
        snakes.get(idx).headDirection = move.toNext(snakes.get(idx).lastHeadDirection).orElse(snakes.get(idx).headDirection);
    }

    public void update() {
        if(snakes.stream().allMatch(snake -> snake.dead)) {
            gameOver = true;
        }

        if(gameOver || paused) {
            return;
        }

        for(Snake snake : snakes) {
            Coordinate offset = snake.headDirection.toCoord();
            snake.lastHeadDirection = snake.headDirection;

            snake.tail.add(snake.head.copy());

            if (snake.head.x < 0 || snake.head.x >= width || snake.head.y < 0 || snake.head.y >= height) {
                snake.dead = true;
            }

            if (snake.head.equals(food)) {
                snake.length += 1;
                score += 1;
                add_food();
            }

            while (snake.tail.size() >= snake.length + 1) {
                snake.tail.remove();
            }

            snake.head.add(offset);

            for (Coordinate i : snake.tail) {
                if (i.equals(snake.head)) {
                    snake.dead = true;
                }
            }
        }
    }
}

