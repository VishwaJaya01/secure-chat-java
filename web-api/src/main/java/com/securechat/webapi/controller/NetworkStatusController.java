package com.securechat.webapi.controller;

import com.securechat.webapi.telemetry.NetworkServiceRegistry;
import com.securechat.webapi.telemetry.NetworkServiceSnapshot;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/network")
@CrossOrigin(origins = "http://localhost:5173")
public class NetworkStatusController {

    private final NetworkServiceRegistry networkServiceRegistry;

    public NetworkStatusController(NetworkServiceRegistry networkServiceRegistry) {
        this.networkServiceRegistry = networkServiceRegistry;
    }

    @GetMapping("/services")
    public List<NetworkServiceSnapshot> getNetworkServices() {
        return networkServiceRegistry.snapshot();
    }
}
