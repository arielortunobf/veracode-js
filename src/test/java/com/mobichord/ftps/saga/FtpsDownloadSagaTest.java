package com.mobichord.ftps.saga;

import com.appchord.messages.Signal;
import com.mobichord.messaging.SignalHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FtpsDownloadSagaTest {

    @InjectMocks
    FtpsDownloadSaga ftpsDownloadSaga;

    @Test
    void getHandlersTest() {
        List<SignalHandler<? extends Signal>> handlers = ftpsDownloadSaga.getHandlers();

        assertEquals(2, handlers.size());
    }
}