package com.wash.iot.controller;

import com.wash.iot.entity.User;
import com.wash.iot.enums.UserRole;
import com.wash.iot.repository.UserRepository;
import com.wash.iot.security.JwtTokenProvider;
import com.wash.iot.util.DeviceUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.show-sql=false",
                "mqtt.broker-url=tcp://127.0.0.1:1",
                "mqtt.client-id=test-client",
                "mqtt.default-topic=status/+"
        }
)
@AutoConfigureMockMvc
public class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private User user;
    private String token;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User u = new User();
        u.setOpenId("test_openid");
        u.setNickName("旧昵称");
        u.setGender(0);
        u.setRole("CONSUMER");
        u = userRepository.save(u);
        this.user = u;
        this.token = tokenProvider.generateToken(u.getId(), UserRole.CONSUMER);
    }

    @Test
    void getProfile_withoutToken_returns401Code() throws Exception {
        mockMvc.perform(get("/api/v1/user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void getProfile_withToken_returnsUserInfo() throws Exception {
        mockMvc.perform(get("/api/v1/user/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.nickName").value("旧昵称"));
    }

    @Test
    void updateProfile_updatesNicknameAndGender() throws Exception {
        mockMvc.perform(post("/api/v1/user/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickName\":\"新昵称\",\"gender\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.nickName").value("新昵称"))
                .andExpect(jsonPath("$.data.gender").value(1));
    }

    @Test
    void consumerDevices_withAdminToken_returnsOk() throws Exception {
        String adminToken = tokenProvider.generateToken(user.getId(), UserRole.ADMIN);
        mockMvc.perform(get("/api/v1/consumer/devices")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void uploadAvatar_setsAvatarUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}
        );

        mockMvc.perform(multipart("/api/v1/user/profile/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.avatarUrl").value(org.hamcrest.Matchers.containsString("/uploads/avatars/" + user.getId() + "/")));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        String avatarUrl = updated.getAvatarUrl();
        if (avatarUrl != null) {
            int idx = avatarUrl.indexOf("/uploads/");
            if (idx >= 0) {
                String publicPath = avatarUrl.substring(idx + 1);
                Path local = Paths.get(System.getProperty("user.dir")).resolve(publicPath).toAbsolutePath().normalize();
                Files.deleteIfExists(local);
                Path parent = local.getParent();
                if (parent != null && Files.isDirectory(parent) && parent.toFile().list() != null && parent.toFile().list().length == 0) {
                    Files.deleteIfExists(parent);
                }
            }
        }
    }

    @Test
    void parseDeviceSnFromQr_directSn_returnsSn() {
        assertEquals("WASH_251226_0001", DeviceUtils.parseDeviceSnFromQr("WASH_251226_0001"));
    }

    @Test
    void parseDeviceSnFromQr_json_returnsSn() {
        assertEquals("WASH_251226_ABCD", DeviceUtils.parseDeviceSnFromQr("{\"deviceSn\":\"WASH_251226_ABCD\",\"type\":\"WASHING_MACHINE\"}"));
    }

    @Test
    void parseDeviceSnFromQr_urlQuery_returnsSn() {
        assertEquals("WASH_251226_ZZZZ", DeviceUtils.parseDeviceSnFromQr("https://example.com/bind?deviceSn=WASH_251226_ZZZZ"));
    }
}
