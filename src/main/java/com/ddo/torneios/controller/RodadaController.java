package com.ddo.torneios.controller;

import com.ddo.torneios.service.RodadaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rodada")
public class RodadaController {

    @Autowired
    private RodadaService rodadaService;
}
