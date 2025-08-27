package com.trademaster.auth.constants;

/**
 * Authentication Constants
 */
public final class AuthConstants {
    
    private AuthConstants() {}
    
    // Risk Score Constants
    public static final int DEFAULT_RISK_SCORE = 50;
    public static final int RISK_SCORE_SUCCESS = 0;
    public static final int RISK_SCORE_MULTIPLE_ATTEMPTS = 70;
    public static final int RISK_SCORE_NEW_DEVICE = 60;
    public static final int RISK_SCORE_LOCATION_CHANGE = 65;
    public static final int RISK_SCORE_ACCOUNT_BLOCKED = 100;
    public static final int RISK_SCORE_PENDING = 30;
    public static final int RISK_SCORE_LOGIN_FAILED = 40;
    
    // Risk Threshold Constants
    public static final int HIGH_RISK_THRESHOLD = 80;
    public static final int CRITICAL_RISK_THRESHOLD = 90;
    
    // Session Constants
    public static final long SESSION_TIMEOUT_MINUTES = 30;
    public static final int MAX_SESSIONS_PER_USER = 5;
    public static final String SESSION_KEY_PREFIX = "session:";
    
    // Token Constants
    public static final long ACCESS_TOKEN_VALIDITY_MS = 15 * 60 * 1000; // 15 minutes
    public static final long REFRESH_TOKEN_VALIDITY_MS = 7 * 24 * 60 * 60 * 1000; // 7 days
    
    // Rate Limiting Constants
    public static final int MAX_LOGIN_ATTEMPTS = 5;
    public static final long LOGIN_ATTEMPT_WINDOW_MS = 15 * 60 * 1000; // 15 minutes
    
    // Password Policy Constants
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 128;
    
    // MFA Constants
    public static final int MFA_CODE_LENGTH = 6;
    public static final int MFA_BACKUP_CODE_COUNT = 10;
    
    // AgentOS Status Constants
    public static final String STATUS_REGISTERED = "REGISTERED";
    public static final String STATUS_REGISTRATION_FAILED = "REGISTRATION_FAILED";
    public static final String STATUS_HEALTHY = "HEALTHY";
    public static final String STATUS_DEGRADED = "DEGRADED";
    public static final String STATUS_UNHEALTHY = "UNHEALTHY";
    public static final String STATUS_HEALTH_CHECK_FAILED = "HEALTH_CHECK_FAILED";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    
    // AgentOS Health Score Thresholds
    public static final double HEALTH_SCORE_HEALTHY = 0.8;
    public static final double HEALTH_SCORE_DEGRADED = 0.5;
    public static final double HEALTH_SCORE_CRITICAL = 0.3;
    
    // AgentOS Capability Constants
    public static final String CAPABILITY_USER_AUTHENTICATION = "USER_AUTHENTICATION";
    public static final String CAPABILITY_MULTI_FACTOR_AUTH = "MULTI_FACTOR_AUTH";
    public static final String CAPABILITY_SECURITY_AUDIT = "SECURITY_AUDIT";
    public static final String CAPABILITY_SESSION_MANAGEMENT = "SESSION_MANAGEMENT";
    public static final String CAPABILITY_DEVICE_TRUST = "DEVICE_TRUST";
    
    // AgentOS Proficiency Levels
    public static final String PROFICIENCY_EXPERT = "EXPERT";
    public static final String PROFICIENCY_ADVANCED = "ADVANCED";
    public static final String PROFICIENCY_INTERMEDIATE = "INTERMEDIATE";
    public static final String PROFICIENCY_BASIC = "BASIC";
    
    // AgentOS Agent Identity Constants
    public static final String AGENT_ID_AUTHENTICATION = "authentication-agent";
    public static final String AGENT_TYPE_AUTHENTICATION = "AUTHENTICATION";
    
    // AgentOS Performance Thresholds (milliseconds)
    public static final long PERFORMANCE_EXCELLENT_MS = 100L;
    public static final long PERFORMANCE_GOOD_MS = 300L;
    public static final long PERFORMANCE_AVERAGE_MS = 500L;
    public static final long PERFORMANCE_POOR_MS = 1000L;
    
