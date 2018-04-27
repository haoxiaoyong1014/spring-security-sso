# spring-security-sso
简单的单点登录Spring Security OAuth2

1.概述
在本教程中，我们将讨论如何使用Spring Security OAuth和Spring Boot实现单点登录 - 单点登录。

我们将使用三个独立的应用程序

授权服务器 - 这是中央认证机制
两个客户端应用程序：使用SSO的应用程序
简而言之，当用户尝试访问客户端应用程序中的安全页面时，他们将首先通过身份验证服务器重定向到进行身份验证。

我们将使用OAuth2中的授权码授权类型来驱动授权委派。

2.客户端应用程序
我们从我们的客户端应用程序开始; 我们当然会使用Spring Boot来最小化配置：

2.1。Maven的依赖
首先，我们将在我们的pom.xml中需要以下依赖项：


         <dependency>
             <groupId>org.springframework.boot</groupId>
             <artifactId>spring-boot-starter-web</artifactId>
         </dependency>
         
         <dependency>
             <groupId>org.springframework.boot</groupId>
             <artifactId>spring-boot-starter-security</artifactId>
         </dependency>
 
         <dependency>
             <groupId>org.springframework.security.oauth</groupId>
             <artifactId>spring-security-oauth2</artifactId>
         </dependency>
 
         <dependency>
             <groupId>org.springframework.boot</groupId>
             <artifactId>spring-boot-starter-thymeleaf</artifactId>
         </dependency>
 
         <dependency>
             <groupId>org.thymeleaf.extras</groupId>
             <artifactId>thymeleaf-extras-springsecurity4</artifactId>
         </dependency>

2.2。安全配置
接下来，最重要的部分是我们客户端应用程序的安全配置：
``` python
@Configuration
 @EnableOAuth2Sso
 public class UiSecurityConfig extends WebSecurityConfigurerAdapter {   
     @Override
     public void configure(HttpSecurity http) throws Exception {
         http.antMatcher("/**")
           .authorizeRequests()
           .antMatchers("/", "/login**")
           .permitAll()
           .anyRequest()
           .authenticated();
     }
 }
 ```

当然，这种配置的核心部分是@ EnableOAuth2Sso注释，我们用它来启用单点登录。

请注意，我们需要扩展WebSecurityConfigurerAdapter - 如果没有它，所有的路径都将得到保护 - 所以用户在尝试访问任何页面时将被重定向到登录。在我们这里的情况下，index和login页面是唯一可以在没有身份验证的情况下访问的页面。

最后，我们还定义了一个RequestContextListener bean来处理请求范围。

和application.yml：

```
server:
    port: 8082
    context-path: /ui
    session:
      cookie:
        name: UISESSION
security:
  basic:
    enabled: false
  oauth2:
    client:
      clientId: SampleClientId
      clientSecret: secret
      accessTokenUri: http://localhost:8081/auth/oauth/token
      userAuthorizationUri: http://localhost:8081/auth/oauth/authorize
    resource:
      userInfoUri: http://localhost:8081/auth/user/me
spring:
  thymeleaf:
    cache: false
```    
> 几个简要说明：

我们禁用了默认的基本认证
accessTokenUri是获取访问令牌的URI
userAuthorizationUri是用户将被重定向到的授权URI
userInfoUri用户端点的URI以获取当前用户详细信息
另请注意，在我们的例子中，我们推出了我们的授权服务器，但我们当然也可以使用其他第三方提供商，如Facebook或GitHub。

2.3。前端
现在，让我们看看我们的客户端应用程序的前端配置。我们在这里不会专注于此，主要是因为我们已经在网站上报道过。

我们的客户端应用程序有一个非常简单的前端; 这里是index.html：

```
<h1>Spring Security SSO</h1>
<a href="securedPage">Login</a>
```
和securedPage.html：
```
<h1>Secured Page</h1>
Welcome, <span th:text="${#authentication.name}">Name</span>
```
该securedPage.html需要用户页面进行认证。如果未经过身份验证的用户尝试访问securedPage.html，他们将首先被重定向到登录页面。

3.身份验证服务器
现在让我们在这里讨论我们的授权服务器。

3.1。Maven的依赖
首先，我们需要在我们的pom.xml中定义依赖关系：
```
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.security.oauth</groupId>
    <artifactId>spring-security-oauth2</artifactId>
</dependency>
```
3.2。OAuth配置
理解我们要在这里一起运行授权服务器和资源服务器是一个单独的可部署单元，这一点很重要。

让我们从配置我们的资源服务器开始 - 这是我们主要的Boot应用程序的两倍：
```
@SpringBootApplication
@EnableResourceServer
public class AuthorizationServerApplication extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(AuthorizationServerApplication.class, args);
    }
}
```
然后，我们将配置我们的授权服务器：
```
@Configuration
@EnableAuthorizationServer
public class AuthServerConfig extends AuthorizationServerConfigurerAdapter {
    @Autowired
    private AuthenticationManager authenticationManager;
 
    @Override
    public void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
        oauthServer.tokenKeyAccess("permitAll()")
          .checkTokenAccess("isAuthenticated()");
    }
 
    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.inMemory()
          .withClient("SampleClientId")
          .secret("secret")
          .authorizedGrantTypes("authorization_code")
          .scopes("user_info")
          .autoApprove(true) ; 
    }
 
    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints.authenticationManager(authenticationManager);
    }
}
```
请注意我们如何使用authorization_code授权类型来启用简单的客户端。

此外，请注意autoApprove如何设置为true，以便我们不会重定向并提升为手动批准任何范围。

3.3。安全配置
首先，我们将通过我们的application.properties禁用默认的基本认证：
```
server.port=8081
server.context-path=/auth
security.basic.enabled=false
```
现在，让我们转到配置并定义一个简单的表单登录机制：
```
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
 
    @Autowired
    private AuthenticationManager authenticationManager;
 
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.requestMatchers()
          .antMatchers("/login", "/oauth/authorize")
          .and()
          .authorizeRequests()
          .anyRequest().authenticated()
          .and()
          .formLogin().permitAll();
    }
 
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.parentAuthenticationManager(authenticationManager)
          .inMemoryAuthentication()
          .withUser("john").password("123").roles("USER");
    }
}
```
请注意，我们使用了简单的内存认证，但我们可以简单地将其替换为自定义的userDetailsS​​ervice。

3.4。用户端点
最后，我们将创建我们之前在我们的配置中使用的用户端点：
```
@RestController
public class UserController {
    @GetMapping("/user/me")
    public Principal user(Principal principal) {
        return principal;
    }
}
```
当然，这将以JSON表示形式返回用户数据。

**参考资料:**

http://www.baeldung.com/sso-spring-security-oauth2