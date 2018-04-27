package org.baeldung.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Created by hxy on 2018/4/24.
 * E-mail:hxyHelloWorld@163.com
 * github:https://github.com/haoxiaoyong1014
 */
@RestController
@RequestMapping(value = "user")
public class UserController {
    @RequestMapping(value = "me")
    public Principal user(Principal principal) {
        System.out.println(principal);
        return principal;
    }
}
