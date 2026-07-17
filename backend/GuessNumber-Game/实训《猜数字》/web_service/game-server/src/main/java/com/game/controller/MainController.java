// com.game.controller.MainController.java
package com.game.controller;

import com.game.entity.GameData;
import com.game.handler.GameWebSocketHandler;
import com.game.repository.GameDataRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class MainController {

    private final GameDataRepository gameDataRepository;
    private final GameWebSocketHandler webSocketHandler;

    // 构造函数注入
    public MainController(GameDataRepository gameDataRepository, GameWebSocketHandler webSocketHandler) {
        this.gameDataRepository = gameDataRepository;
        this.webSocketHandler = webSocketHandler;
    }

    // 前台游戏页面
    @GetMapping("/game")
    public String gamePage() {
        return "game";  // 对应game.html
    }

    // 登录页
    @GetMapping("/login")
    public String loginPage() {
        return "login";  // 对应login.html
    }

    // 后台管理页面
    @GetMapping("/admin")
    public String adminPage(Model model) {
        // 查询所有游戏数据
        List<GameData> allData = gameDataRepository.findAll();
        model.addAttribute("allData", allData);
        return "admin";  // 对应admin.html
    }

    // 后台更新数据接口
    @PostMapping("/admin/update")
    @ResponseBody
    public String updateData(@RequestBody GameData data) {
        try {
            data.setUpdateTime(LocalDateTime.now());
            gameDataRepository.save(data);
            // 同步广播更新
            webSocketHandler.broadcast(data);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    // 获取所有数据接口（供前端调用）- 整合了GameDataController的功能
    @GetMapping("/api/allData")
    @ResponseBody
    public List<GameData> getAllData() {
        List<GameData> allData = gameDataRepository.findAll(Sort.by(Sort.Direction.DESC, "updateTime"));

        // 调试：打印第一条数据
        if (!allData.isEmpty()) {
            GameData first = allData.get(0);
            System.out.println("第一条数据详情:");
            System.out.println("ID: " + first.getId());
            System.out.println("GameType: " + first.getGameType());
            System.out.println("Target: " + first.getTarget());
            System.out.println("Result: " + first.getResult());
            System.out.println("Attempts: " + first.getAttempts());
            System.out.println("CurrentHistory: " + first.getCurrentHistory());
        }

        return allData;
    }

    @PostMapping("/admin/clearAll")
    @ResponseBody
    public String clearAllData() {
        try {
            gameDataRepository.deleteAll();
            // 广播数据已清空
            webSocketHandler.broadcast(null); // 可以考虑发送一个特定的清空消息
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
}