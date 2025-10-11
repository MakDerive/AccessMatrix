package org.example.accessmatrix.service;

import lombok.AllArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;

@Service
public class MatrixService {

    HashMap<String,HashMap<String,String>> matrix;

    public HashMap<String,HashMap<String,String>> getMatrix(){
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<HashMap<String,HashMap<String,String>>> response = restTemplate.exchange(
                "http://194.87.94.159/supersec/api.php?action=get",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<HashMap<String,HashMap<String,String>>>() {}
        );
        matrix = response.getBody();
        return matrix;
    }


}
