package paypal;

/**
 * Created by Ming on 19/05/2014.
 */
public class Plan {
    private int type = SUBSCRIPTION;   // 1 payment, 0 subscription
    private String name;
    private String description;
    private String cost;

    public static int SUBSCRIPTION = 0;
    public static int PAY_AS_YOU_GO = 1;

    public String getBillingAgreementDescription() {
        if (type == SUBSCRIPTION) {
            return "$" + cost + " per month";
        } else if(type == PAY_AS_YOU_GO){
            return name + " $" + cost + " "  + description;
        }
        return null;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCost() {
        return cost;
    }

    public void setCost(String cost) {
        this.cost = cost;
    }
}
