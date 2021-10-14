package me.schawe.multijsnake;

import me.schawe.multijsnake.highscore.Highscore;
import me.schawe.multijsnake.highscore.HighscoreRepository;
import me.schawe.multijsnake.snake.*;
import me.schawe.multijsnake.snake.ai.Autopilot;
import me.schawe.multijsnake.snake.ai.GreedyAutopilot;
import me.schawe.multijsnake.snake.ai.KerasModel;
import me.schawe.multijsnake.snake.ai.RandomAutopilot;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameStateMap {
    private final ConcurrentHashMap<String, GameState> gameStateMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SnakeId> sessionMap = new ConcurrentHashMap<>();
    private final WebSocketService webSocketService;
    private final HighscoreRepository highscoreRepository;

    private final Map<String, String> aiDescriptionMap;

    GameStateMap(WebSocketService webSocketService, HighscoreRepository highscoreRepository) {
        this.webSocketService = webSocketService;
        this.highscoreRepository = highscoreRepository;

        this.aiDescriptionMap = aiDescriptions();
    }

    public GameState newGameState(int w, int h, long seed) {
        GameState gameState = new GameState(w, h, seed);
        return initGamestate(gameState);
    }

    public GameState newGameState(int w, int h, String id) {
        GameState gameState = new GameState(w, h, id);
        return initGamestate(gameState);
    }

    public GameState newGameState(int w, int h) {
        GameState gameState = new GameState(w, h);
        return initGamestate(gameState);
    }

    private GameState initGamestate(GameState gameState) {
        int size = gameState.getWidth()*gameState.getHeight();
        gameState.setSnakeDiesCallback(x -> updateHighscore(x, size));

        if(gameStateMap.containsKey(gameState.getId())) {
            throw new InvalidMapException("This id '" + gameState.getId() + "' already exists!");
        }
        gameStateMap.put(gameState.getId(), gameState);

        return gameState;
    }

    @Scheduled(fixedRate = 300)
    void periodicUpdate() {
        Set<String> ids = allIds();
        for (String id : ids) {
            GameState gameState = gameStateMap.get(id);
            if(!gameState.isPaused() && !gameState.isGameOver()) {
                gameState.update();
                webSocketService.update(gameState);
            }
        }
    }

    private void updateHighscore(Snake snake, int size) {
        var score = snake.getLength();

        // do not save highscore for AI snakes
        // also do not save very low scores
        if (snake.ai().isPresent() || score < 5) {
            return;
        }

        Date date = new Date();
        Highscore highscore = new Highscore(score, snake.getName(), size, date);
        highscoreRepository.save(highscore);
        webSocketService.updateHighscore(size);
        webSocketService.updateGlobalHighscore();
    }

    GameState get(String id) {
        if(!gameStateMap.containsKey(id)) {
            throw new InvalidMapException(id);
        }

        return gameStateMap.get(id);
    }

    SnakeId session2id(String sessionId) {
        if(!sessionMap.containsKey(sessionId)) {
            throw new InvalidMapException("");
        }

        return sessionMap.get(sessionId);
    }

    void close(String id) {
        gameStateMap.remove(id);
    }

    void put(String id, GameState state) {
        gameStateMap.put(id, state);
    }

    void putSession(String sessionId, SnakeId id) {
        sessionMap.put(sessionId, id);
    }

    public Set<String> allIds() {
        return gameStateMap.keySet();
    }

    public void pause(String sessionId) {
        SnakeId snakeId = session2id(sessionId);
        GameState state = get(snakeId.getId());
        state.setPause(true);
        webSocketService.update(state);
    }

    public void unpause(String sessionId) {
        SnakeId snakeId = session2id(sessionId);
        GameState state = get(snakeId.getId());
        state.setPause(false);
        webSocketService.update(state);
    }

    public void reset(String sessionId) {
        SnakeId snakeId = session2id(sessionId);
        GameState state = get(snakeId.getId());
        state.reset();
        webSocketService.update(state);
    }

    public void join(String sessionId, String id) {
        // if the id does not exist, make it exist
        if(!gameStateMap.containsKey(id)) {
            newGameState(20, 20, id);
        }

        int idx = get(id).addSnake();

        putSession(sessionId, new SnakeId(id, idx));
        GameState state = get(id);

        webSocketService.update(state);
        webSocketService.updateHighscore(state.getWidth()*state.getHeight());
        webSocketService.updateGlobalHighscore();
        webSocketService.publishIdx(sessionId, idx);
    }

    public void move(String sessionId, Move move) {
        SnakeId snakeId = session2id(sessionId);
        get(snakeId.getId()).turn(snakeId.getIdx(), move);
    }

    public void setName(String sessionId, String name) {
        SnakeId snakeId = session2id(sessionId);
        GameState state = get(snakeId.getId());
        state.changeName(snakeId.getIdx(), name);

        webSocketService.update(state);
    }

    public void addAI(String sessionId, String key) {
        SnakeId snakeId = session2id(sessionId);
        GameState state = get(snakeId.getId());
        Autopilot autopilot;

        String type = aiDescriptionMap.get(key);
        if(Objects.equals(type, "greedy")) {
            autopilot = new GreedyAutopilot();
        } else if (type.startsWith("models/")) {
            autopilot = new KerasModel(type);
        } else {
            autopilot = new RandomAutopilot();
        }
        state.addAISnake(autopilot);

        webSocketService.update(state);
    }

    public static Map<String, String> aiDescriptions() {
        var out = new HashMap<String, String>();

        out.put("Random", "random");
        out.put("Greedy", "greedy");

        // TODO: maybe just read the folder ...
        out.put("Deep Q (n=200)", "models/DQN_200.keras");
        // out.put("Actor-Critic (n=1000)", "models/AC_1000.keras");

        return out;
    }

    public List<String> listAi() {
        return new ArrayList<>(aiDescriptionMap.keySet());
    }
}
