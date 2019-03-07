package com.apollocurrency.aplwallet.api.response;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetAccountBlockCount extends ResponseBase {
    public long numberOfBlocks;
}