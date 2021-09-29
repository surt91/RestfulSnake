package me.schawe.multijsnake;

import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.*;

@RestController
public class RestfulSnakeController {
    private final GameStateMap map;
    private final WebSocketService webSocketService;

    RestfulSnakeController(GameStateMap map, WebSocketService webSocketService) {
        this.map = map;
        this.webSocketService = webSocketService;
    }

    @PostMapping("/api/init/{w}/{h}")
    GameState init(@PathVariable int w, @PathVariable int h) {
        return map.newGameState(w, h);
    }

    @PostMapping("/api/init")
    GameState init() {
        return init(10, 10);
    }

    @MessageMapping("/pause")
    void pause(@Header("simpSessionId") String sessionId) {
        map.pause(sessionId);
    }

    @MessageMapping("/unpause")
    void unpause(@Header("simpSessionId") String sessionId) {
        map.unpause(sessionId);
    }

    @MessageMapping("/reset")
    void reset(@Header("simpSessionId") String sessionId) {
        map.reset(sessionId);
    }

    @MessageMapping("/join")
    void join(@Header("simpSessionId") String sessionId, String id) {
        map.join(sessionId, id);
    }

    @MessageMapping("/move")
    void move(@Header("simpSessionId") String sessionId, Move move) {
        map.move(sessionId, move);
    }

    @MessageMapping("/setName")
    void setName(@Header("simpSessionId") String sessionId, String name) {
        map.setName(sessionId, name);
    }
}
