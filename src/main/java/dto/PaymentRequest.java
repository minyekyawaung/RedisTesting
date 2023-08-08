package dto;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {
    private String fromAccountId;
    private String toAccountId;
    //@Range(min = 1,message = "Please enter valid amount.")
    private BigDecimal amount;
    private String currency;
    private String narrative="";
    private String toAccountType; // bank, phone
    private String paymentType; // wave wallet, topUp, bill
    private PaymentAdditional paymentAdditional;
    private boolean jointFlag;
    private String otpToken;

}

