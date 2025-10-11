package org.example.accessmatrix.controller;

import lombok.AllArgsConstructor;
import org.example.accessmatrix.service.MatrixService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;

@Controller
@AllArgsConstructor
public class ContentController {

    MatrixService matrixService;

    @GetMapping("/")
    public String index(){
        return "index";
    }

    @GetMapping("/getMatrix")
    public String getMatrix(Model model){
        HashMap<String, HashMap<String,String>> matrix = matrixService.getMatrix();
        model.addAttribute("matrix",matrix);
        return "index";
    }

}
