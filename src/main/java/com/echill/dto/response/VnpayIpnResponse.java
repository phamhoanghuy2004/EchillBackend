package com.echill.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class VnpayIpnResponse {
    private String RspCode;
    private String Message;
}