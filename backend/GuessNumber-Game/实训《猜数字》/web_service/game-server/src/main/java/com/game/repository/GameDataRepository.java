// com.game.repository.GameDataRepository.java
package com.game.repository;

import com.game.entity.GameData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GameDataRepository extends JpaRepository<GameData, Long> {
    // 按客户端类型查询
    List<GameData> findByClientType(String clientType);

    // 按客户端ID查询
    GameData findByClientId(String clientId);
}