package org.example.accessmatrix.controller;

import lombok.AllArgsConstructor;
import org.example.accessmatrix.service.MatrixService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

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
        Map<String, Map<String,String>> matrix = matrixService.getMatrix();
        System.out.println(matrix);
        Set<String> files = matrix.keySet();
        Set<String> users = matrixService.getUsersMatrix();
        model.addAttribute("matrixFiles",files);
        model.addAttribute("matrixUsers",users);
        model.addAttribute("matrix",matrix);
        return "index";
    }

    @GetMapping(value = "/test.txt", produces = "text/plain")
    public ResponseEntity<String> getTextFile() {
        try {
            Resource resource = new ClassPathResource("/static/test.txt");
            String content = new String(Files.readAllBytes(resource.getFile().toPath()));
            return ResponseEntity.ok(content);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
        }
    }

}
