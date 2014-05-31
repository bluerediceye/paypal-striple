package paypal

import com.paypal.api.payments.*
import com.paypal.core.rest.APIContext
import com.paypal.core.rest.PayPalResource
import com.paypal.ipn.IPNMessage
import urn.ebay.api.PayPalAPI.*
import urn.ebay.apis.CoreComponentTypes.BasicAmountType
import urn.ebay.apis.eBLBaseComponents.*

import java.text.SimpleDateFormat

class PaypalController {
    //todo this map can be replaced by a database store
    /**
     * guid to payment id mapping
     */
    private static Map<String, String> paymentTracker = new HashMap<String, String>();

    /**
     * guid to subscription plan mapping
     */
    private static Map<String, Plan> subscriptionTracker = new HashMap<String, Plan>();
    private static CurrencyCodeType currency = CurrencyCodeType.USD;

    private Properties paypalProps;
    private PayPalAPIInterfaceServiceService service;

    public PaypalController() {
        paypalProps = new Properties()
        paypalProps.load(PaypalController.getClassLoader().getResourceAsStream("sdk_config.properties"))
        service = new PayPalAPIInterfaceServiceService(paypalProps);
    }

    //todo show payment page
    def index() {
        render(view: 'payment')
    }

    //todo show payment confirmation page to let users confirm the payment. At the moment, it's default to complete payment when users are directed
    //todo you need a page before payment.execute()
    def paypalpaymentcallback_return() {
        String accessToken = GenerateAccessToken.getAccessToken();
        String guid = (String) request.getParameter("guid");
        String paymentId = paymentTracker.get(guid);
        Payment payment = Payment.get(accessToken, paymentId);
        //todo a confirmation page required before the following execution. the payment object obtained above can be used to render that page.
        payment.execute(accessToken, new PaymentExecution(payerId: payment.getPayer().getPayerInfo().getPayerId()));
        render(text: payment.toJSON())
    }

    //todo show payment cancellation page
    def paypalpaymentcallback_cancel() {
        render(text: "Payment cancellation page")
    }

    //todo show subscription confirmation page to let user confirm the subscription.
    //todo you need a page before CreateRecurringPaymentsProfileReq profileReq = new CreateRecurringPaymentsProfileReq() to show users' plan choice
    //todo you can get users plan choice from subscriptionTracker. In production, you probably want to replace it with a database.
    def paypalsubscriptioncallback() {
        GetExpressCheckoutDetailsReq req = new GetExpressCheckoutDetailsReq();
        GetExpressCheckoutDetailsRequestType reqType = new GetExpressCheckoutDetailsRequestType(request.getParameter("token"));
        req.setGetExpressCheckoutDetailsRequest(reqType);
        GetExpressCheckoutDetailsResponseType resp = service.getExpressCheckoutDetails(req);

        if (resp != null && resp.getAck().toString().equalsIgnoreCase("SUCCESS")) {
            Map<Object, Object> map = new LinkedHashMap<Object, Object>();
            map.put("ack", resp.getAck());
            map.put("token", resp.getGetExpressCheckoutDetailsResponseDetails().getToken());
            map.put("payerId", resp.getGetExpressCheckoutDetailsResponseDetails().getPayerInfo().getPayerID());
            String token = resp.getGetExpressCheckoutDetailsResponseDetails().getToken();
            String payerId = resp.getGetExpressCheckoutDetailsResponseDetails().getPayerInfo().getPayerID()

            //todo here payment confirmation page (show payment plan) can be shown with response details before here,
            // todo and then confirm the payment with the code below in another page.
            //todo the stored plan with guid and GetExpressCheckoutDetailsResponseType can be used to render the page.

            Plan plan = (Plan) subscriptionTracker.get(request.getParameter("guid"));
            String paidPerMonth = plan.getCost();

            CreateRecurringPaymentsProfileReq profileReq = new CreateRecurringPaymentsProfileReq();
            CreateRecurringPaymentsProfileRequestType profileReqType = new CreateRecurringPaymentsProfileRequestType();

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, 1);
            String nextBillingDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSSZ").format(cal.getTime());
            RecurringPaymentsProfileDetailsType profileDetails = new RecurringPaymentsProfileDetailsType(nextBillingDate);

            ActivationDetailsType activationDetails = new ActivationDetailsType();
            activationDetails.setInitialAmount(new BasicAmountType(currency, paidPerMonth));
            activationDetails.setFailedInitialAmountAction(FailedPaymentActionType.CANCELONFAILURE);

            ScheduleDetailsType scheduleDetails = new ScheduleDetailsType();
            scheduleDetails.setDescription(plan.getBillingAgreementDescription());
            scheduleDetails.setMaxFailedPayments(new Integer(1));
            scheduleDetails.setAutoBillOutstandingAmount(AutoBillType.ADDTONEXTBILLING);
            scheduleDetails.setActivationDetails(activationDetails);

            int frequency = Integer.parseInt("1");
            BasicAmountType paymentAmount = new BasicAmountType(currency, paidPerMonth)
            BillingPeriodType period = BillingPeriodType.MONTH;
            BillingPeriodDetailsType paymentPeriod = new BillingPeriodDetailsType(period, frequency, paymentAmount);
            paymentPeriod.setTotalBillingCycles(Integer.parseInt("0"));
            paymentPeriod.setTaxAmount(new BasicAmountType(currency, "0"));
            scheduleDetails.setPaymentPeriod(paymentPeriod);

            CreateRecurringPaymentsProfileRequestDetailsType reqDetails = new CreateRecurringPaymentsProfileRequestDetailsType(profileDetails, scheduleDetails);
            reqDetails.setToken(token);
            profileReqType.setCreateRecurringPaymentsProfileRequestDetails(reqDetails);
            profileReq.setCreateRecurringPaymentsProfileRequest(profileReqType);
            CreateRecurringPaymentsProfileResponseType profileResp = service.createRecurringPaymentsProfile(profileReq);
            if (profileResp != null && profileResp.getAck().toString().equalsIgnoreCase("SUCCESS")) {
                Map<Object, Object> profileMap = new HashMap<Object, Object>();
                profileMap.put("Ack", profileResp.getAck());
                profileMap.put("profileId", profileResp.getCreateRecurringPaymentsProfileResponseDetails().getProfileID());
                profileMap.put("transactionId", profileResp.getCreateRecurringPaymentsProfileResponseDetails().getTransactionID());
                //todo this will be used to see if the payment is complete (pending means it's still being processed and it can be not paid yet)
                profileMap.put("profileStatus", profileResp.getCreateRecurringPaymentsProfileResponseDetails().getProfileStatus());
            } else {
                //todo redirect unsuccessful payment page
                redirect(action: 'fail')
                return;
            }
        } else {
            //todo redirect unsuccessful payment page
            redirect(action: 'fail')
            return;
        }

