package edu.farmingdale.advancedprogramming_capstone_project;

import com.azure.communication.identity.CommunicationIdentityClient;
import com.azure.communication.identity.CommunicationIdentityClientBuilder;
import com.azure.communication.identity.models.CommunicationUserIdentifierAndToken;
import com.azure.communication.identity.models.CommunicationTokenScope;
import java.util.Arrays;
import java.time.Duration;

/**
 * TokenGeneratorService creates a new ACS user identity and generates an access token.
 * This simplifies token generation for testing purposes.
 */
public class TokenGeneratorService {

    private final CommunicationIdentityClient identityClient;

    /**
     * Constructs the service using the provided ACS connection string.
     * @param connectionString The ACS connection string.
     */
    public TokenGeneratorService(String connectionString) {
        identityClient = new CommunicationIdentityClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }

    /**
     * Generates a new user access token with the "voip" scope, valid for 1 hour.
     * @return A TokenInfo object containing the user ID and access token.
     */
    public TokenInfo generateToken() {
        CommunicationUserIdentifierAndToken userAndToken = identityClient.createUserAndToken(
                Arrays.asList(CommunicationTokenScope.VOIP),
                Duration.ofHours(1)
        );
        return new TokenInfo(userAndToken.getUser().getId(), userAndToken.getUserToken().getToken());
    }

    /**
     * TokenInfo holds the user ID and token.
     */
    public static class TokenInfo {
        private final String userId;
        private final String token;

        public TokenInfo(String userId, String token) {
            this.userId = userId;
            this.token = token;
        }

        public String getUserId() {
            return userId;
        }

        public String getToken() {
            return token;
        }
    }
}
