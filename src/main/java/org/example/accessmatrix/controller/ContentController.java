package org.example.accessmatrix.controller;

import lombok.AllArgsConstructor;
import org.example.accessmatrix.service.MatrixService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Controller
@AllArgsConstructor
public class ContentController {

    MatrixService matrixService;
    private final RestTemplate restTemplate;

    private static final String CORRECT_ACCESSES_URL = "http://localhost:8080/test.txt";

    public static class VerificationResult {
        private List<String> errors = new ArrayList<>();
        private List<String> corrections = new ArrayList<>();
        private List<String> missingUsers = new ArrayList<>();
        private List<String> missingFiles = new ArrayList<>();
        private List<String> correctionsToApply = new ArrayList<>();

        public List<String> getErrors() { return errors; }
        public List<String> getCorrections() { return corrections; }
        public List<String> getMissingUsers() { return missingUsers; }
        public List<String> getMissingFiles() { return missingFiles; }
        public List<String> getCorrectionsToApply() { return correctionsToApply; }

        public boolean hasMissingUsers() { return !missingUsers.isEmpty(); }
        public boolean hasMissingFiles() { return !missingFiles.isEmpty(); }
        public boolean hasErrors() { return !errors.isEmpty(); }
        public boolean hasCorrections() { return !corrections.isEmpty(); }
        public boolean hasItemsToApply() { return !correctionsToApply.isEmpty(); }

        private List<String> missingUserErrors = new ArrayList<>();
        private List<String> missingFileErrors = new ArrayList<>();
        private List<String> missingUserCorrections = new ArrayList<>();
        private List<String> missingFileCorrections = new ArrayList<>();

        public List<String> getMissingUserErrors() { return missingUserErrors; }
        public List<String> getMissingFileErrors() { return missingFileErrors; }
        public List<String> getMissingUserCorrections() { return missingUserCorrections; }
        public List<String> getMissingFileCorrections() { return missingFileCorrections; }

        public boolean hasMissingUserErrors() { return !missingUserErrors.isEmpty(); }
        public boolean hasMissingFileErrors() { return !missingFileErrors.isEmpty(); }
        public boolean hasMissingUserCorrections() { return !missingUserCorrections.isEmpty(); }
        public boolean hasMissingFileCorrections() { return !missingFileCorrections.isEmpty(); }
    }

    @GetMapping("/")
    public String index(Model model){
        return "index";
    }

    @GetMapping("/getMatrix")
    public String getMatrix(Model model){
        Map<String, Map<String,String>> matrix = matrixService.getMatrix();
        Set<String> files = matrix.keySet();
        Set<String> users = matrixService.getUsersMatrix();
        model.addAttribute("matrixFiles",files);
        model.addAttribute("matrixUsers",users);
        model.addAttribute("matrix",matrix);
        return "index";
    }

    @GetMapping("/setDataMatrix")
    public String setDataMatrixForm(Model model) {
        model.addAttribute("message", "");
        return "setDataMatrix";
    }

    @PostMapping("/setDataMatrix")
    public String setDataMatrix(Model model, RedirectAttributes redirectAttributes) {
        try {
            String correctAccessesText = loadAccessesFromUrl();
            VerificationResult result = verifyAndFixAccessRights(correctAccessesText);

            redirectAttributes.addFlashAttribute("result", result);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Проверка завершена. Найдено расхождений: " + result.getCorrectionsToApply().size());

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при загрузке или обработке файла: " + e.getMessage());
        }
        return "redirect:/setDataMatrix";
    }

    @PostMapping("/setDataMatrix/apply")
    public String applyCorrections(@RequestParam(value = "correctionsToApply", required = false) List<String> correctionsToApply,
                                   RedirectAttributes redirectAttributes) {
        try {
            List<String> appliedCorrections = new ArrayList<>();

            if (correctionsToApply == null || correctionsToApply.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Не выбраны изменения для применения");
                return "redirect:/setDataMatrix";
            }

            for (String correction : correctionsToApply) {
                String[] parts = correction.split(" ");
                if (parts.length >= 3) {
                    String user = parts[0];
                    String file = parts[1];
                    String value = parts[2];

                    boolean success = matrixService.setAccess(user, file, value);
                    TimeUnit.MILLISECONDS.sleep(1000);

                    if (success) {
                        appliedCorrections.add("Установлен: " + user + " -> " + file + " = " + value);
                    } else {
                        appliedCorrections.add("Ошибка установки: " + user + " -> " + file + " = " + value);
                    }
                }
            }

            redirectAttributes.addFlashAttribute("appliedCorrections", appliedCorrections);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Применено изменений: " + appliedCorrections.size());

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при применении изменений: " + e.getMessage());
        }
        return "redirect:/setDataMatrix";
    }

