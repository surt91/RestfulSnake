package me.schawe.multijsnake.gamemanagement;

import me.schawe.multijsnake.snake.GameState;
import me.schawe.multijsnake.snake.SnakeId;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {
    private final GameStateMap gameStateMap;
    private final WebSocketService webSocketService;

    public WebSocketEventListener(GameStateMap gameStateMap, WebSocketService webSocketService) {
        this.gameStateMap = gameStateMap;
        this.webSocketService = webSocketService;
    }

    @EventListener
    public void onDisconnectEvent(SessionDisconnectEvent event) {
        SnakeId snakeId;
        try {
            snakeId = gameStateMap.session2id(event.getSessionId());
        } catch (InvalidMapException e) {
            return;
        }

        GameState gameState = gameStateMap.get(snakeId.getId());
        gameState.kill(snakeId);
        gameState.markForRemoval(snakeId);
        webSocketService.update(gameState);
    }
}