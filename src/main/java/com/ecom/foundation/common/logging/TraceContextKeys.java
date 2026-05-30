package com.ecom.foundation.common.logging;


public class TraceContextKeys {

    private TraceContextKeys() {
    }

    public static final String TRACE_TYPE = "traceType";
    public static final String REQUEST_ID = "requestId";
    public static final String CORRELATION_ID = "correlationId";
    public static final String PROCESS_ID = "processId";

    public static final String BUSINESS_ID = "businessId";

    public static final String HTTP_METHOD = "httpMethod";
    public static final String REQUEST_URI = "requestUri";

    public static final String MESSAGE_ID = "messageId";
    public static final String EVENT_TYPE = "eventType";
    public static final String CONSUMER_NAME = "consumerName";

    public static final String JOB_NAME = "jobName";

}