    // AgentOS Timeout Constants
    public static final long DEFAULT_AGENT_TIMEOUT_MS = 30000L;
    public static final long HEALTH_CHECK_INTERVAL_MS = 30000L;
    public static final long CAPABILITY_MONITOR_INTERVAL_MS = 120000L;
    
    // AgentOS Request Context
    public static final String AGENT_REQUEST_PREFIX = "AgentOS-";
    public static final String AGENT_USER_AGENT = "TradeMaster-AuthAgent/1.0";
    public static final String AGENT_CONTEXT_HEADER = "X-Agent-Context";
    public static final String AGENT_CONTEXT_VALUE = "AuthenticationAgent";
    
    // Security Configuration Constants
    public static final int BCRYPT_STRENGTH = 12;
    public static final long CORS_MAX_AGE_SECONDS = 3600L;
    public static final boolean HIDE_USER_NOT_FOUND_EXCEPTIONS = false;
    
    // API Endpoint Constants  
    public static final String API_V1_AUTH = "/api/v1/auth";
    public static final String ENDPOINT_REGISTER = "/register";
    public static final String ENDPOINT_LOGIN = "/login";
    public static final String ENDPOINT_FORGOT_PASSWORD = "/forgot-password";
    public static final String ENDPOINT_RESET_PASSWORD = "/reset-password";
    public static final String ENDPOINT_VERIFY_EMAIL = "/verify-email";
    public static final String ENDPOINT_MFA = "/mfa/**";
    public static final String ENDPOINT_REFRESH = "/refresh";
    public static final String ENDPOINT_LOGOUT = "/logout";
    public static final String ENDPOINT_PROFILE = "/api/v1/profile/**";
    public static final String ENDPOINT_ADMIN = "/api/v1/admin/**";
    
    // CORS Origins
    public static final String[] ALLOWED_ORIGINS = {
        "http://localhost:3000",
        "http://localhost:8080", 
        "https://*.trademaster.com",
        "https://trademaster.com"
    };
    
    // HTTP Methods
    public static final String[] ALLOWED_METHODS = {"GET", "POST", "PUT", "DELETE", "OPTIONS"};
    
    // Validation Constants
    public static final int MIN_NAME_LENGTH = 1;
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_PHONE_LENGTH = 20;
    public static final int MAX_COUNTRY_CODE_LENGTH = 3;
    public static final int MAX_USER_AGENT_LENGTH = 200;
    
    // Virtual Thread Configuration
    public static final int AUTH_EXECUTOR_CONCURRENCY_LIMIT = 500;
    public static final int NOTIFICATION_EXECUTOR_CONCURRENCY_LIMIT = 200;
    public static final String VT_AUTH_PREFIX = "vt-auth-";
    public static final String VT_AUTH_SCHEDULER_PREFIX = "vt-auth-scheduler-";
    public static final String VT_AUTH_PROC_PREFIX = "vt-auth-proc-";
    public static final String VT_NOTIFICATION_PREFIX = "vt-notification-";
    
    // Logging Constants
    public static final String LOG_LEVEL_ERROR = "ERROR";
    public static final String LOG_LEVEL_WARN = "WARN";
    public static final String LOG_LEVEL_INFO = "INFO";
    public static final String LOG_LEVEL_DEBUG = "DEBUG";
    public static final String LOG_LEVEL_TRACE = "TRACE";
    
    // Risk Level Constants
    public static final String RISK_LEVEL_HIGH = "high";
    public static final String RISK_LEVEL_MEDIUM = "medium";
    public static final String RISK_LEVEL_LOW = "low";
    
