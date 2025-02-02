package com.microsoft.applicationinsights.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.*;

import static org.junit.Assert.*;

@UseAgent
public class JettyNativeHandlerTest extends AiSmokeTest {

    @Test
    @TargetUri("/path")
    public void doSimpleTest() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);

        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

        assertEquals("HTTP GET", rd.getName());
        assertEquals("200", rd.getResponseCode());
        assertTrue(rd.getProperties().isEmpty());
        assertTrue(rd.getSuccess());
    }
}
