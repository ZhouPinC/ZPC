// com.game.config.WebSocketConfig.java
package com.game.config;

import com.game.handler.GameWebSocketHandler;
import com.game.repository.GameDataRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

// 注入 GameDataRepository（Spring 会自动查找并注入该 Bean）
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final GameDataRepository gameDataRepository;

    // 通过构造函数注入 GameDataRepository
    public WebSocketConfig(GameDataRepository gameDataRepository) {
        this.gameDataRepository = gameDataRepository;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 创建 GameWebSocketHandler 时传入构造器所需的 gameDataRepository
        registry.addHandler(new GameWebSocketHandler(gameDataRepository), "/gameSync")
                .setAllowedOrigins("*");
    }
}