package stripe

import com.stripe.Stripe
import com.stripe.exception.InvalidRequestException
import com.stripe.model.Charge
import com.stripe.model.Customer
import com.stripe.model.Subscription

class StripeController {

    //This Test Publishable Key - you can get it from your Stripe account settings.
    private static final String API_KEY = "sk_test_UkUNgK9qJ0esq1lF2aG7JuSK";

    def index() {
        //Show price and plan choice page
        render(view: 'payment')
    }

    def showPaymentForm() {
        //Show form for single payment
        render(view: 'formForPayment')
    }

    def showSubscriptionForm() {
        //Show form for subscription payment
        render(view: 'formForSubscription')
    }

    def pay() {
        //The request is of type HttpServletRequest.
        //"stripeToken" is generated by Stripe server using its Stripe.js. You need to capture this parameter and pass it into card parameter in a map.
        String token = request.getParameter("stripeToken");
        Charge charge = processPaymentWithStrip(token, 400, "usd", "single payment", "li.ming116@gmail.com")

        //You can render the resulting page using charge object
        render(text: charge.toString())
    }

    public Charge processPaymentWithStrip(String stripeToken, Integer amount, String currency, String description, String email) {
        Stripe.apiKey = API_KEY;
        Map<String, String> metadata = new HashMap<String, String>()
        metadata.put("email", email)
        Map<String, Object> chargeParams = new HashMap<String, Object>();
        chargeParams.put("amount", amount);
        chargeParams.put("currency", currency);
        chargeParams.put("card", stripeToken); // obtained with Stripe.js
        chargeParams.put("description", description);
        chargeParams.put("metadata", metadata);
            Charge charge = Charge.create(chargeParams);
        return charge;
    }

    def subscribe() {
        //The request is of type HttpServletRequest
        String token = request.getParameter("stripeToken");
        Subscription subscription = processSubscriptionWithStripe(token, "customerId", "li.ming116@gmail.com", "professional");

        //You can render the resulting page using subscription object
        render(text: subscription.toString())
    }

    public Subscription processSubscriptionWithStripe(String stripeToken, String customerId, String customerEmail, String plan) {
        Stripe.apiKey = API_KEY;
        Customer customer = null;

        try {
            customer = Customer.retrieve(customerId);
        } catch (InvalidRequestException e) {
            if (e.getMessage().startsWith("No such customer")) {
                Map<String, Object> params = new HashMap<String, Object>();
                if (customerEmail != null && !"".equals(customerEmail))
                    params.put("email", customerEmail);
                customer = Customer.create(params);
            } else {
                throw e;
            }
        }

        //customer id - need to be stored in the database to enable us to identify the user in Stripe.
        String cuId = customer.getId();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("plan", plan);
        params.put("card", stripeToken);
        Subscription subscription = customer.createSubscription(params);
        return subscription;
    }
}