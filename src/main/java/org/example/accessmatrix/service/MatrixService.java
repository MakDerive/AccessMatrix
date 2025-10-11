package org.example.accessmatrix.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class MatrixService {

    Map<String, Map<String, String>> matrix;

    public Map<String, Map<String, String>> getMatrix(){

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map<String,Map<String,String>>> response = restTemplate.exchange(
                "http://194.87.94.159/supersec/api.php?action=get",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String,Map<String,String>>>() {}
        );

        matrix = response.getBody();
        return matrix;
    }

    public Set<String> getUsersMatrix(){
        Set<String> users = new HashSet<>();
        for (Map<String,String> usersFromMatrix : matrix.values()) {
            users.addAll(usersFromMatrix.keySet());
        }
        return users;
    }


}
