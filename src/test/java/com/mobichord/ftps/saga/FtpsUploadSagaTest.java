package com.mobichord.ftps.saga;

import com.appchord.messages.Signal;
import com.mobichord.messaging.SignalHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class FtpsUploadSagaTest {
    @InjectMocks
    private FtpsUploadSaga ftpsUploadSaga;

    @Test
    void getHandlersTest() {

        List<SignalHandler<? extends Signal>> handlers = ftpsUploadSaga.getHandlers();

        assertEquals(2, handlers.size());
    }

}
