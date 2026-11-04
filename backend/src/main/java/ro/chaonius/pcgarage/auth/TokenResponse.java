package ro.chaonius.pcgarage.auth;

record TokenResponse(String accessToken, String tokenType, long expiresIn, UserRole role) {
}
