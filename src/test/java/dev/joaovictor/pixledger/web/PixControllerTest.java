package dev.joaovictor.pixledger.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.joaovictor.pixledger.application.pix.ReceivePixUseCase;
import dev.joaovictor.pixledger.application.pix.SendPixCommand;
import dev.joaovictor.pixledger.application.pix.SendPixUseCase;
import dev.joaovictor.pixledger.domain.exception.InsufficientFundsException;

/** HTTP contract only: status codes and error format. Business rules are tested elsewhere. */
@WebMvcTest(PixController.class)
class PixControllerTest {

    private static final UUID ACCOUNT = UUID.fromString("57b722a0-77e9-4cd5-9bb1-4e3b7d340d9b");

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private SendPixUseCase sendPix;
    @MockitoBean
    private ReceivePixUseCase receivePix;

    @Test
    void shouldReturn201WhenPixIsSent() throws Exception {
        // given
        when(sendPix.execute(any(SendPixCommand.class))).thenReturn(UUID.randomUUID());

        // when / then
        mvc.perform(post("/pix/transfers").contentType(MediaType.APPLICATION_JSON).content(pix(3000)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.endToEndId").value("E123"));
    }

    @Test
    void shouldReturn422WithProblemDetailWhenBalanceIsInsufficient() throws Exception {
        // given
        when(sendPix.execute(any(SendPixCommand.class)))
                .thenThrow(new InsufficientFundsException(ACCOUNT, 100, 3000));

        // when / then
        mvc.perform(post("/pix/transfers").contentType(MediaType.APPLICATION_JSON).content(pix(3000)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Insufficient funds"));
    }

    @Test
    void shouldReturn400WhenAmountIsNotPositive() throws Exception {
        mvc.perform(post("/pix/transfers").contentType(MediaType.APPLICATION_JSON).content(pix(-5)))
                .andExpect(status().isBadRequest());
    }

    private static String pix(long amount) {
        return """
                {"customerAccountId": "%s", "amount": %d, "endToEndId": "E123"}
                """.formatted(ACCOUNT, amount);
    }
}
