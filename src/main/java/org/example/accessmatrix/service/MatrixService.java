package org.example.accessmatrix.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class MatrixService {

    Map<String, Map<String, String>> matrix;
    private final RestTemplate restTemplate;

    public MatrixService() {
        this.restTemplate = new RestTemplate();
    }

    public Map<String, Map<String, String>> getMatrix(){
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

    public boolean setAccess(String user, String file, String value) {
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("user", user);
            params.add("file", file);
            params.add("value", value);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "http://194.87.94.159/supersec/api.php?action=set",
                    request,
                    String.class
            );
            TimeUnit.MILLISECONDS.sleep(2000);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Map<String, String> getUserAccesses(String username) {
        Map<String, String> userAccesses = new HashMap<>();
        if (matrix == null) {
            getMatrix();
        }

        for (Map.Entry<String, Map<String, String>> fileEntry : matrix.entrySet()) {
            String file = fileEntry.getKey();
            Map<String, String> fileAccess = fileEntry.getValue();
            if (fileAccess.containsKey(username)) {
                userAccesses.put(file, fileAccess.get(username));
            }
        }
        return userAccesses;
    }

    public boolean userExists(String username) {
        if (matrix == null) {
            getMatrix();
        }
        return getUsersMatrix().contains(username);
    }

    public boolean fileExists(String filename) {
        if (matrix == null) {
            getMatrix();
        }
        return matrix.containsKey(filename);
    }

    public String getCurrentAccess(String user, String file) {
        if (matrix == null) {
            getMatrix();
        }
        return matrix.getOrDefault(file, new HashMap<>()).getOrDefault(user, null);
    }
}