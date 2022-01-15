package com.example.ochapauthentication.routes;

import com.example.ochapauthentication.commands.AccountRegisterCommand;
import com.example.ochapauthentication.commands.AuthLoginCommand;
import com.example.ochapauthentication.dto.AccountInfoDTO;
import com.example.ochapauthentication.entities.Role;
import com.example.ochapauthentication.entities.User;
import com.example.ochapauthentication.repository.RoleDAO;
import com.example.ochapauthentication.repository.UserDAO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class AuthenticationController {

    final UserDAO userRepository;
    final RoleDAO roleRepository;

    @Autowired
    AuthenticationController(UserDAO userRepository, RoleDAO roleRepository){
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }


    @PostMapping("/auth/login")
    @ResponseBody
    String loginAutentication(@RequestBody AuthLoginCommand credentials){
        Optional<User> user = userRepository.getUserByUsername(credentials.getUsername());
        if (user.isEmpty()){
            return "{\"error\" : \"user don't exist\"}";
        }

        if (hashPassword(credentials.getPassword(), user.get().getSalt())
            != user.get().getPassword()) {
            return "{\"error\" : \"wrong password\"}";
        }
        return "{\"success\" : \"logged\"}";
    }

    @PostMapping(value = "/accounts/register",  produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    String registerAutentication(@RequestBody AccountRegisterCommand credentials) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

    if (userRepository.getUserByUsername(credentials.getUsername()).isPresent()) {
      //TODO
    }

        User user = new User();
        user.setUsername(credentials.getUsername());

        byte[] salt = generateSalt();

        user.setPassword(hashPassword(credentials.getPassword(), salt));
        user.setSalt(salt);
        Role role = roleRepository.findByName("user");
        user.setRole(role);
        User userCreated = userRepository.save(user);



        AccountInfoDTO accountInfo = new AccountInfoDTO();
        accountInfo.setId(userCreated.getId());
        accountInfo.setUsername(userCreated.getUsername());
        accountInfo.setRole(userCreated.getRole().getName());

        return objectMapper.writeValueAsString(accountInfo);
    }

    private byte[] hashPassword(String password, byte[] salt){

        try{
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(salt);
            return md.digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "ERROR".getBytes(StandardCharsets.UTF_8);
        }
    }

    private byte[] generateSalt(){
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }
}
