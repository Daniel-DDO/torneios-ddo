package com.ddo.torneios.controller;

import com.ddo.torneios.service.TorneioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/torneio")
public class TorneioController {

    @Autowired
    private TorneioService torneioService;

}
