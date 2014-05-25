package stripe

class StripeController {

    def index() {
        render(view: 'form')
    }


    def pay(){
        render(text: params.stripeToken)
    }

    def subscribe() {

    }
}
