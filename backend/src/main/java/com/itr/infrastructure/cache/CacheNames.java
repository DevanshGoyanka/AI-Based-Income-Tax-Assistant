package com.itr.infrastructure.cache;

import java.util.Set;

/** Cache names as constants. */
public final class CacheNames {

    private CacheNames() {}

    public static final String PAN_VALIDATION = "pan-validation";
    public static final String AY_CONSTANTS = "ay-constants";
    public static final String CLIENT_LIST = "client-list";
    public static final String AIS_DATA = "ais-data";
    public static final String FORM_26AS = "form-26as";
    public static final String ITD_SESSION = "itd-session";

    public static final Set<String> ALL_CACHES = Set.of(
        PAN_VALIDATION, AY_CONSTANTS, CLIENT_LIST, AIS_DATA, FORM_26AS, ITD_SESSION
    );
}
