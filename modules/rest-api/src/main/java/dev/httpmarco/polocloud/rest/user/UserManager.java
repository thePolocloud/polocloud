package dev.httpmarco.polocloud.rest.user;

import at.favre.lib.crypto.bcrypt.BCrypt;
import dev.httpmarco.polocloud.rest.RestAPI;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Getter
@Accessors(fluent = true)
public class UserManager {

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final List<User> users = new ArrayList<>();
    private final UserConfiguration userConfiguration;
    private final RestAPI restAPI;

    //TODO async all code
    //TODO perms
    public UserManager(RestAPI restAPI) {
        this.restAPI = restAPI;

        this.userConfiguration = new UserConfiguration();
        var configUsers = this.userConfiguration.load();
        if (!configUsers.isEmpty()) {
            this.users.addAll(configUsers);
        }
    }

    public String createUser(String name) {
        var passwordGenerator = new KeyGenerator.KeyGeneratorBuilder()
                .usePunctuation(true)
                .useDigits(true)
                .useLower(true)
                .useUpper(true)
                .build();

        var uuid = UUID.randomUUID();
        var password = passwordGenerator.generate(12);
        var user = new User(name, hash(password), uuid);

        addUser(user);
        this.restAPI.logger().info("Successfully created User \"{}\" with ID \"{}\"", name, uuid);
        return password;
    }

    public CompletableFuture<String> login(User user, String password, String ip, String userAgent) {
        return CompletableFuture.supplyAsync(() -> {
            boolean valid = BCrypt.verifyer().verify(password.toCharArray(), user.hashedPassword()).verified;
            if (!valid) return null;

            this.restAPI.logger().info("\"{}\" logged in from a new IP address!", user.username());
            return generateAndStoreToken(user, ip, userAgent).join();
        }, this.executor);
    }

    private CompletableFuture<String> generateAndStoreToken(User user, String ip, String userAgent) {
        return CompletableFuture.supplyAsync(() -> {
            var tokenGenerator = new KeyGenerator.KeyGeneratorBuilder()
                    .useDigits(true)
                    .useLower(true)
                    .useUpper(true)
                    .build();

            var generatedToken = tokenGenerator.generate(32);
            CompletableFuture.runAsync(() -> {
                var token = new Token(generatedToken, hash(ip), hash(userAgent));

                synchronized (this.users) {
                    user.tokens().add(token);
                    updateUser(user);
                }
            });

            return generatedToken;
        }, this.executor);
    }

    public String checkToken(User user, String ip) {
        var validToken = user.tokens().stream()
                .filter(t -> BCrypt.verifyer().verify(ip.toCharArray(), t.hashedLastIp().toCharArray()).verified)
                .filter(t -> (System.currentTimeMillis() - t.created()) <= TimeUnit.DAYS.toMillis(7))
                .findFirst();

        return validToken.map(Token::token).orElse(null);

    }

    public void deleteUser(User user) {
        removeUser(user);
    }

    private void updateUser(User user) {
        this.users.stream().filter(u -> u.uuid().equals(user.uuid())).findFirst().ifPresent(us -> {
            removeUser(us);
            addUser(user);
        });
    }

    private void addUser(User user) {
        this.users.add(user);
        this.userConfiguration.save(users);
    }

    private void removeUser(User user) {
        this.users.remove(user);
        this.userConfiguration.save(users);
    }

    public User findUserByToken(String token, String ip) {
        return this.users.stream().filter(user -> user.tokens().stream()
                .filter(it -> BCrypt.verifyer().verify(ip.toCharArray(), it.hashedLastIp().toCharArray()).verified)
                .map(Token::toString)
                .toList().contains(token)).findFirst().orElse(null);
    }

    public User findeUserByUUID(UUID uuid) {
        return this.users.stream().filter(user -> user.uuid().equals(uuid)).findFirst().orElse(null);
    }

    private String hash(String content) {
        return BCrypt.withDefaults().hashToString(12, content.toCharArray());
    }
}
