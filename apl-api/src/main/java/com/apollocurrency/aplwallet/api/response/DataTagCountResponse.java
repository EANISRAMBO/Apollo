package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataTagCountResponse extends ResponseBase {

    @ApiModelProperty
    public Integer numberOfDataTags;

    public DataTagCountResponse(Integer numberOfDataTags) {
        this.numberOfDataTags = numberOfDataTags;
    }
}