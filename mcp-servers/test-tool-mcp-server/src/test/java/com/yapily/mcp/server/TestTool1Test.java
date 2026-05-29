package com.yapily.mcp.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestTool1Test
{
    private TestTool1 tools;


    @BeforeEach
    void setUp()
    {
        tools = new TestTool1();
    }


    @Test
    void greet_inEnglish_returnsEnglishGreeting()
    {
        String result = tools.greet("Alice", null);
        assertThat(result).contains("Alice").containsIgnoringCase("hello");
    }
}