    // Logging Field Constants
    public static final String CORRELATION_ID = "correlationId";
    public static final String USER_ID_FIELD = "userId";
    public static final String SESSION_ID_FIELD = "sessionId";
    public static final String IP_ADDRESS_FIELD = "ipAddress";
    public static final String USER_AGENT_FIELD = "userAgent";
    public static final String OPERATION_FIELD = "operation";
    public static final String DURATION_MS_FIELD = "durationMs";
    public static final String STATUS_FIELD = "status";
    public static final String SECURITY_EVENT_FIELD = "securityEvent";
    public static final String RISK_LEVEL_FIELD = "riskLevel";
    
    // Logging Operation Constants
    public static final String OPERATION_AUTHENTICATION_ATTEMPT = "authentication_attempt";
    public static final String OPERATION_AUTHENTICATION = "authentication";
    public static final String OPERATION_USER_REGISTRATION = "user_registration";
    public static final String OPERATION_SECURITY_INCIDENT = "security_incident";
    public static final String OPERATION_RATE_LIMIT_CHECK = "rate_limit_check";
    public static final String OPERATION_MFA_VERIFICATION = "mfa_verification";
    public static final String OPERATION_SESSION_MANAGEMENT = "session_management";
    public static final String OPERATION_TOKEN_MANAGEMENT = "token_management";
    public static final String OPERATION_API_REQUEST = "api_request";
    public static final String OPERATION_DATABASE_OPERATION = "database_operation";
    public static final String OPERATION_CACHE_OPERATION = "cache_operation";
    
    // Logging Status Constants
    public static final String STATUS_INITIATED = "initiated";
    public static final String STATUS_SUCCESS_LOG = "success";
    public static final String STATUS_FAILURE_LOG = "failure";
    public static final String STATUS_ERROR_LOG = "error";
    public static final String STATUS_VIOLATION = "violation";
    
    // Security Event Constants
    public static final String SECURITY_EVENT_AUTH_FAILURE = "auth_failure";
    public static final String SECURITY_EVENT_RATE_LIMIT_VIOLATION = "rate_limit_violation";
    
    // Audit Category Constants
    public static final String AUDIT_CATEGORY = "audit";
    public static final String PERFORMANCE_CATEGORY = "performance";
    
    // Role Constants
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";
    
    // JWT Constants
    public static final String JWT_BEARER_FORMAT = "JWT";
    
    // HTTP Status Code Constants
    public static final int HTTP_CLIENT_ERROR_THRESHOLD = 400;
    
    // Session Management Constants
    public static final int SESSION_MAX_INACTIVE_SECONDS = 86400; // 24 hours
    public static final int SECURITY_HEADER_MAX_AGE_SECONDS = 31536000; // 1 year
    
    // User Agent Constants
    public static final int MAX_USER_AGENT_DISPLAY_LENGTH = 100;
    
    // Controller Pagination Constants
    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int DEFAULT_AUDIT_HOURS = 24;
    public static final int DEFAULT_METRICS_DAYS = 30;
    public static final int DEFAULT_DASHBOARD_DAYS = 7;
    public static final int DEFAULT_HIGH_RISK_HOURS = 24;
    
    // Rate Limiting Messages
    public static final String MSG_TOO_MANY_REGISTRATION_ATTEMPTS = "Too many registration attempts. Please try again later.";
    public static final String MSG_TOO_MANY_LOGIN_ATTEMPTS = "Too many login attempts. Please try again in %d seconds.";
    public static final String MSG_TOO_MANY_EMAIL_VERIFICATION_ATTEMPTS = "Too many email verification attempts. Please try again later.";
    
    // Token Display Constants
    public static final int TOKEN_LOG_PREFIX_LENGTH = 8;
    public static final int BEARER_TOKEN_PREFIX_LENGTH = 7;
    
    // Time Formatting Constants
    public static final String TIME_START_SUFFIX = "T00:00:00";
    public static final String TIME_END_SUFFIX = "T23:59:59";
    
    // API Version Constants
    public static final String API_VERSION = "1.0.0";
    
    // CORS Origins Constants (moved from hardcoded arrays)
    public static final String LOCALHOST_FRONTEND = "http://localhost:3000";
    public static final String PRODUCTION_DOMAIN = "https://trademaster.com";
}