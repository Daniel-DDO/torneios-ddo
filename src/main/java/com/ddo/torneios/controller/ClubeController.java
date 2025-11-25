package com.ddo.torneios.controller;

import com.ddo.torneios.service.ClubeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clube")
public class ClubeController {

    @Autowired
    private ClubeService clubeService;

}
