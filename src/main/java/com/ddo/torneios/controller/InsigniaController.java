package com.ddo.torneios.controller;

import com.ddo.torneios.service.InsigniaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/insignia")
public class InsigniaController {

    @Autowired
    private InsigniaService insigniaService;


}
