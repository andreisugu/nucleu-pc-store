package ro.chaonius.pcgarage.auth;

record AccountResponse(String email, UserRole role) {

    static AccountResponse from(Account account) {
        return new AccountResponse(account.getEmail(), account.getRole());
    }
}
