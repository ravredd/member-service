package com.sams.merch.engenable.memberservice.web;

/**
 * Used for Swagger documentation examples and testing.
 */
public class ApiStaticExamples {

    private ApiStaticExamples(){};//Utility static class should never have an instance

    public static final String MEMBER_EXAMPLE1_ID = "1";
    public static final String MEMBER_EXAMPLE1 = "{\"id\":"+ MEMBER_EXAMPLE1_ID + ",\"firstName\":\"Jessica\",\"lastName\":\"Campbell\",\"email\":\"jcampbell2i@cpanel.net\"}";
    public static final String MEMBER_EXAMPLE2_ID = "2";
    public static final String MEMBER_EXAMPLE2 = "{\"id\":"+ MEMBER_EXAMPLE2_ID + ",\"firstName\":\"Bob\",\"lastName\":\"Marley\",\"email\":\"bob.marley@google.com\"}";
    public static final String MEMBER_EXAMPLE3_ID = "3";
    public static final String MEMBER_EXAMPLE3 = "{\"id\":"+ MEMBER_EXAMPLE3_ID + ",\"firstName\":\"Tom\",\"lastName\":\"Brady\",\"email\":\"tom.brady@expat.net\"}";

    public static final String MEMBER_ARRAY_EXAMPLE = "["+MEMBER_EXAMPLE1+","+MEMBER_EXAMPLE2+","+MEMBER_EXAMPLE3+"]";


}


