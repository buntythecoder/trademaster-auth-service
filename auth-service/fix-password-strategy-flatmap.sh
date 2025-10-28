#!/bin/bash
# Batch fix flatMap signatures in PasswordAuthenticationStrategy.java

FILE="src/main/java/com/trademaster/auth/strategy/PasswordAuthenticationStrategy.java"

echo "Fixing flatMap signatures in PasswordAuthenticationStrategy..."

# Fix findUserByEmail
sed -i '140,154s/private Function<Result<AuthenticationContext, String>, Result<AuthenticatedUserContext, String>> findUserByEmail()/private Function<AuthenticationContext, Result<AuthenticatedUserContext, String>> findUserByEmail()/' "$FILE"
sed -i '141s/return result -> result\.flatMap(context ->/return context ->/' "$FILE"
sed -i '154s/);$/;/' "$FILE"

# Fix authenticateCredentials
sed -i '160,174s/private Function<Result<AuthenticatedUserContext, String>, Result<AuthenticatedUserContext, String>> authenticateCredentials()/private Function<AuthenticatedUserContext, Result<AuthenticatedUserContext, String>> authenticateCredentials()/' "$FILE"
sed -i '161s/return result -> result\.flatMap(context ->/return context ->/' "$FILE"
sed -i '172s/? Result\.success/? Result.<AuthenticatedUserContext, String>success/' "$FILE"
sed -i '174s/);$/);/' "$FILE"

# Fix checkAccountStatus
sed -i '192,198s/private Function<Result<AuthenticatedUserContext, String>, Result<AuthenticatedUserContext, String>> checkAccountStatus()/private Function<AuthenticatedUserContext, Result<AuthenticatedUserContext, String>> checkAccountStatus()/' "$FILE"
sed -i '193s/return result -> result\.flatMap(context ->/return context ->/' "$FILE"
sed -i '198s/);$/;/' "$FILE"

# Fix generateTokens signature
sed -i '204s/private Function<Result<AuthenticatedUserContext, String>, Result<TokenGenerationContext, String>> generateTokens()/private Function<AuthenticatedUserContext, Result<TokenGenerationContext, String>> generateTokens()/' "$FILE"
sed -i '205s/return result -> result\.flatMap(context ->/return context ->/' "$FILE"

echo "Fixed PasswordAuthenticationStrategy flatMap signatures"
