package com.trademaster.marketdata.agentos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MCP Method annotation for method identification
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface MCPMethod {
    String value();
}
