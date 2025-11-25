package com.ddo.torneios.controller;

import com.ddo.torneios.service.CompeticaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/competicao")
public class CompeticaoController {

    @Autowired
    private CompeticaoService competicaoService;

}
