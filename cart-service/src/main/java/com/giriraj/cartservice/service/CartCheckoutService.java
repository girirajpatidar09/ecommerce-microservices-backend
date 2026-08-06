package com.giriraj.cartservice.service;

import com.giriraj.cartservice.dto.checkout.CheckoutCartDTO;

public interface CartCheckoutService {

    CheckoutCartDTO getCheckoutCart(Long userId);

    void clearCheckoutCart(Long userId);
}