package com.example.ochapauthentication.routes;

import com.example.ochapauthentication.commands.AccountRegisterCommand;
import com.example.ochapauthentication.commands.AuthLoginCommand;
import com.example.ochapauthentication.dto.AccountInfoDTO;
import com.example.ochapauthentication.dto.ErrorDTO;
import com.example.ochapauthentication.dto.ErrorsDTO;
import com.example.ochapauthentication.dto.TokenDTO;
import com.example.ochapauthentication.entities.Role;
import com.example.ochapauthentication.dto.ErrorDTO;
import com.example.ochapauthentication.dto.ErrorsDTO;
import com.example.ochapauthentication.dto.TokenDTO;
import com.example.ochapauthentication.entities.Role;
import com.example.ochapauthentication.dto.TokenDTO;
import com.example.ochapauthentication.entities.User;
import com.example.ochapauthentication.jwt.JwtTokenUtil;
import com.example.ochapauthentication.repository.RoleDAO;
import com.example.ochapauthentication.repository.UserDAO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

@Controller
public class AuthenticationController {

    final UserDAO userRepository;
    final RoleDAO roleRepository;
    final ObjectMapper objectMapper;

    final int MIN_USERNAME_LENGTH = 3;
    final int MIN_PASSWORD_LENGTH = 8;

    @Autowired
    AuthenticationController(UserDAO userRepository, RoleDAO roleRepository){
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Autowired
    private JwtTokenUtil jwt;

    @PostMapping("/auth/login")
    @ResponseBody
    ResponseEntity<String> loginAutentication(@RequestBody AuthLoginCommand credentials) throws JsonProcessingException {
        Optional<User> user = userRepository.getUserByUsername(credentials.getUsername());

        if (user.isEmpty() || !Arrays.equals(hashPassword(credentials.getPassword(), user.get().getSalt()),
                                             user.get().getPassword())) {
            com.example.ochapauthentication.dto.Error incorrectCredentials =
                    new com.example.ochapauthentication.dto.Error("Incorrect credentials");

            return new ResponseEntity<>(objectMapper.writeValueAsString(incorrectCredentials), HttpStatus.FORBIDDEN);
        }

        // Generate JWT
        User u = user.get();
        AccountInfoDTO accountInfo = new AccountInfoDTO(u.getId(), u.getUsername(), u.getRole().getName());

        TokenDTO tokenDTO = new TokenDTO(new JwtTokenUtil().generateToken(accountInfo), accountInfo);

        // Generate JWT
        User u = user.get();
        AccountInfoDTO accountInfo = new AccountInfoDTO();
        accountInfo.setUsername(u.getUsername());
        accountInfo.setId(u.getId());
        accountInfo.setRole(u.getRole().getName());

        TokenDTO tokenDTO = new TokenDTO();
        String token = jwt.generateToken(accountInfo);
        System.out.println(token);
        tokenDTO.setToken(token);
        tokenDTO.setAccountInfo(accountInfo);

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(tokenDTO);
        AccountInfoDTO account = new AccountInfoDTO(
                user.get().getId(),
                user.get().getUsername(),
                user.get().getRole().getName());
        TokenDTO payload = new TokenDTO("PLACEHOLDER", account); //FIXME place a valid JWT token
        return new ResponseEntity<>(objectMapper.writeValueAsString(payload), HttpStatus.OK);
    }

    @PostMapping(value = "/accounts/register",  produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<String> registerAutentication(@RequestBody AccountRegisterCommand credentials) throws JsonProcessingException {

        boolean isError = false;
        ArrayList<ErrorDTO> errorList = new ArrayList<>();
        if(credentials.getUsername() == null || credentials.getUsername().length() < MIN_USERNAME_LENGTH) {
            isError = true;
            errorList.add(new ErrorDTO("username", "Must be at least " + MIN_USERNAME_LENGTH + " characters long"));
        }

        if(credentials.getPassword() == null || credentials.getPassword().length() < MIN_USERNAME_LENGTH) {
            isError = true;
            errorList.add(new ErrorDTO("password", "Must be at least " + MIN_PASSWORD_LENGTH + " characters long"));
        }

        if(isError){
            ErrorsDTO errors = new ErrorsDTO(errorList);
            return new ResponseEntity<>(objectMapper.writeValueAsString(errors), HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (userRepository.getUserByUsername(credentials.getUsername()).isPresent()) {
            com.example.ochapauthentication.dto.Error alreadyExisting =
            new com.example.ochapauthentication.dto.Error("The username already exist");

            return new ResponseEntity<>(objectMapper.writeValueAsString(alreadyExisting), HttpStatus.CONFLICT);
        }

        User user = new User();
        user.setUsername(credentials.getUsername());

        byte[] salt = generateSalt();

        user.setPassword(hashPassword(credentials.getPassword(), salt));
        user.setSalt(salt);
        Role role = roleRepository.findByName("user");
        user.setRole(role);
        User userCreated = userRepository.save(user);

        AccountInfoDTO accountInfo = new AccountInfoDTO(
                user.getId(),
                user.getUsername(),
                user.getRole().getName());

        ObjectMapper objectMapper = new ObjectMapper();

        return new ResponseEntity<>(objectMapper.writeValueAsString(accountInfo), HttpStatus.CREATED);
    }

    private byte[] hashPassword(String password, byte[] salt){

        try{
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(salt);
            return md.digest(password.getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    private byte[] generateSalt(){
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }
}