        //todo show successful payment page
        render(text: "Successful subscription")
    }


    //todo execute subscription action
    def subscribe() {
        Plan planA = new Plan();
        planA.setCost("6");
        planA.setName("Monthly Subscription");
        planA.setDescription("Unlimited (Professional)");
        planA.setType(Plan.SUBSCRIPTION);

        String returnUrl = "http://test.bluerediceye.eu.cloudbees.net/paypal/paypalsubscriptioncallback";
        String cancelUrl = "http://test.bluerediceye.eu.cloudbees.net/paypal/paypalsubscriptioncallback";
        String approvedRedirectUrl = processPlanSubscription(planA, returnUrl, cancelUrl, "li.mingxyz+paypal+buyer@gmail.com");
        redirect(url: approvedRedirectUrl);
    }


    //todo execute payment action
    def pay() {
        Plan planB = new Plan();
        planB.setType(Plan.PAY_AS_YOU_GO);
        planB.setCost("10");
        planB.setName("Pay as You Use (single payment)");
        planB.setDescription("100 Responses")
        String returnUrl = "http://test.bluerediceye.eu.cloudbees.net/paypal/paypalpaymentcallback_return";
        String cancelUrl = "http://test.bluerediceye.eu.cloudbees.net/paypal/paypalpaymentcallback_cancel";
        String approvedRedirectUrl = processPlanPayment(planB, returnUrl, cancelUrl);
        redirect(url: approvedRedirectUrl);
    }

    private String processPlanSubscription(Plan plan, String returnUrl, String cancelUrl, String buyerEmail) {
        SetExpressCheckoutRequestType setExpressCheckoutReq = new SetExpressCheckoutRequestType();
        SetExpressCheckoutRequestDetailsType details = new SetExpressCheckoutRequestDetailsType();

        String guid = UUID.randomUUID().toString().replaceAll("-", "");
        //todo you can do it with database
        subscriptionTracker.put(guid, plan);
        String returnURL = returnUrl + "?guid=" + guid;
        String cancelURL = cancelUrl + "?guid=" + guid;
        details.setReturnURL(returnURL);
        details.setCancelURL(cancelURL);
        details.setBuyerEmail(buyerEmail);

        double itemTotal = 0.00;
        double orderTotal = 0.00;
        String amountItems = plan.getCost();
        String qtyItems = "1";

        List<PaymentDetailsItemType> lineItems = new ArrayList<PaymentDetailsItemType>();

        PaymentDetailsItemType item = new PaymentDetailsItemType();
        BasicAmountType itemAmt = new BasicAmountType(CurrencyCodeType.USD, amountItems);
        item.setQuantity(new Integer(1));
        item.setName(plan.getName());
        item.setAmount(itemAmt);
        item.setItemCategory(ItemCategoryType.DIGITAL);
        item.setDescription(plan.getDescription());
        lineItems.add(item);

        itemTotal += Double.parseDouble(qtyItems) * Double.parseDouble(amountItems);
        orderTotal += itemTotal;

        List<PaymentDetailsType> payDetails = new ArrayList<PaymentDetailsType>();
        PaymentDetailsType paydtl = new PaymentDetailsType();
        paydtl.setPaymentAction(PaymentActionCodeType.SALE);
        BasicAmountType itemsTotal = new BasicAmountType(CurrencyCodeType.USD, Double.toString(itemTotal));
        paydtl.setOrderTotal(new BasicAmountType(CurrencyCodeType.USD, Double.toString(orderTotal)));
        paydtl.setPaymentDetailsItem(lineItems);
        paydtl.setItemTotal(itemsTotal);
        payDetails.add(paydtl);
        details.setPaymentDetails(payDetails);

        BillingAgreementDetailsType billingAgreement = new BillingAgreementDetailsType(BillingCodeType.RECURRINGPAYMENTS)
        billingAgreement.setBillingAgreementDescription(plan.getBillingAgreementDescription())
        List<BillingAgreementDetailsType> billList = new ArrayList<BillingAgreementDetailsType>()
        billList.add(billingAgreement)
        details.setBillingAgreementDetails(billList)

        setExpressCheckoutReq.setSetExpressCheckoutRequestDetails(details);
        SetExpressCheckoutReq expressCheckoutReq = new SetExpressCheckoutReq();
        expressCheckoutReq.setSetExpressCheckoutRequest(setExpressCheckoutReq);

        SetExpressCheckoutResponseType setExpressCheckoutResponse = service.setExpressCheckout(expressCheckoutReq);

        if (setExpressCheckoutResponse != null && setExpressCheckoutResponse.getAck().toString().equalsIgnoreCase("SUCCESS")) {
            return "https://www.sandbox.paypal.com/cgi-bin/webscr?cmd=_express-checkout&token=" + setExpressCheckoutResponse.getToken();
        }else{
            return  "http://test.bluerediceye.eu.cloudbees.net/paypal/fail";
        }
    }

    /**
     * Process a plan payment with given information and return paypal approval redirect url
     * @param plan
     * @param returnUrl
     * @param cancelUrl
     * @return paypal approval url
     */
    private String processPlanPayment(Plan plan, String returnUrl, String cancelUrl) {
        PayPalResource.initConfig(paypalProps);
        String accessToken = GenerateAccessToken.getAccessToken();
        APIContext apiContext = new APIContext(accessToken);

        // Let's you specify details of a payment amount.
        Details details = new Details();
        details.setSubtotal(plan.getCost());

        // Let's you specify a payment amount.
        Amount amount = new Amount();
        amount.setCurrency(CurrencyCodeType.USD.value);

        // Total must be equal to sum of shipping, tax and subtotal.
        amount.setTotal(plan.getCost());
        amount.setDetails(details);

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setDescription(plan.getDescription());

        transaction.itemList = new ItemList();
        List<Item> items = new ArrayList<Item>();
        transaction.getItemList().setItems(items);
        Item item = new Item("1", plan.getName(), plan.getCost(), CurrencyCodeType.USD.value);
        transaction.getItemList().getItems().add(item);
        List<Transaction> transactions = new ArrayList<Transaction>();
        transactions.add(transaction);

        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");

        Payment payment = new Payment();
        payment.setIntent("sale");
        payment.setPayer(payer);
        payment.setTransactions(transactions);

        // ###Redirect URLs
        RedirectUrls redirectUrls = new RedirectUrls();
        String guid = UUID.randomUUID().toString().replaceAll("-", "");
        redirectUrls.setReturnUrl(returnUrl + "?guid=" + guid);
        redirectUrls.setCancelUrl(cancelUrl + "?guid=" + guid);
        payment.setRedirectUrls(redirectUrls);

        Payment createdPayment = payment.create(apiContext);
        paymentTracker.put(guid, createdPayment.getId());
        String approvedRedirectUrl = null;
        Iterator<Links> links = createdPayment.getLinks().iterator();
        while (links.hasNext()) {
            Links link = links.next();
            if (link.getRel().equalsIgnoreCase("approval_url")) {
                approvedRedirectUrl = link.getHref();
            }
        }
        return approvedRedirectUrl;
    }

    //todo any failed request goes this page.
    def fail() {
        render(text: "fail!!!!!!!")
    }


    //todo instance payment notification. this url can be accessed by paypal.
    def ipn(){
        Map<String,String> configurationMap = new HashMap<String, String>();
        for (String name: paypalProps.stringPropertyNames()) {
            configurationMap.put(name, paypalProps.getProperty(name));
        }

        IPNMessage ipnlistener = new IPNMessage(request, configurationMap);
        boolean isIpnVerified = ipnlistener.validate();
        String transactionType = ipnlistener.getTransactionType();
        Map<String,String> map = ipnlistener.getIpnMap();

        log.info("******* IPN (name:value) pair : "+ map + "  " +
                "######### TransactionType : "+transactionType+"  ======== IPN verified : "+ isIpnVerified);
    }
}