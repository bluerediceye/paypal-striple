<%--
  Created by IntelliJ IDEA.
  User: Ming
  Date: 15/05/2014
  Time: 22:40
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title></title>
</head>

<body>
<table id="plans-prices">
    <caption>Paid Plans and Prices</caption>
    <tr>
        <th>Plan</th>
        <th>Responses</th>
        <th>Cost</th>
        <th>Action</th>
    </tr>

    <tr>
        <td rowspan="3" class="plan">Monthly Subscription</td>
        <td>1,000<br/>(Starter)</td>
        <td>Monthly $4</td>
        <td>
            <button data-amount="Starter" class="subscribe">Subscribe</button>
        </td>
    </tr>

    <tr>
        <td>2,000<br/>(Professional)</td>
        <td>Monthly $5</td>
        <td>
            <button data-amount="Professional" class="subscribe">Subscribe</button>
        </td>
    </tr>

    <tr>
        <td>Unlimited</td>
        <td>Monthly $6</td>
        <td>
            <button data-amount="Unlimited" class="subscribe">Subscribe</button>
        </td>
    </tr>

    <tr>
        <td rowspan="3" class="plan">Pay as You Use<br/>(single payment)</td>
        <td>25</td>
        <td>$4</td>
        <td>
            <button data-amount="25" class="pay">Pay</button>
        </td>
    </tr>

    <tr>
        <td>70</td>
        <td>$5</td>
        <td>
            <button data-amount="70" class="pay">Pay</button>
        </td>
    </tr>

    <tr>
        <td>100</td>
        <td>$10</td>
        <td>
            <button data-amount="100" class="pay">Pay</button>
        </td>
    </tr>
</table>

<div id="payment-logos"><div>Payments powered by</div>
    <a href="http://www.worldpay.com"><img src="images/worldpay_logo.gif" alt="Worldpay Logo" id="worldpay"/></a>
    <a href="http://www.paypal.com"><img src="images/paypal_logo.gif" alt="PayPal Logo" id="Paypal"/></a>
</div>
</body>

<script src="https://ajax.googleapis.com/ajax/libs/jquery/2.0.0/jquery.min.js"></script>
<script type="text/javascript">

    (function () {
        var planPayments = {
            attachEvents: function () {
                var that = this;

                $('button').on('click', function (e) {
                    e.preventDefault();

                    if ($(e.target).hasClass('pay')) {
                        that.paymentProcess();
                    } else {
                        that.subscribeProcess();
                    }
                });
            },

            paymentProcess: function () {
                window.location.href = 'http://mings-mbp:8080/paypal/payment/pay';
            },

            subscribeProcess: function () {
                window.location.href = 'http://mings-mbp:8080/paypal/payment/subscribe';
            }
        };

        planPayments.attachEvents();
    }());
</script>
</html>