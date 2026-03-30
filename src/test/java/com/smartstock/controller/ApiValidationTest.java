package com.smartstock.controller;

import com.smartstock.common.GlobalExceptionHandler;
import com.smartstock.service.TradeService;
import com.smartstock.service.UserService;
import com.smartstock.util.UserContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiValidationTest {

    private static LocalValidatorFactoryBean validator;

    @BeforeAll
    static void setUpValidator() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
    }

    @AfterAll
    static void closeValidator() {
        validator.close();
    }

    @Test
    void loginShouldRejectInvalidEmailFormat() throws Exception {
        UserService userService = Mockito.mock(UserService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("邮箱格式不正确"));
    }

    @Test
    void buyShouldRejectPriceWithMoreThanTwoDecimals() throws Exception {
        TradeService tradeService = Mockito.mock(TradeService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TradeController(tradeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        mockMvc.perform(post("/api/trade/buy")
                        .requestAttr(UserContext.REQUEST_ATTR_USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stockCode": "000001",
                                  "price": 10.123,
                                  "quantity": 100
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("买入价格最多保留2位小数"));
    }

    @Test
    void sellShouldRejectPriceWithMoreThanTwoDecimals() throws Exception {
        TradeService tradeService = Mockito.mock(TradeService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TradeController(tradeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        mockMvc.perform(post("/api/trade/sell")
                        .requestAttr(UserContext.REQUEST_ATTR_USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stockCode": "000001",
                                  "price": 10.123,
                                  "quantity": 100
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("卖出价格最多保留2位小数"));
    }
}
