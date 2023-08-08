package dto;


import lombok.Builder;
import lombok.Data;

@Data
public class PaymentAdditional {
    //TopUp
    private String provider;
    private String topUpOrPack;
    private String packId;
    //InterBank transfer
    private String beneficiaryName;
    private String beneficiaryMobileNumber;
    private String beneficiaryBankCode;
    private String beneficiaryBankStateDivision;
    private String beneficiaryBankBranch;
    private String beneficiaryBankCCTCode;
    private String interbankTransactionType;
    //Bill payment
    private String billTemplateId;
    private String billReferenceId;
    //For response
    private String fiName;
    private String branchName;

    private String reference;
    private String name;
    private boolean quote;
    private String clientTransRef;
    private String payer;
    private String note;
}