    @GetMapping("/getUserAccesses")
    public String getUserAccessesForm(Model model) {
        model.addAttribute("userAccesses", new HashMap<>());
        return "getUserAccesses";
    }

    @PostMapping("/getUserAccesses")
    public String getUserAccesses(@RequestParam String username, Model model) {
        Map<String, String> userAccesses = matrixService.getUserAccesses(username);
        model.addAttribute("userAccesses", userAccesses);
        model.addAttribute("searchedUser", username);
        model.addAttribute("userExists", matrixService.userExists(username));
        return "getUserAccesses";
    }

    @GetMapping("/giveAccessToUser")
    public String giveAccessToUserForm(Model model) {
        model.addAttribute("message", "");
        return "giveAccessToUser";
    }

    @PostMapping("/giveAccessToUser")
    public String giveAccessToUser(@RequestParam String user,
                                   @RequestParam String file,
                                   @RequestParam String value,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            boolean userExists = matrixService.userExists(user);
            boolean fileExists = matrixService.fileExists(file);

            if (!userExists || !fileExists) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Ошибка: " +
                                (!userExists ? "Пользователь '" + user + "' не найден. " : "") +
                                (!fileExists ? "Файл '" + file + "' не найден." : ""));
                return "redirect:/giveAccessToUser";
            }

            boolean success = matrixService.setAccess(user, file, value);
            if (success) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Доступ успешно установлен: " + user + " -> " + file + " = " + value);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Ошибка при установке доступа");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка: " + e.getMessage());
        }
        return "redirect:/giveAccessToUser";
    }

    private String loadAccessesFromUrl() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(CORRECT_ACCESSES_URL, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Не удалось загрузить файл по URL: " + CORRECT_ACCESSES_URL);
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при загрузке файла: " + e.getMessage());
        }
    }

    private VerificationResult verifyAndFixAccessRights(String correctAccessesText) {
        VerificationResult result = new VerificationResult();

        String[] lines = correctAccessesText.split("\n");
        int lineNumber = 0;

        for (String line : lines) {
            lineNumber++;
            line = line.trim();
            if (line.isEmpty()) continue;

            try {
                String[] parts = line.split("\\s+");
                if (parts.length < 3) {
                    result.getErrors().add("Строка " + lineNumber + ": Неверный формат - '" + line + "'");
                    continue;
                }

                StringBuilder usernameBuilder = new StringBuilder();
                int i = 0;
                while (i < parts.length - 2) {
                    if (usernameBuilder.length() > 0) {
                        usernameBuilder.append(" ");
                    }
                    usernameBuilder.append(parts[i]);
                    i++;
                }

                String user = usernameBuilder.toString();
                String file = parts[parts.length - 2];
                String correctValue = parts[parts.length - 1];

                if (!correctValue.equals("+") && !correctValue.equals("-")) {
                    result.getErrors().add("Строка " + lineNumber + ": Неверное значение доступа - '" + correctValue + "'");
                    continue;
                }

                boolean userExists = matrixService.userExists(user);
                boolean fileExists = matrixService.fileExists(file);

                if (!userExists && !fileExists) {
                    String errorMsg = "User и File не найдены: " + user + " -> " + file + " " + correctValue;
                    result.getMissingUserErrors().add(errorMsg);
                    result.getMissingUserCorrections().add(user + " " + file + " " + correctValue);
                    result.getMissingFileCorrections().add(user + " " + file + " " + correctValue);
                    continue;
                }

                if (!userExists) {
                    String errorMsg = "User не найден: " + user + " -> " + file + " " + correctValue;
                    result.getMissingUserErrors().add(errorMsg);
                    result.getMissingUserCorrections().add(user + " " + file + " " + correctValue);
                    continue;
                }

                if (!fileExists) {
                    String errorMsg = "File не найден: " + user + " -> " + file + " " + correctValue;
                    result.getMissingFileErrors().add(errorMsg);
                    result.getMissingFileCorrections().add(user + " " + file + " " + correctValue);
                    continue;
                }

                String currentAccess = matrixService.getCurrentAccess(user, file);

                if (currentAccess == null || !currentAccess.equals(correctValue)) {
                    String correctionInfo = String.format("Расхождение: %s -> %s : должно быть %s (сейчас: %s)",
                            user, file, correctValue, currentAccess != null ? currentAccess : "null");
                    result.getCorrections().add(correctionInfo);
                    result.getCorrectionsToApply().add(user + " " + file + " " + correctValue);
                }
            } catch (Exception e) {
                result.getErrors().add("Строка " + lineNumber + ": Ошибка обработки - '" + line + "'");
            }
        }

        if (result.getCorrections().isEmpty() && result.getErrors().isEmpty() &&
                result.getMissingUserErrors().isEmpty() && result.getMissingFileErrors().isEmpty()) {
            result.getCorrections().add("Все доступы уже установлены правильно!");
        }

        return result;
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