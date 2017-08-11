package ua.com.juja.microservices.teams.slackbot.repository.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.teams.slackbot.exceptions.ApiError;
import ua.com.juja.microservices.teams.slackbot.exceptions.UserExchangeException;
import ua.com.juja.microservices.teams.slackbot.model.User;
import ua.com.juja.microservices.teams.slackbot.model.UserSlackNameRequest;
import ua.com.juja.microservices.teams.slackbot.model.UserUuidRequest;
import ua.com.juja.microservices.teams.slackbot.repository.UserRepository;
import ua.com.juja.microservices.teams.slackbot.util.SlackNameHandler;
import ua.com.juja.microservices.teams.slackbot.util.Utils;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

/**
 * @author Ivan Shapovalov
 */
@Repository
@Slf4j
@Profile({"production", "default"})
public class RestUserRepository implements UserRepository {
    private final RestTemplate restTemplate;
    @Value("${users.rest.api.version}")
    private String usersRestApiVersion;
    @Value("${users.baseURL}")
    private String usersUrlBase;
    @Value("${users.endpoint.usersBySlackNames}")
    private String usersUrlFindUsersBySlackNames;
    @Value("${users.endpoint.usersByUuids}")
    private String usersUrlFindUsersByUuids;

    @Inject
    public RestUserRepository(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<User> findUsersBySlackNames(List<String> slackNames) {
        log.debug("Received slackNames to convert : '{}'", slackNames);
        SlackNameHandler.addAtToSlackNames(slackNames);
        log.debug("Started creating userSlackNameRequest and HttpEntity");
        UserSlackNameRequest userSlackNameRequest = new UserSlackNameRequest(slackNames);
        HttpEntity<UserSlackNameRequest> request = new HttpEntity<>(userSlackNameRequest, Utils.setupJsonHttpHeaders());
        log.debug("Finished creating userSlackNameRequest and HttpEntity");

        String userServiceURL = usersUrlBase + usersRestApiVersion + usersUrlFindUsersBySlackNames;
        List<User> users = getUsers(request, userServiceURL);
        log.info("Found User: '{}' for slackNames: {}", users, slackNames);
        return users;
    }

    @Override
    public List<User> findUsersByUuids(List<String> uuids) {
        log.debug("Received uids to convert : '{}'", uuids);
        UserUuidRequest userUuidRequest = new UserUuidRequest(uuids);
        HttpEntity<UserUuidRequest> request = new HttpEntity<>(userUuidRequest, Utils.setupJsonHttpHeaders());
        log.debug("Finished creating userUuidsRequest and HttpEntity");
        String userServiceURL = usersUrlBase + usersRestApiVersion + usersUrlFindUsersByUuids;
        List<User> users = getUsers(request, userServiceURL);
        log.info("Found User:{} for uuids: {}", users, uuids);
        return users;
    }

    private <T> List<User> getUsers(HttpEntity<T> request, String userServiceURL) {
        List<User> users;
        try {
            log.debug("Started request to Users service url '{}'. Request is : '{}'",
                    userServiceURL, request.toString());
            ResponseEntity<User[]> response = restTemplate.exchange(userServiceURL,
                    HttpMethod.POST, request, User[].class);
            log.debug("Finished request to Users service. Response is: '{}'", response.toString());
            users = Arrays.asList(response.getBody());
        } catch (HttpClientErrorException ex) {
            ApiError error = Utils.convertToApiError(ex);
            log.warn("Users service returned an error: '{}'", error);
            throw new UserExchangeException(error, ex);
        }
        return users;
    }
}